package net.apachegui.db;


import net.apachegui.locks.Operation;

import java.sql.Connection;
import java.sql.SQLException;

public class LogDataJdbcConnection extends JdbcConnection{

    private final static String LOG_DATABASE_FILE = "apachegui-history-database.db";

    public LogDataJdbcConnection() {
        super("apachegui-history-database.lock");
    }

    public Connection getConnection(Operation operation) {
        return super.getConnection(LOG_DATABASE_FILE, operation);
    }

}
