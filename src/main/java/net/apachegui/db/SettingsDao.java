package net.apachegui.db;

import net.apachegui.locks.Operation;
import org.apache.log4j.Logger;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.HashMap;

public class SettingsDao {

    private static Logger log = Logger.getLogger(SettingsDao.class);

    private static final String SETTINGS_TABLE = "SETTINGS";

    private static SettingsDao instance = null;

    private HashMap<String,String> settingsMap;

    private boolean cache;

    private SettingsDao() {
        cache = true;
        settingsMap = new HashMap<String, String>();

        getAllSettings();
    }

    public static SettingsDao getInstance() {

        synchronized (SettingsDao.class) {
            if(instance == null) {
                synchronized (SettingsDao.class) {
                    instance = new SettingsDao();
                }
            }
        }

        return instance;
    }

    public boolean isCache() {
        return cache;
    }

    public void setCache(boolean cache) {
        this.cache = cache;
    }

    public void clearDatabase() {

        GuiJdbcConnection guiJdbcConnection = new GuiJdbcConnection();
        Connection connection = null;
        Statement statement = null;
        try {
            connection = guiJdbcConnection.getConnection(Operation.WRITE);
            statement = connection.createStatement();

            String update = "DELETE FROM SETTINGS";
            statement.executeUpdate(update);

            update = "VACUUM";
            statement.executeUpdate(update);
            settingsMap.clear();
        } catch (Exception e) {
            log.error(e.getMessage(), e);
        } finally {
            guiJdbcConnection.closeStatement(statement);
            guiJdbcConnection.closeConnection(connection);
        }
    }

    /**
     * @param name
     *            the name of the setting
     * @return value if found or null if not found
     * @throws SQLException
     * @throws ClassNotFoundException
     * @throws InstantiationException
     * @throws IllegalAccessException
     * **/
    public String getSetting(String name) {
        log.trace("Getting setting " + name);

        if(cache) {
            return settingsMap.get(name);
        }

        String value = null;
        GuiJdbcConnection guiJdbcConnection = new GuiJdbcConnection();
        Connection connection = null;
        Statement statement =  null;
        ResultSet resultSet = null;

        try {
            connection = guiJdbcConnection.getConnection(Operation.READ);
            statement = connection.createStatement();

            String query = "SELECT VALUE FROM " + SETTINGS_TABLE + " WHERE NAME='" + name + "'";
            resultSet = statement.executeQuery(query);

            if(resultSet.next()) {
                value = resultSet.getString("VALUE");
            }

            if (value == null) {
                log.trace("Setting not found in the table");
            } else {
                log.trace("Setting value " + value);
            }

        } catch (Exception e) {
            log.error(e.getMessage(), e);
        } finally {
            guiJdbcConnection.closeResultSet(resultSet);
            guiJdbcConnection.closeStatement(statement);
            guiJdbcConnection.closeConnection(connection);
        }

        return value;
    }

    /**
     * Sets a setting in the database. If the setting already exists then it will be overwritten.
     * 
     * @param name
     *            the name of the setting
     * @param value
     *            the value of the setting
     * @throws SQLException
     * @throws ClassNotFoundException
     * @throws InstantiationException
     * @throws IllegalAccessException
     * **/
    public void setSetting(String name, String value) {
        log.trace("Setting setting " + name + " value " + value);

        GuiJdbcConnection guiJdbcConnection = new GuiJdbcConnection();
        Connection connection = null;
        Statement statement =  null;
        ResultSet resultSet = null;

        try {
            connection = guiJdbcConnection.getConnection(Operation.WRITE);
            statement = connection.createStatement();

            String query = "SELECT VALUE FROM " + SETTINGS_TABLE + " WHERE NAME='" + name + "'";
            resultSet = statement.executeQuery(query);

            String update = "";
            if (resultSet.next()) {
                update = "UPDATE " + SETTINGS_TABLE + " SET VALUE='" + value + "' WHERE NAME='" + name + "'";
            } else {
                update = "INSERT INTO " + SETTINGS_TABLE + "(NAME,VALUE) VALUES ('" + name + "','" + value + "')";
            }

            statement.executeUpdate(update);
            settingsMap.put(name, value);

        } catch (Exception e) {
            log.error(e.getMessage(), e);
        } finally {
            guiJdbcConnection.closeResultSet(resultSet);
            guiJdbcConnection.closeStatement(statement);
            guiJdbcConnection.closeConnection(connection);
        }
    }

    /**
     * Get a Map of all current Settings
     * 
     * @return a HashMap with a name value pairs.
     */
    public HashMap<String, String> getAllSettings() {
        log.trace("Getting all Settings");

        GuiJdbcConnection guiJdbcConnection = new GuiJdbcConnection();
        Connection connection = null;
        Statement statement =  null;
        ResultSet resultSet = null;

        try {
            connection = guiJdbcConnection.getConnection(Operation.READ);
            statement = connection.createStatement();
            String query = "SELECT NAME,VALUE FROM " + SETTINGS_TABLE;
            resultSet = statement.executeQuery(query);

            settingsMap.clear();
            while(resultSet.next()) {
                settingsMap.put(resultSet.getString("NAME"), resultSet.getString("VALUE"));
            }

        } catch (Exception e) {
            log.error(e.getMessage(), e);
        } finally {
            guiJdbcConnection.closeResultSet(resultSet);
            guiJdbcConnection.closeStatement(statement);
            guiJdbcConnection.closeConnection(connection);
        }

        return settingsMap;
    }

}
