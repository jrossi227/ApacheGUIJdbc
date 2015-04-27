package net.apachegui.db;


import java.sql.Connection;
import java.sql.SQLException;

public class LogDataJdbcConnection extends JdbcConnection{

    private final static String LOG_DATABASE_FILE = "apachegui-history-database.db";

    public LogDataJdbcConnection() {
        super("LogData.lock");
    }

    public Connection getConnection() {
        return super.getConnection(LOG_DATABASE_FILE);
    }

}
