package com.egls.server.mybatis.task;

import com.egls.server.mybatis.structure.DbCallBack;
import com.egls.server.mybatis.structure.TaskFuture;
import lombok.Getter;
import lombok.Setter;
import org.apache.ibatis.session.SqlSession;

/**
 * @author LiuQi - [Created on 2018-10-09]
 */
@Getter
@Setter
public abstract class BaseDbTask {

    protected long boundId = -1;
    protected DbCallBack callBack;
    protected TaskFuture future;

    public abstract Object execute(SqlSession sqlSession);

}
