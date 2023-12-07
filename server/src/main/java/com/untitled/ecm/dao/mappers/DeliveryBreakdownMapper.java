package com.untitled.ecm.dao.mappers;

import com.untitled.ecm.dtos.http.DeliveryBreakdown;
import org.skife.jdbi.v2.StatementContext;
import org.skife.jdbi.v2.tweak.ResultSetMapper;

import java.sql.ResultSet;
import java.sql.SQLException;


public class DeliveryBreakdownMapper implements ResultSetMapper<DeliveryBreakdown> {
    @Override
    public DeliveryBreakdown map(int index, ResultSet resultSet, StatementContext statementContext) throws SQLException {

        int openCount = resultSet.getInt("open_count");
        int deliveredCount = resultSet.getInt("delivered_count");
        int scheduledRunCount = resultSet.getInt("scheduled_run_count");
        int otherCount = resultSet.getInt("other_count");

        return new DeliveryBreakdown(openCount, deliveredCount, scheduledRunCount, otherCount);
    }
}

