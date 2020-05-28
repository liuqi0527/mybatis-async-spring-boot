package com.egls.server.mybatis.version;

import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

/**
 * @author LiuQi - [Created on 2018-12-27]
 */
public interface DataBaseVersionMapper {

    @Update("${sql}")
    void executeCustomSql(@Param("sql") String sql);


    @Update("create table if not exists `database_version`(" +
            "`id` int(1) NOT NULL DEFAULT 0, " +
            "`version` bigint(20) NOT NULL DEFAULT 0, " +
            "`modify_time` timestamp NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP, " +
            "PRIMARY KEY (`id`)" +
            ");")
    int createVersionTable();


    @Select("select `version` from `database_version` limit 1")
    Integer getVersion();

    @Update("insert into `database_version` set `id` = 0, `version` = #{version}")
    int setVersion(@Param("version") int version);


    @Update("update `database_version` set `version` = #{version}")
    int updateVersion(@Param("version") int version);
}
