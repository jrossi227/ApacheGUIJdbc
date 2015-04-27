package net.apachegui.db;


import org.apache.log4j.Logger;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.channels.FileChannel;
import java.nio.channels.FileLock;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.sql.*;

public class JdbcConnection {
    private static Logger log = Logger.getLogger(JdbcConnection.class);

    private static JdbcConnection instance = null;

    // locking fields
    private String lockName;
    private FileChannel channel = null;
    private FileLock lock = null;

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

    private JdbcConnection() {
        this.lockName = "Jdbc.lock";
    }

    protected JdbcConnection(String lockName) {
        this.lockName = lockName;
    }

    private String getDatabaseDirectory() {
        String directory = System.getProperty("catalina.base");
        log.trace("Loading database from file: " + directory);

        return directory;
    }

    protected Connection getConnection(String dbName) {
        Connection connection = null;
        try {
            Class.forName("org.sqlite.JDBC");
            connection = DriverManager.getConnection("jdbc:sqlite:/" + new File(getDatabaseDirectory(), "db/" +  dbName).getAbsolutePath());
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
        SettingsDao.getInstance().clearDatabase();
        LogDataDao.getInstance().clearDatabase();
        UsersDao.getInstance().clearDatabase();
    }

    public void readLock() throws IOException {

        File file = new File(getDatabaseDirectory(), lockName);
        if (!file.exists()) {
            file.createNewFile();
        }

        Path path = Paths.get(file.getAbsolutePath());
        channel = FileChannel.open(path, StandardOpenOption.READ);
        lock = channel.lock(0, Long.MAX_VALUE, true);
    }

    public void writeLock() throws IOException {
        File file = new File(getDatabaseDirectory(), lockName);
        if (!file.exists()) {
            file.createNewFile();
        }

        Path path = Paths.get(file.getAbsolutePath());
        channel = FileChannel.open(path, StandardOpenOption.READ, StandardOpenOption.WRITE);
        lock = channel.lock();
    }

    public void releaseLock() {

        try {
            if( lock != null ) {
                lock.release();
                lock = null;
            }

            if( channel != null) {
                channel.close();
                channel = null;
            }
        } catch (Exception e) {
            log.error(e.getMessage(), e);
        }
    }

}
