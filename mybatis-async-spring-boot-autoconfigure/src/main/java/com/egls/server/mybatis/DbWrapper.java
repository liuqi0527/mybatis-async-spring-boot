package com.egls.server.mybatis;

import com.egls.server.mybatis.task.BaseDbTask;
import org.apache.ibatis.session.SqlSession;
import org.apache.ibatis.session.SqlSessionFactory;

/**
 * 数据库相关操作的一个包装类
 * <p>
 * 主要用于管理数据库线程池的实例，提供数据库任务提交的入口
 *
 * @author LiuQi - [Created on 2018-10-09]
 */
public class DbWrapper {

    private static volatile DbTaskPool dbTaskPool;

    public static void init(DbTaskPool pool) {
        if (dbTaskPool == null) {
            dbTaskPool = pool;
        } else {
            throw new IllegalArgumentException("db task pool is already exist");
        }
    }

    /**
     * 提交一个数据库任务
     *
     * @param task
     */
    public static void submit(BaseDbTask task) {
        dbTaskPool.submit(task);
    }

    /**
     * 创建一个SqlSession来执行数据库任务
     * 注意：使用完毕后需要调用{@link #closeSqlSession(SqlSession)}关闭掉
     *
     * @return
     */
    public static SqlSession openSqlSession() {
        return dbTaskPool.openSqlSession();
    }

    /**
     * 关闭SqlSession
     * <p>
     * Mybatis中的SqlSession本身是一个快速创建和销毁的类
     * 所以每次执行Sql语句都需要去重新获取一个SqlSession对象，使用完即销毁
     * 否则会导致SqlSession占用数据库资源不释放，导致后续的任务无法执行
     *
     * @param sqlSession
     */
    public static void closeSqlSession(SqlSession sqlSession) {
        dbTaskPool.closeSqlSession(sqlSession);
    }

    public static SqlSessionFactory getSqlSessionFactory() {
        return dbTaskPool.getSqlSessionFactory();
    }

    /**
     * 是否还有数据库任务正在执行、或者等待执行
     */
    public static boolean isBusy() {
        return dbTaskPool.isBusy();
    }

    public static DbTaskPool getDbTaskPool() {
        return dbTaskPool;
    }
}
