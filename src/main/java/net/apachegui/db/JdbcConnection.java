package net.apachegui.db;


import org.apache.log4j.Logger;

import java.io.File;
import java.sql.*;

public class JdbcConnection {
    private static Logger log = Logger.getLogger(JdbcConnection.class);

    //Constants for jdbc
    private final static String LOG_DATABASE_FILE = "apachegui-history-database.db";
    private final static String GUI_DATABASE_FILE = "apachegui-gui-database.db";
    private final static String DEFAULT_USERNAME = "admin";
    private final static String DEFAULT_PASSWORD = "admin";

    private static JdbcConnection instance = null;

    public static JdbcConnection getInstance() {

        synchronized (JdbcConnection.class) {
            if(instance == null) {
                synchronized (JdbcConnection.class) {
                    instance = new JdbcConnection();
                }
            }
        }

        return instance;
    }

    protected JdbcConnection() {

    }

    private String getDatabaseDirectory() {
        return System.getProperty("catalina.base");
    }

    private Connection getConnection(String dbName) {
        Connection connection = null;
        try {
            Class.forName("org.sqlite.JDBC");
            connection = DriverManager.getConnection("jdbc:sqlite:/" + new File(getDatabaseDirectory(), dbName).getAbsolutePath());
        } catch (Exception e) {
            log.error(e.getMessage(), e);
        }

        return connection;
    }

    protected Connection getLogDataConnection() {
        return getConnection(LOG_DATABASE_FILE);
    }

    protected Connection getGuiConnection() {
        return getConnection(GUI_DATABASE_FILE);
    }

    public void closeResultSet(ResultSet resultSet) {
        try {
            if(resultSet != null && !resultSet.isClosed()) {
                resultSet.close();
            }
        } catch (SQLException e) {
            log.error(e.getMessage(), e);
        }
    }

    public void closeStatement(Statement statement) {

        try {
            if(statement != null && !statement.isClosed()) {
                statement.close();
            }
        } catch (SQLException e) {
            log.error(e.getMessage(), e);
        }

    }

    public void closeConnection(Connection connection) {
        try {
            if(connection != null && !connection.isClosed()) {
                connection.close();
            }
        } catch (SQLException e) {
            log.error(e.getMessage(), e);
        }
    }

    /**
     * Clears the ApacheGUI database and sets the username and password back to default values.
     * 
     * @throws SQLException
     * @throws IllegalAccessException
     * @throws InstantiationException
     * @throws ClassNotFoundException
     */
    public void clearAllDatabases() {

        Connection guiConnection = null;
        Statement guiStatement = null;

        Connection logDataConnection = null;
        Statement logDataStatement = null;
        try {
            guiConnection = getGuiConnection();

            guiStatement = guiConnection.createStatement();
            String update = "DELETE FROM SETTINGS";
            guiStatement.executeUpdate(update);

            update = "VACUUM";
            guiStatement.executeUpdate(update);

            closeStatement(guiStatement);
            closeConnection(guiConnection);
            guiStatement = null;
            guiConnection = null;

            logDataConnection = getLogDataConnection();

            logDataStatement = logDataConnection.createStatement();
            update = "DELETE FROM LOGDATA";
            logDataStatement.executeUpdate(update);

            update = "VACUUM";
            logDataStatement.executeUpdate(update);

            closeStatement(logDataStatement);
            closeConnection(logDataConnection);
            logDataStatement = null;
            logDataConnection = null;

            UsersDao.getInstance().setUsername(DEFAULT_USERNAME);
            UsersDao.getInstance().setPassword(DEFAULT_PASSWORD);
        } catch (Exception e) {
            log.error(e.getMessage(), e);
        } finally {
            closeStatement(guiStatement);
            closeConnection(guiConnection);

            closeStatement(logDataStatement);
            closeConnection(logDataConnection);
        }

    }

}
