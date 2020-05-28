package com.egls.server.mybatis;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicLong;

/**
 * @author LiuQi - [Created on 2019-04-11]
 */
public class DbStatistics {

    /**
     * DB模块收到的任务数量
     */
    public static final Recorder RECEIVE_TASK_COUNT = new Recorder();

    /**
     * 成功提交到线程池的任务数量
     */
    public static final Recorder SUBMIT_TASK_COUNT = new Recorder();

    /**
     * 执行成功的任务数量
     */
    public static final Recorder EXECUTE_SUCCESS_TASK_COUNT = new Recorder();

    /**
     * 执行失败的任务数量
     */
    public static final Recorder EXECUTE_FAIL_TASK_COUNT = new Recorder();

    /**
     * 执行任务所用时间
     */
    public static final Recorder EXECUTE_SUCCESS_TASK_COST_MILLIS = new Recorder();


    public static final Map<String, Integer> XML_STATEMENT_RECORD = new HashMap<>();


    public static class Recorder {

        public final AtomicLong atomicLong = new AtomicLong(0);

        private long startStamp;

        public void start() {
            startStamp = System.currentTimeMillis();
        }

        public void record() {
            long cost = System.currentTimeMillis() - startStamp;
            if (startStamp > 0 && cost > 0) {
                atomicLong.getAndAdd(cost);
            }
            startStamp = 0;
        }

        public long get() {
            return atomicLong.get();
        }

        public long incrementAndGet() {
            return atomicLong.incrementAndGet();
        }
    }


    /**
     * 打印所有正在执行、待执行的任务信息
     *
     * @return
     */
    public static String getTaskDebugInfo() {
        return getThreadPoolDebugInfo() + getTaskQueueDebugInfo() + getCostMillisDebugInfo();
    }

    public static String getThreadPoolDebugInfo() {
        DbTaskPool dbTaskPool = DbWrapper.getDbTaskPool();
        return "active thread count : " + dbTaskPool.getThreadPool().getActiveCount() + "; \n" +
                "complete task count in pool : " + dbTaskPool.getThreadPool().getCompletedTaskCount() + "; \n" +
                "waiting  task count in pool : " + dbTaskPool.getThreadPool().getQueue().size() + "; \n";
    }

    public static String getTaskQueueDebugInfo() {
        DbTaskPool dbTaskPool = DbWrapper.getDbTaskPool();
        StringBuilder stringBuilder = new StringBuilder();

        long taskQueueCount = dbTaskPool.getTaskMap().size();
        stringBuilder.append("task queue count : ").append(taskQueueCount).append("; \n");

        long executingTaskCount = dbTaskPool.getTaskMap().values().stream().map(queue -> queue.executingTask).filter(Objects::nonNull).count();
        stringBuilder.append("executing task count : ").append(executingTaskCount).append("; \n");

        long waitingTaskCount = dbTaskPool.getTaskMap().values().stream().mapToInt(queue -> queue.waitingQueue.size()).sum();
        stringBuilder.append("waiting task count : ").append(waitingTaskCount).append("; \n");

        stringBuilder.append("fail task count : ").append(DbStatistics.EXECUTE_FAIL_TASK_COUNT.get()).append("; \n");
        return stringBuilder.toString();
    }

    public static String getCostMillisDebugInfo() {
        return "cost millis per task : " + DbStatistics.EXECUTE_SUCCESS_TASK_COST_MILLIS.get() / DbStatistics.EXECUTE_SUCCESS_TASK_COUNT.get() + "; \n";
    }

    public static String getXmlStatementDebugInfo() {
        StringBuilder stringBuilder = new StringBuilder();
        for (Map.Entry<String, Integer> entry : DbStatistics.XML_STATEMENT_RECORD.entrySet()) {
            stringBuilder.append(entry.getKey()).append(":").append(entry.getValue()).append("\n");
        }
        return stringBuilder.toString();
    }
}
