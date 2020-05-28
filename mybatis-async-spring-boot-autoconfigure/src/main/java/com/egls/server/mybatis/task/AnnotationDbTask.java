package com.egls.server.mybatis.task;

import java.util.function.Consumer;
import java.util.function.Function;

import com.egls.server.mybatis.structure.DbCallBack;

import org.apache.ibatis.session.SqlSession;

/**
 * @author LiuQi - [Created on 2018-10-09]
 */
public class AnnotationDbTask<M, R> extends BaseDbTask {

    private Class<M> mapperClass;
    private Consumer<M> mapperConsumer;
    private Function<M, R> mapperFunction;

    public AnnotationDbTask(Class<M> mapperClass, Consumer<M> mapperConsumer, DbCallBack<R> callBack) {
        this.mapperClass = mapperClass;
        this.mapperConsumer = mapperConsumer;
        this.callBack = callBack;
    }

    public AnnotationDbTask(Class<M> mapperClass, Function<M, R> mapperFunction, DbCallBack<R> callBack) {
        this.mapperClass = mapperClass;
        this.mapperFunction = mapperFunction;
        this.callBack = callBack;
    }

    @Override
    public R execute(SqlSession sqlSession) {
        M mapper = sqlSession.getMapper(mapperClass);
        if (mapperFunction != null) {
            return mapperFunction.apply(mapper);
        }

        mapperConsumer.accept(mapper);
        return null;
    }


    @Override
    public String toString() {
        return "MapperDbTask{" +
                "mapperClass=" + mapperClass +
                ", boundId=" + boundId +
                '}';
    }
}
