package com.egls.server.mybatis;

import com.egls.server.mybatis.config.MybatisAsyncProperties;
import com.egls.server.mybatis.exception.RejectedTaskException;
import com.egls.server.mybatis.structure.DbCallBack;
import com.egls.server.mybatis.structure.DbCallBackHandler;
import com.egls.server.mybatis.structure.TaskFuture;
import com.egls.server.mybatis.structure.TaskResult;
import com.egls.server.mybatis.task.BaseDbTask;
import com.egls.server.mybatis.task.XmlDbTask;
import com.egls.server.utils.concurrent.ThreadUtil;
import com.egls.server.utils.math.MathUtil;
import com.google.common.util.concurrent.ThreadFactoryBuilder;
import lombok.Getter;
import lombok.Setter;
import org.apache.ibatis.session.SqlSession;
import org.apache.ibatis.session.SqlSessionFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Deque;
import java.util.Map;
import java.util.concurrent.*;


/**
 * @author LiuQi - [Created on 2018-10-09]
 */
@Getter
@Setter
public class DbTaskPool {

    private static final Logger log = LoggerFactory.getLogger(DbTaskPool.class);

    /**
     * 配置属性
     */
    private MybatisAsyncProperties properties;
    /**
     * 异步回掉处理
     */
    private DbCallBackHandler taskBackHandler;

    private SqlSessionFactory sqlSessionFactory;
    private Map<Long, TaskQueue> taskMap = new ConcurrentHashMap<>();

    private Thread processor;
    private ThreadPoolExecutor threadPool;

    private volatile boolean shutdown;

    public DbTaskPool(SqlSessionFactory sqlSessionFactory, DbCallBackHandler taskBackHandler) {
        this.sqlSessionFactory = sqlSessionFactory;
        this.taskBackHandler = taskBackHandler;
        DbWrapper.init(this);
    }


    public synchronized void start() {
        if (sqlSessionFactory == null) {
            throw new IllegalArgumentException("SqlSessionFactory not be set in DbExecutor");
        }

        if (processor == null) {
            log.debug("Initializing DbTaskPool");

            processor = new ThreadFactoryBuilder().build().newThread(this::distribute);
            processor.setName("Db-Distribute-Processor");
            processor.start();


            MybatisAsyncProperties.Pool poolProperties = properties.getPool();
            threadPool = new ThreadPoolExecutor(
                    poolProperties.getCoreSize(), poolProperties.getMaximumSize(),
                    poolProperties.getKeepAliveMinute(), TimeUnit.MINUTES,
                    new LinkedBlockingQueue<>(poolProperties.getQueueSize()),
                    ThreadUtil.newThreadFactory("db-task-pool-thread-%d")
            );
        }
    }

    public synchronized void shutdown() {
        log.debug("DbTaskPool shutting down ...");
        shutdown = true;
    }

    public void submit(BaseDbTask dbTask) {
        if (dbTask.getBoundId() <= 0) {
            try {
                log.debug("db task submit to pool task ->  {}", dbTask.toString());
                submitToPool(dbTask);
            } catch (RejectedTaskException e) {
                log.error("thread pool queue is full, submit to queue task ->  {}", dbTask.toString());
                taskMap.computeIfAbsent(dbTask.getBoundId(), key -> new TaskQueue()).add(dbTask);
            }
        } else {
            log.debug("db task submit to queue task ->  {}", dbTask.toString());
            taskMap.computeIfAbsent(dbTask.getBoundId(), key -> new TaskQueue()).add(dbTask);
        }

        if (dbTask instanceof XmlDbTask) {
            DbStatistics.XML_STATEMENT_RECORD.merge(((XmlDbTask) dbTask).getStatement(), 1, MathUtil::plusInt);
        }
        DbStatistics.RECEIVE_TASK_COUNT.incrementAndGet();
    }

    private void distribute() {
        while (!shutdown || isBusy()) {
            try {
                taskMap.values().forEach(taskQueue -> {
                    BaseDbTask task = null;
                    try {
                        if (taskQueue.isIdle()) {
                            task = taskQueue.poll();
                            submitToPool(task);
                        }
                    } catch (RejectedTaskException e) {
                        log.info("thread pool queue is full, task is rejected ->  {}", (task == null ? "" : task.toString()));
                        taskQueue.reset();
                    }
                });
            } catch (Throwable e) {
                log.error("", e);
            } finally {
                try {
                    Thread.sleep(1L);
                } catch (InterruptedException e) {
                    log.error("", e);
                }
            }
        }

        try {
            threadPool.shutdown();
            threadPool.awaitTermination(30, TimeUnit.SECONDS);
        } catch (InterruptedException e) {
            log.error("", e);
        }
        log.debug("DbTaskPool shut down complete");
    }

    private void submitToPool(BaseDbTask task) throws RejectedTaskException {
        if (task != null) {
            try {
                threadPool.submit(new DbRunnableTask(task));
                DbStatistics.SUBMIT_TASK_COUNT.incrementAndGet();
            } catch (RejectedExecutionException e) {
                throw new RejectedTaskException();
            }
        }
    }


    private class DbRunnableTask implements Runnable {

        private BaseDbTask task;

        private DbRunnableTask(BaseDbTask task) {
            this.task = task;
        }


        @Override
        public void run() {

            //执行数据库操作
            TaskResult<?> result = execute();

            //处理回调函数
            handleResult(result);

            //修改队列状态、使分发线程可以继续获取执行队列中的其他任务
            if (task.getBoundId() > 0 || taskMap.containsKey(task.getBoundId())) {
                taskMap.get(task.getBoundId()).setIdle();
            }

            //统计数据
            if (result.isSuccess()) {
                DbStatistics.EXECUTE_SUCCESS_TASK_COUNT.incrementAndGet();
            } else {
                DbStatistics.EXECUTE_FAIL_TASK_COUNT.incrementAndGet();
            }
        }

        private TaskResult<Object> execute() {
            int retryCount = 0;
            TaskResult<Object> result = new TaskResult<>();

            SqlSession sqlSession = openSqlSession();
            do {
                try {
                    result.setThrowable(null);
                    log.debug("db task execute begin  task ->  {}, retry -> {}", task.toString(), retryCount);
                    DbStatistics.EXECUTE_SUCCESS_TASK_COST_MILLIS.start();

                    result.setData(task.execute(sqlSession));

                    DbStatistics.EXECUTE_SUCCESS_TASK_COST_MILLIS.record();
                    log.debug("db task execute finished  task ->  {}, retry -> {}", task.toString(), retryCount);
                } catch (Throwable t) {
                    result.setThrowable(t);
                    log.error(String.format("db task error task -> %s, retry->%d", task.toString(), retryCount), t);
                }
            } while (!result.isSuccess() && retryCount++ < properties.getFailRetryCount());

            //关闭Session
            try {
                closeSqlSession(sqlSession);
            } catch (Exception e) {
                log.error("close sql session error", e);
            }

            return result;
        }

        private void handleResult(TaskResult result) {
            try {
                DbCallBack callBack = task.getCallBack();
                if (task.getFuture() != null) {
                    TaskFuture future = task.getFuture();
                    callBack = new DbCallBack<>(future::complete, future::completeExceptionally);
                }


                if (callBack != null) {
                    callBack.setBoundId(task.getBoundId());
                    callBack.setResult(result);

                    if (taskBackHandler != null) {
                        log.debug("db callback handled begin task ->  {},  result ->  {}", task.toString(), result);
                        taskBackHandler.handle(callBack);
                    } else {
                        log.debug("db callback execute direct begin task ->  {},  result ->  {}", task.toString(), result);
                        callBack.call();
                    }
                }
            } catch (Exception e) {
                log.error(String.format("db task callback error result->%s, task->%s", result.getData(), task.toString()), e);
            }
        }
    }


    /**
     * 提交到线程池的任务有可能会指定一个绑定的ID，
     * 对于同一个绑定ID的所有任务，会放入同一个任务队列中顺序执行
     */
    public static class TaskQueue {

        Deque<BaseDbTask> waitingQueue = new ConcurrentLinkedDeque<>();

        BaseDbTask executingTask;

        void add(BaseDbTask task) {
            waitingQueue.addLast(task);
        }

        BaseDbTask poll() {
            return executingTask = waitingQueue.pollFirst();
        }

        void reset() {
            if (executingTask != null) {
                waitingQueue.addFirst(executingTask);
                executingTask = null;
            }
        }

        boolean removable() {
            return isIdle() && waitingQueue.isEmpty();
        }

        boolean isIdle() {
            return executingTask == null;
        }

        void setIdle() {
            this.executingTask = null;
        }
    }


    /**
     * SqlSession暂时使用随时创建、用完销毁的方式
     * 以后如果这里产生性能问题，再以按线程复用Session的方式进行优化
     */
    public SqlSession openSqlSession() {
        return sqlSessionFactory.openSession(true);
    }

    public void closeSqlSession(SqlSession sqlSession) {
        sqlSession.close();
    }

    public boolean isBusy() {
        return threadPool.getActiveCount() > 0 || !threadPool.getQueue().isEmpty() || !taskMap.values().stream().allMatch(TaskQueue::isIdle);
    }

}
