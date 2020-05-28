package com.egls.server.mybatis.structure;

import org.apache.ibatis.session.SqlSession;

/**
 * @author LiuQi - [Created on 2018-10-09]
 */
public enum DbType {

    //
    INSERT {
        @Override
        public Object execute(SqlSession sqlSession, String statement, Object parameter) {
            return sqlSession.insert(statement, parameter);
        }
    },

    UPDATE {
        @Override
        public Object execute(SqlSession sqlSession, String statement, Object parameter) {
            return sqlSession.update(statement, parameter);
        }
    },

    DELETE {
        @Override
        public Object execute(SqlSession sqlSession, String statement, Object parameter) {
            return sqlSession.delete(statement, parameter);
        }
    },

    SELECT_ONE {
        @Override
        public Object execute(SqlSession sqlSession, String statement, Object parameter) {
            return sqlSession.selectOne(statement, parameter);
        }
    },

    SELECT_LIST {
        @Override
        public Object execute(SqlSession sqlSession, String statement, Object parameter) {
            return sqlSession.selectList(statement, parameter);
        }
    },;

    public abstract Object execute(SqlSession sqlSession, String statement, Object parameter);

}
