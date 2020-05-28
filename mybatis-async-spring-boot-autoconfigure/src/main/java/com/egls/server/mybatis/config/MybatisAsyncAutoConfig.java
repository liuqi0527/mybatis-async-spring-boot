package com.egls.server.mybatis.config;

import com.egls.server.mybatis.DbTaskPool;
import com.egls.server.mybatis.structure.DbCallBackHandler;
import com.egls.server.mybatis.version.DatabaseVersionUpdate;
import org.apache.ibatis.session.SqlSessionFactory;
import org.mybatis.spring.SqlSessionFactoryBean;
import org.mybatis.spring.boot.autoconfigure.MybatisAutoConfiguration;
import org.springframework.boot.autoconfigure.AutoConfigureAfter;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;


/**
 * @author LiuQi - [Created on 2020-01-17]
 */
@Configuration
@EnableConfigurationProperties(MybatisAsyncProperties.class)
@ConditionalOnClass({SqlSessionFactory.class, SqlSessionFactoryBean.class})
@ConditionalOnBean({SqlSessionFactory.class})
@AutoConfigureAfter(MybatisAutoConfiguration.class)
public class MybatisAsyncAutoConfig {

    private MybatisAsyncProperties properties;

    public MybatisAsyncAutoConfig(MybatisAsyncProperties properties) {
        this.properties = properties;
    }

    @ConditionalOnMissingBean
    @Bean(initMethod = "start", destroyMethod = "shutdown")
    public DbTaskPool configPool(SqlSessionFactory sqlSessionFactory,
                                 DbCallBackHandler callBackHandler) {
        DbTaskPool pool = new DbTaskPool(sqlSessionFactory, callBackHandler);
        pool.setProperties(properties);
        return pool;
    }

    @ConditionalOnMissingBean
    @Bean(initMethod = "init")
    public DatabaseVersionUpdate update(DbTaskPool pool) {
        return new DatabaseVersionUpdate(properties.getVersionFilePath(), pool);
    }

}
