package com.untitled.ecm.dao.mappers;

import com.untitled.ecm.dtos.http.SuccessFailureBreakdown;
import org.skife.jdbi.v2.StatementContext;
import org.skife.jdbi.v2.tweak.ResultSetMapper;

import java.sql.ResultSet;
import java.sql.SQLException;

public class SuccessFailureBreakdownMapper implements ResultSetMapper<SuccessFailureBreakdown> {
    @Override
    public SuccessFailureBreakdown map(int i, ResultSet resultSet, StatementContext statementContext) throws SQLException {
        int successCount = resultSet.getInt("success_count");
        int failureCount = resultSet.getInt("failure_count");

        return new SuccessFailureBreakdown(successCount, failureCount);
    }
}
