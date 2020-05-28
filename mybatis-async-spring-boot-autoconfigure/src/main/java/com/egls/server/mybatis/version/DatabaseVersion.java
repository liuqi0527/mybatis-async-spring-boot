package com.egls.server.mybatis.version;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.xml.bind.annotation.XmlRootElement;

/**
 * @author LiuQi - [Created on 2018-12-27]
 */
@XmlRootElement(name = "root")
public class DatabaseVersion {

    private Map<Integer, SqlList> version = new HashMap<>();

    public Map<Integer, SqlList> getVersion() {
        return version;
    }

    public void setVersion(Map<Integer, SqlList> version) {
        this.version = version;
    }

    public static class SqlList {

        private List<String> list = new ArrayList<>();

        public List<String> getList() {
            return list;
        }

        public void setList(List<String> list) {
            this.list = list;
        }
    }

}

