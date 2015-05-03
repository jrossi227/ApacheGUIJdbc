package net.apachegui.db;


import net.apachegui.locks.LockManager;
import net.apachegui.locks.Operation;
import org.apache.log4j.Logger;

import java.io.File;
import java.nio.channels.FileChannel;
import java.nio.channels.FileLock;
import java.sql.*;

public class JdbcConnection {
    private static Logger log = Logger.getLogger(JdbcConnection.class);

    // locking fields
    private String lockName;

    private FileChannel readChannel = null;
    private FileLock readLock = null;

    private FileChannel writeChannel = null;
    private FileLock writeLock = null;

    Operation operation;

    private JdbcConnection() {
        operation = Operation.WRITE;
        this.lockName = "jdbc.lock";
    }

    protected JdbcConnection(String lockName) {
        operation = Operation.WRITE;
        this.lockName = lockName;
    }

    private String getDatabaseDirectory() {
        String directory = System.getProperty("catalina.base");
        log.trace("Loading database from file: " + directory);

        return (new File(directory, "db")).getAbsolutePath();
    }

    protected Connection getConnection(String dbName, Operation operation) {
        Connection connection = null;
        try {
            Class.forName("org.sqlite.JDBC");
            connection = DriverManager.getConnection("jdbc:sqlite:/" + new File(getDatabaseDirectory(),dbName).getAbsolutePath());

            this.operation = operation;

            String lockFile = new File(getDatabaseDirectory(),lockName).getAbsolutePath();
            if(this.operation == Operation.WRITE) {
                LockManager.getInstance().lockWrite(lockFile);
            } else {
                LockManager.getInstance().lockRead(lockFile);
            }
        } catch (Exception e) {
            log.error(e.getMessage(), e);
        }

        return connection;
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
        } finally {

            String lockFile = new File(getDatabaseDirectory(),lockName).getAbsolutePath();
            if(this.operation == Operation.WRITE) {
                LockManager.getInstance().unlockWrite(lockFile);
            } else {
                LockManager.getInstance().unlockRead(lockFile);
            }
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
    public static void clearAllDatabases() {
        SettingsDao.getInstance().clearDatabase();
        LogDataDao.getInstance().clearDatabase();
        UsersDao.getInstance().clearDatabase();
    }

}
