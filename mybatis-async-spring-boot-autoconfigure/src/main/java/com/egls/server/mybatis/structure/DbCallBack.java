package com.egls.server.mybatis.structure;

import lombok.Getter;
import lombok.Setter;

import java.util.function.Consumer;

/**
 * 封装了数据库执行的结果，以及成功失败对应的函数
 *
 * @author LiuQi - [Created on 2018-10-09]
 */
@Getter
@Setter
public class DbCallBack<R> {

    private long boundId;
    private Consumer<R> resultConsumer;
    private Consumer<Throwable> failConsumer;
    private TaskResult<R> result;

    public DbCallBack(Consumer<R> resultConsumer) {
        this(resultConsumer, null);
    }

    public DbCallBack(Consumer<R> resultConsumer, Consumer<Throwable> failConsumer) {
        this.resultConsumer = resultConsumer;
        this.failConsumer = failConsumer;
    }


    public void call() {
        if (result.isSuccess()) {
            if (resultConsumer != null) {
                resultConsumer.accept(result.getData());
            }
        } else if (failConsumer != null) {
            failConsumer.accept(result.getThrowable());
        }
    }
}
