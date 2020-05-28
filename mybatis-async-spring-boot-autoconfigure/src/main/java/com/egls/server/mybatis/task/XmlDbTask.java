package com.egls.server.mybatis.task;

import com.egls.server.mybatis.structure.DbCallBack;
import com.egls.server.mybatis.structure.DbType;
import com.egls.server.mybatis.structure.TaskFuture;
import org.apache.ibatis.session.SqlSession;

import java.util.concurrent.CompletableFuture;

/**
 * @author LiuQi - [Created on 2018-10-09]
 */
public class XmlDbTask extends BaseDbTask {

    private DbType dbType;
    private String statement;
    private Object parameter;

    public XmlDbTask(DbType dbType, String statement, Object parameter, DbCallBack<?> callBack) {
        this.statement = statement;
        this.dbType = dbType;
        this.parameter = parameter;
        this.callBack = callBack;
    }

    public XmlDbTask(DbType dbType, String statement, Object parameter, TaskFuture<?> future) {
        this.dbType = dbType;
        this.statement = statement;
        this.parameter = parameter;
        this.future = future;
    }

    @Override
    public Object execute(SqlSession sqlSession) {
        return dbType.execute(sqlSession, statement, parameter);
    }

    @Override
    public String toString() {
        return "SqlDbTask{" +
                "dbType=" + dbType +
                ", statement='" + statement + '\'' +
                ", parameter=" + (parameter != null ? parameter.toString() : "") +
                ", boundId=" + boundId +
                '}';
    }

    public String getStatement() {
        return statement;
    }
}
