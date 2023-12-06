package com.untitled.ecm.dao;

import org.skife.jdbi.v2.DBI;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.SQLException;

public final class DakiyaDBFactory {
    private final static Logger logger = LoggerFactory.getLogger(DakiyaDBFactory.class);
    private static DBI metabaseDB = null;
    private static DBI dakiyaDB = null;
    private DakiyaDBFactory() {

    }

    public static DBI getMetabaseDB() throws SQLException {
        if (DakiyaDBFactory.metabaseDB == null) {
            logger.error("metabaseDBI access requested before initializing it");
            throw new SQLException("metabaseDBI access requested before initializing it");
        }
        return DakiyaDBFactory.metabaseDB;
    }

    public static void setMetabaseDB(DBI metabaseDB) {
        if (DakiyaDBFactory.metabaseDB != null) {
            logger.warn("metabaseDBI reference modified, this may(will) lead to errors");
        }
        DakiyaDBFactory.metabaseDB = metabaseDB;
    }

    public static DBI getDakiyaDB() throws SQLException {
        if (DakiyaDBFactory.dakiyaDB == null) {
            logger.error("dakiyaDBI access requested before initializing it");
            throw new SQLException("dakiyaDBI access requested before initializing it");
        }
        return dakiyaDB;
    }

    public static void setDakiyaDB(DBI dakiyaDB) {
        if (DakiyaDBFactory.dakiyaDB != null) {
            logger.warn("dakiyaDBI reference modified, this may(will) lead to errors");
        }
        DakiyaDBFactory.dakiyaDB = dakiyaDB;
    }
}
