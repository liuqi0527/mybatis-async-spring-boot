package com.egls.server.mybatis.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * @author LiuQi - [Created on 2020-01-17]
 */
@Data
@ConfigurationProperties(prefix = "mybatis.async")
public class MybatisAsyncProperties {

    private Pool pool = new Pool();

    /**
     * 数据库任务执行失败后最多重试的次数
     */
    private int failRetryCount = 2;

    /**
     * 数据库版本文件路径
     */
    private String versionFilePath;


    @Data
    public static class Pool {

        private int coreSize = 5;
        private int maximumSize = 10;
        private int keepAliveMinute = 5;
        private int queueSize = 3000;

    }
}
