package com.untitled.ecm.dao.external;

import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.skife.jdbi.v2.DBI;
import org.skife.jdbi.v2.Handle;
import org.skife.jdbi.v2.ResultIterator;
import org.skife.jdbi.v2.tweak.HandleCallback;
import org.skife.jdbi.v2.util.StringColumnMapper;

import java.util.List;
import java.util.Map;

@Slf4j
public class RedshiftDao {

    private DBI metabaseDBI;

    public RedshiftDao(DBI metabaseDBI) {
        this.metabaseDBI = metabaseDBI;
    }

    public List<String> getAllRecipients(String sql) {
        return this.metabaseDBI.withHandle(new HandleCallback<List<String>>() {
            @Override
            public List<String> withHandle(Handle handle) {
                return handle.createQuery(sql).map(StringColumnMapper.INSTANCE).list();
            }
        });
    }

    // make sure to close the connection from outside (gets closed automatically if iterator is exhausted)
    public ResultIterator<String> getAllRecipientsIterator(String sql) {
        return this.metabaseDBI.withHandle(new HandleCallback<ResultIterator<String>>() {
            @Override
            public ResultIterator<String> withHandle(Handle handle) {
                return handle.createQuery(sql).map(StringColumnMapper.INSTANCE).iterator();
            }
        });
    }

    public List<Map<String, Object>> getSingleResultsetRow(String sql) {
        List<Map<String, Object>> resultset;
        Handle handle = null;
        try {
            handle = metabaseDBI.open();
            String sql_ = StringUtils.replacePattern(sql, ";$", "");
            sql_ = StringUtils.replacePattern(sql_, "(?i)limit \\d+$", "limit 1");
            if (!sql_.endsWith("limit 1")) {
                sql_ = sql_.concat(" limit 1;");
            } else {
                sql_ = sql_.concat(";");
            }
            resultset = handle.select(sql_);
            handle.close();
            return resultset;
        } catch (Exception e) {
            log.error("could not execute sql query in metabase", e);
            if (handle != null) {
                handle.close();
            }
            return null;
        }
    }

    public List<Map<String, Object>> getResultsetRows(String sql) {
        List<Map<String, Object>> resultset;
        Handle handle = null;
        try {
            handle = metabaseDBI.open();
            resultset = handle.select(sql);
            handle.close();
            return resultset;
        } catch (Exception e) {
            log.error("could not execute sql query in metabase", e);
            if (handle != null) {
                handle.close();
            }
            return null;
        }
    }

    public String getRowCountOfSqlQueryResult(String sql) {
        return this.metabaseDBI.withHandle(new HandleCallback<String>() {
            @Override
            public String withHandle(Handle handle) {
                final String sql_ = StringUtils.replacePattern(sql, ";$", "");
                return handle.createQuery(String.format("select count(*) from ( %s ) x;", sql_)).map(StringColumnMapper.INSTANCE).first();
            }
        });
    }

}
