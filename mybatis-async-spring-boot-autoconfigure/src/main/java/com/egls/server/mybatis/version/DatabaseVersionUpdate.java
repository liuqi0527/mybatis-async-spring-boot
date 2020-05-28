package com.egls.server.mybatis.version;

import com.egls.server.mybatis.DbTaskPool;
import lombok.extern.slf4j.Slf4j;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.Unmarshaller;
import java.io.File;
import java.util.List;
import java.util.Map;

/**
 * <pre>
 * 升级数据库结构版本
 * 使用此功能，需要项目维护一个数据库版本文件，该文件中包含每个版本中对应的结构修改的SQL语句，
 * 当数据库结构有更新时新增一个版本号，并增加对应的SQL语句即可。
 * 项目启动时会自动检查当前数据库的结构版本，并对比版本文件的版本，以进行更新。
 * 首次使用此功能，或者新建立的数据库不会执行任何更新SQL语句，会直接更新此库的版本到文件内的最新版本
 *
 * 数据库版本文件的结构可以查看模板文件com.egls.server.mybatis.support.version.database_version.xml
 *
 * **在项目存在多个分支，多个发行版本的情况下，做版本更新时应尽量避免直接手动修改数据库结构，毕竟手动操作容易产生遗漏
 * </pre>
 *
 * @author LiuQi - [Created on 2020-03-13]
 */
@Slf4j
public class DatabaseVersionUpdate {

    private String versionFilePath;

    private DbTaskPool taskPool;

    public DatabaseVersionUpdate(String versionFilePath, DbTaskPool taskPool) {
        this.versionFilePath = versionFilePath;
        this.taskPool = taskPool;
    }

    public void init() {
        if (versionFilePath == null) {
            return;
        }
        File file = new File(versionFilePath);
        if (!file.exists()) {
            log.error("Database version file -> <{}> not exist", versionFilePath);
            return;
        }


        //初始化version表
        taskPool.getSqlSessionFactory().getConfiguration().addMapper(DataBaseVersionMapper.class);
        DataBaseVersionMapper versionMapper = taskPool.getSqlSessionFactory().getConfiguration().getMapper(DataBaseVersionMapper.class, taskPool.openSqlSession());

        log.info("Database version begin initializing.........");
        versionMapper.createVersionTable();


        Map<Integer, DatabaseVersion.SqlList> versionMap = parseVersionFile(file).getVersion();


        //获取原有版本
        Integer oldVersion = versionMapper.getVersion();
        int maxVersion = versionMap.keySet().stream().max(Integer::compare).orElse(0);
        if (oldVersion == null) {
            //当前数据库没有已记录的版本号，说明是新数据库，直接存储最新版本号即可
            log.info(String.format("Database version -> (null), set to max version (%d)", maxVersion));
            versionMapper.setVersion(maxVersion);
            return;
        }


        log.info(String.format("Database version -> (%d), max version -> (%d)", oldVersion, maxVersion));
        //执行每个版本的结构更新
        for (Map.Entry<Integer, DatabaseVersion.SqlList> entry : versionMap.entrySet()) {
            List<String> sqlList = entry.getValue().getList();
            if (entry.getKey() > oldVersion) {
                log.info(String.format("Database update to version -> (%d), sql count -> (%d)", entry.getKey(), sqlList.size()));

                for (String sql : sqlList) {
                    log.info(String.format("Database execute -> <%s> ", sql));
                    versionMapper.executeCustomSql(sql);
                }
                versionMapper.updateVersion(entry.getKey());
                oldVersion = entry.getKey();
                log.info(String.format("Database update to version -> (%d) success", entry.getKey()));
            }
        }
        log.info("Database version initialize success");
    }

    private DatabaseVersion parseVersionFile(File file) {
        try {
            JAXBContext jaxbContext = JAXBContext.newInstance(DatabaseVersion.class);
            Unmarshaller jaxbUnmarshaller = jaxbContext.createUnmarshaller();
            return (DatabaseVersion) jaxbUnmarshaller.unmarshal(file);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}
