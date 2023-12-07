package com.untitled.ecm.resources.validators;

import com.untitled.ecm.constants.DakiyaStrings;
import com.untitled.ecm.dao.external.RedshiftDao;
import lombok.extern.slf4j.Slf4j;
import org.everit.json.schema.FormatValidator;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Slf4j
public class MetabaseSQLValidator implements FormatValidator {
    private RedshiftDao redshiftDao;
    private List<String> forbiddelSQLCommands;

    public MetabaseSQLValidator(RedshiftDao redshiftDao) {
        if (redshiftDao == null) {
            throw new InstantiationError("null value provided");
        }
        this.redshiftDao = redshiftDao;
        this.populateForbiddenSQLCommands();
    }

    @Override
    public Optional<String> validate(final String sql) {
        if (!isRedshiftUpAndAccessible()) {
            return Optional.of("Redshift is down");
        }

        String sql_ = sql.toLowerCase();
        for (String command : this.forbiddelSQLCommands) {
            if (sql_.contains(command)) {
                return Optional.of(DakiyaStrings.NON_SELECT_COMMAND_PRESENT_IN_SQl);
            }
        }

        if (!sql_.contains("select ")) {
            return Optional.of(DakiyaStrings.SELECT_COMMAND_NOT_PRESENT_IN_SQl);
        }

        try {
            this.redshiftDao.getAllRecipientsIterator(sql_).close();
        } catch (Exception e) {
            return Optional.of(e.getMessage());
        }

        return Optional.empty();

    }

    public boolean isRedshiftUpAndAccessible() {
        String intStr = "1";
        try {
            List<String> results = this.redshiftDao.getAllRecipients("select " + intStr);
            if (results.size() != 1) {
                log.error("select 1 query to redshift returned incorrect value");
                return false;
            }
            return true;
        } catch (Exception e) {
            log.error("Error occurred while checking redshift");
            return false;
        }
    }

    private void populateForbiddenSQLCommands() {
        this.forbiddelSQLCommands = new ArrayList<>();
        // trailing space is important to prevent catching any column or table name

        // ddl
        this.forbiddelSQLCommands.add("create ");
        this.forbiddelSQLCommands.add("alter ");
        this.forbiddelSQLCommands.add("drop ");
        this.forbiddelSQLCommands.add("truncate ");
        this.forbiddelSQLCommands.add("comment ");
        this.forbiddelSQLCommands.add("rename ");

        // dml
        this.forbiddelSQLCommands.add("insert ");
        this.forbiddelSQLCommands.add("update ");
        this.forbiddelSQLCommands.add("delete ");
        this.forbiddelSQLCommands.add("merge ");
        this.forbiddelSQLCommands.add("call ");
        this.forbiddelSQLCommands.add("lock table ");

        // dcl
        this.forbiddelSQLCommands.add("grant ");
        this.forbiddelSQLCommands.add("revoke ");

        // tcl
        this.forbiddelSQLCommands.add("commit ");
        this.forbiddelSQLCommands.add("rollback ");

    }
}
