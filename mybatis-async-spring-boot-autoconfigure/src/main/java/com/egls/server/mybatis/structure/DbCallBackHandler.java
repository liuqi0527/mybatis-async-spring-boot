package com.egls.server.mybatis.structure;

import com.egls.server.mybatis.DbTaskPool;

/**
 * <pre>
 *     有时我们需要依赖数据库执行的结果来执行一些逻辑，而使用异步任务的方式是无法直接获取到结果的，
 *     所以使用回调函数的方式来处理执行结果。
 *     而这个结果是在数据库线程中获得的，如果直接执行回调任务，会导致数据库线程、逻辑线程之间的并发问题。
 *
 *     此类的实现就是用来接管数据库执行结果，具体什么时候执行，在哪里执行，由具体的业务自己决定。
 *     具体的实现类需要注入到{@link DbTaskPool}中才会生效
 *
 * </pre>
 *
 * @author LiuQi - [Created on 2018-10-09]
 */
public interface DbCallBackHandler {

    /**
     * 处理回调函数
     *
     * @param callBack
     */
    void handle(DbCallBack<?> callBack);

}
