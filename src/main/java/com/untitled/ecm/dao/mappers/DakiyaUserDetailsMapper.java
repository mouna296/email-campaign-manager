package com.untitled.ecm.dao.mappers;

import com.untitled.ecm.dtos.DakiyaUserDetails;
import org.skife.jdbi.v2.StatementContext;
import org.skife.jdbi.v2.tweak.ResultSetMapper;

import java.sql.ResultSet;
import java.sql.SQLException;

public class DakiyaUserDetailsMapper implements ResultSetMapper<DakiyaUserDetails> {

    public DakiyaUserDetails map(int index, ResultSet r, StatementContext ctx) throws SQLException {
        DakiyaUserDetails dakiyaUserDetails = new DakiyaUserDetails();
        dakiyaUserDetails.setCreatedOn(r.getTimestamp("created_on"));
        dakiyaUserDetails.setFirstName(r.getString("first_name"));
        dakiyaUserDetails.setLastName(r.getString("last_name"));
        dakiyaUserDetails.setRole(r.getString("role"));
        dakiyaUserDetails.setEmail(r.getString("email"));
        return dakiyaUserDetails;
    }
}
