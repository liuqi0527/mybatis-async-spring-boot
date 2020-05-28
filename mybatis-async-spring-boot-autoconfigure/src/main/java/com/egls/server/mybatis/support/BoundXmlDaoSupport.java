package com.egls.server.mybatis.support;

import com.egls.server.mybatis.task.XmlDbTask;

/**
 * <pre>
 *    操作数据库的支持类，用于执行Xml配置的SQL
 *
 *    继承或使用此类需要指定一个boundId。
 *    对于拥有相同boundId的DAO提交的数据库任务，其会严格按照提交顺序来顺序执行
 *    当指定的boundId<=0时，执行顺序将得不到保障
 *
 *    此类的主要使用场景是对于玩家而言，对于同一个玩家或者同一个模块的数据来说需要保证其执行顺序
 *    否则可能导致后一个更新操作在前一个提交的插入操作前执行，那这个更新操作可能会失败，导致数据丢失。
 * </pre>
 *
 * @author LiuQi - [Created on 2018-10-09]
 */
public class BoundXmlDaoSupport<T> extends BaseXmlDaoSupport<T> {

    private long boundId;

    /**
     * 只有子类可以使用此构造方法
     * 因为父类可以获取到子类的定义的明确泛型类型
     *
     * @param boundId
     */
    protected BoundXmlDaoSupport(long boundId) {
        this.boundId = boundId;
    }

    /**
     * 如果不使用子类定义泛型的方式，则需要明确的执行数据类的类型
     *
     * @param boundId
     * @param structDataClass
     */
    public BoundXmlDaoSupport(long boundId, Class<T> structDataClass) {
        super(structDataClass);
        this.boundId = boundId;
    }

    @Override
    protected void submit(XmlDbTask task) {
        task.setBoundId(boundId);
        super.submit(task);
    }
}
