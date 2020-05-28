package com.egls.server.mybatis.support;

import com.egls.server.mybatis.DbWrapper;
import com.egls.server.mybatis.structure.DbType;
import com.egls.server.mybatis.structure.TaskFuture;
import com.egls.server.mybatis.task.XmlDbTask;
import org.apache.ibatis.session.SqlSession;

import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.List;

/**
 * <pre>
 *     操作数据库的支持类，用于执行Xml配置的SQL
 *
 *     简单的增删改查功能已经实现，如需实现定制、复杂功能需要子类自己实现。
 *     直接调用{@link #openSqlSession()}的方法，属于同步操作，需要等待数据库操作完成才能继续执行，
 *     推荐使用异步回调的方式操作，将数据库操作和回调函数封装成{@link XmlDbTask}, 并将其通过{@link #submit(XmlDbTask)}提交到线程池执行
 * </pre>
 *
 * @author LiuQi - [Created on 2018-10-09]
 */
public abstract class BaseXmlDaoSupport<T> {

    protected static final String INSERT = "insert";
    protected static final String UPDATE = "update";
    protected static final String DELETE = "delete";
    protected static final String SELECT_BY_PRIMARY = "select_by_primary";
    protected static final String DELETE_BY_PRIMARY = "delete_by_primary";
    protected static final String SELECT_LIST = "select_list";

    /**
     * 查找SQL语句时
     * 1、在数据类型的全限定名所代表的命名空间中去查找（即namespace配置为类全名的Xml映射文件中查找）
     * 2、以指定的ID查找具体要执行的SQL语句
     */
    private String statementNamespace;

    public BaseXmlDaoSupport() {
        Type parameterType = this.getClass().getGenericSuperclass();
        if (parameterType instanceof Class) {
            throw new IllegalArgumentException("dao init error: constructed without actual type information " + getClass().getName());
        }

        Class<T> parameterTypeClass = (Class<T>) ((ParameterizedType) parameterType).getActualTypeArguments()[0];
        if (parameterTypeClass == null) {
            throw new IllegalArgumentException("dao init error: constructed without actual type information" + getClass().getName());
        }
        this.statementNamespace = parameterTypeClass.getName();
    }

    public BaseXmlDaoSupport(Class<T> structDataClass) {
        this.statementNamespace = structDataClass.getName();
    }

    public TaskFuture<Integer> insert(T data) {
        return submit(DbType.INSERT, INSERT, data);
    }

    public TaskFuture<Integer> update(T data) {
        return submit(DbType.UPDATE, UPDATE, data);
    }

    public TaskFuture<Integer> delete(T data) {
        return submit(DbType.DELETE, DELETE, data);
    }

    public TaskFuture<Integer> deleteByPrimary(Object primary) {
        return submit(DbType.DELETE, DELETE_BY_PRIMARY, primary);
    }

    public TaskFuture<T> findByPrimary(Object primary) {
        return submit(DbType.SELECT_ONE, SELECT_BY_PRIMARY, primary);
    }

    public TaskFuture<List<T>> findList(Object parameter) {
        return submit(DbType.SELECT_LIST, SELECT_LIST, parameter);
    }

    protected <R> TaskFuture<R> submit(DbType type, String statementSuffix, Object parameter) {
        TaskFuture<R> completedFuture = new TaskFuture<>();
        XmlDbTask sqlTask = new XmlDbTask(type, getStatement(statementSuffix), parameter, completedFuture);
        submit(sqlTask);
        return completedFuture;
    }

    protected void submit(XmlDbTask task) {
        DbWrapper.submit(task);
    }

    protected SqlSession openSqlSession() {
        return DbWrapper.openSqlSession();
    }

    protected void closeSqlSession(SqlSession sqlSession) {
        DbWrapper.closeSqlSession(sqlSession);
    }

    protected String getStatement(String statementId) {
        return statementNamespace + "." + statementId;
    }

}
