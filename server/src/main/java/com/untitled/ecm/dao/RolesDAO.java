package com.untitled.ecm.dao;

import org.skife.jdbi.v2.sqlobject.SqlQuery;

import java.util.List;

public interface RolesDAO {
    @SqlQuery("SELECT * FROM dakiya_roles")
    List<String> findAll();

    void close();
}
