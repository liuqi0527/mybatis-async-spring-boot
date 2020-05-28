package com.egls.server.mybatis.structure;

import lombok.Getter;
import lombok.Setter;

/**
 * @author LiuQi - [Created on 2020-01-16]
 */
@Setter
@Getter
public class TaskResult<R> {

    private R data;

    private Throwable throwable;

    public boolean isSuccess() {
        return throwable == null;
    }
}
