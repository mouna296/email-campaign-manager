package com.untitled.ecm.dao.mappers;

import com.untitled.ecm.dtos.DakiyaSetting;
import org.skife.jdbi.v2.StatementContext;
import org.skife.jdbi.v2.tweak.ResultSetMapper;

import java.sql.ResultSet;
import java.sql.SQLException;

public class DakiyaSettingMapper implements ResultSetMapper<DakiyaSetting> {
    public DakiyaSetting map(int index, ResultSet r, StatementContext ctx) throws SQLException {
        return new DakiyaSetting(r.getString("key"), r.getString("value"));
    }
}
