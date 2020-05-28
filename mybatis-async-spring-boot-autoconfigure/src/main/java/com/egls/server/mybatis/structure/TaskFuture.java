package com.egls.server.mybatis.structure;

import lombok.extern.slf4j.Slf4j;

import java.util.concurrent.CompletableFuture;
import java.util.function.Consumer;

/**
 * @author LiuQi - [Created on 2020-01-16]
 */
@Slf4j
public class TaskFuture<T> extends CompletableFuture<T> {

    public CompletableFuture<T> whenComplete(Consumer<T> consumer) {
        return whenComplete((t, throwable) -> {
            if (throwable != null) {
                log.error("", throwable);
            } else {
                try {
                    consumer.accept(t);
                } catch (Throwable th) {
                    log.error("", th);
                }
            }
        });
    }
}
