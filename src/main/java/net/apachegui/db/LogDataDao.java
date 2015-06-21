package net.apachegui.db;

import net.apachegui.locks.Operation;
import org.apache.log4j.Logger;

import java.sql.*;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;

public class LogDataDao {
    private static Logger log = Logger.getLogger(LogDataDao.class);

    private static final String LOG_TABLE = "LOGDATA";

    private static LogDataDao instance = null;

    public static LogDataDao getInstance() {

        synchronized (LogDataDao.class) {
            if(instance == null) {
                synchronized (LogDataDao.class) {
                    instance = new LogDataDao();
                }
            }
        }

        return instance;
    }

    public void clearDatabase() {

        LogDataJdbcConnection logDataJdbcConnection = new LogDataJdbcConnection();
        Connection connection = null;
        Statement statement = null;
        try {
            connection = logDataJdbcConnection.getConnection(Operation.WRITE);
            statement = connection.createStatement();

            String update = "DELETE FROM LOGDATA";
            statement.executeUpdate(update);

            update = "VACUUM";
            statement.executeUpdate(update);

        } catch (Exception e) {
            log.error(e.getMessage(), e);
        } finally {
            logDataJdbcConnection.closeStatement(statement);
            logDataJdbcConnection.closeConnection(connection);
        }


    }

    /**
     * Writes the log data directly to the database.
     * 
     * @param data
     *            - an Array of LogData to write to the database.
     */
    public void commitLogData(final LogData data[]) {

        LogDataJdbcConnection logDataJdbcConnection = new LogDataJdbcConnection();
        Connection connection = null;
        PreparedStatement preparedStatement =  null;

        try {
            connection = logDataJdbcConnection.getConnection(Operation.WRITE);
            connection.setAutoCommit(false);

            String update = "INSERT INTO " + LOG_TABLE + " (HOST,INSERTDATE,USERAGENT,REQUESTSTRING,STATUS,CONTENTSIZE) VALUES (?,?,?,?,?,?)";
            preparedStatement = connection.prepareStatement(update);

            LogData logData;
            for(int i=0; i<data.length; i++) {
                logData = data[i];

                preparedStatement.setString(1, logData.getHost());
                preparedStatement.setLong(2, logData.getInsertDate().getTime());
                preparedStatement.setString(3, logData.getUserAgent());
                preparedStatement.setString(4, logData.getRequestString());
                preparedStatement.setString(5, logData.getStatus());
                preparedStatement.setString(6, logData.getContentSize());

                preparedStatement.addBatch();
            }

            preparedStatement.executeBatch();
            connection.commit();

        } catch (Exception e) {
            log.error(e.getMessage(), e);
        } finally {
            logDataJdbcConnection.closeStatement(preparedStatement);
            logDataJdbcConnection.closeConnection(connection);
        }
    }

    /**
     * Returns a Log Data query in descending order of date.
     * 
     * @param startDate
     *            - The start date to query. This value is required.
     * @param endDate
     *            - The end date to query. This value is required.
     * @param host
     *            - The host to query. The supplied host can be a substring and is not case sensitive. Can be left blank if its not required.
     * @param userAgent
     *            - The userAgent to query. The supplied user agent can be a substring and is not case sensitive. Can be left blank if its not required.
     * @param requestString
     *            - The request String to query. The supplied request string can be a substring and is not case sensitive. Can be left blank if its not required.
     * @param status
     *            - An integer with the desired status. The supplied status must exactly match the status in the database. Can be left blank if its not required.
     * @param contentSize
     *            - An integer with the desired contentSize. The supplied contentSize must exactly match the status in the database. Can be left blank if its not required.
     * @param maxResults
     *            - The maximum amounts of results to return.
     * @return an array of LogData results.
     * @throws SQLException
     */
    public String generateLogDataQuery(Timestamp startDate, Timestamp endDate, String host, String userAgent, String requestString, String status, String contentSize, String maxResults)
            throws SQLException {
        log.trace("Entering queryLogData");
        log.trace("startDate " + startDate.toString());
        log.trace("endDate " + endDate.toString());
        log.trace("host " + host);
        log.trace("userAgent " + userAgent);
        log.trace("requestString " + requestString);
        log.trace("status " + status);
        log.trace("contentSize " + contentSize);
        log.trace("maxResults " + maxResults);
        StringBuffer query = new StringBuffer();

        query.append("SELECT * \nFROM " + LOG_TABLE + " \nWHERE INSERTDATE > " + startDate.getTime() + " \nAND INSERTDATE < " + endDate.getTime());
        if (!host.equals("")) {
            query.append(" \nAND UPPER(HOST) LIKE '%" + host.toUpperCase() + "%'");
        }

        if (!userAgent.equals("")) {
            query.append(" \nAND UPPER(USERAGENT) LIKE '%" + userAgent.toUpperCase() + "%'");
        }

        if (!requestString.equals("")) {
            query.append(" \nAND UPPER(REQUESTSTRING) LIKE '%" + requestString.toUpperCase() + "%'");
        }

        if (!status.equals("")) {
            query.append(" \nAND STATUS='" + status + "'");
        }

        if (!contentSize.equals("")) {
            query.append(" \nAND CONTENTSIZE='" + contentSize + "'");
        }

        query.append(" \nORDER BY INSERTDATE DESC LIMIT " + maxResults);
        log.trace("Query: " + query.toString());

        return query.toString();
    };

    public LogData[] executeLogDataQuery(String query) throws Exception {
        LogDataJdbcConnection logDataJdbcConnection = new LogDataJdbcConnection();
        Connection connection = null;
        Statement statement =  null;
        ResultSet resultSet = null;

        List<LogData> logData = new ArrayList<LogData>();
        try {
            connection = logDataJdbcConnection.getConnection(Operation.READ);
            statement = connection.createStatement();
            resultSet = statement.executeQuery(query.toString());

            while(resultSet.next()) {
                Timestamp insertDateResult = new Timestamp((resultSet.getLong("INSERTDATE") * 1000));
                log.trace("INSERTDATE " + insertDateResult.toString());
                String hostResult = resultSet.getString("HOST");
                log.trace("HOST "+hostResult);
                String userAgentResult = resultSet.getString("USERAGENT");
                log.trace("USERAGENT " + userAgentResult);
                String requestStringResult = resultSet.getString("REQUESTSTRING");
                log.trace("REQUESTSTRING "+requestStringResult);
                String statusResult = resultSet.getString("STATUS");
                log.trace("STATUS " + statusResult);
                String contentSizeResult = resultSet.getString("CONTENTSIZE");
                log.trace("CONTENTSIZE "+contentSizeResult);

                logData.add(new LogData(insertDateResult, hostResult, userAgentResult, requestStringResult, statusResult, contentSizeResult));
            }

        } finally {
            logDataJdbcConnection.closeResultSet(resultSet);
            logDataJdbcConnection.closeStatement(statement);
            logDataJdbcConnection.closeConnection(connection);
        }

        return logData.toArray(new LogData[logData.size()]);
    }

    /**
     * @param startDate     The start date to query. This value is required.
     * @param endDate       The end date to query. This value is required.
     * @param host          The host to query. The supplied host can be a substring and is not case sensitive. Can be left blank if its not required.
     * @param userAgent     The userAgent to query. The supplied user agent can be a substring and is not case sensitive. Can be left blank if its not required.
     * @param requestString The request String to query. The supplied request string can be a substring and is not case sensitive. Can be left blank if its not required.
     * @param status        An integer with the desired status. The supplied status must exactly match the status in the database. Can be left blank if its not required.
     * @param contentSize   An integer with the desired contentSize. The supplied contentSize must exactly match the status in the database. Can be left blank if its not required.
     * @return a String with the update query
     */
    public String generateDeleteLogDataUpdate(Timestamp startDate, Timestamp endDate, String host, String userAgent, String requestString, String status, String contentSize) {
        log.trace("Entering deleteLogData");
        log.trace("startDate " + startDate.toString());
        log.trace("endDate " + endDate.toString());
        log.trace("host " + host);
        log.trace("userAgent " + userAgent);
        log.trace("requestString " + requestString);
        log.trace("status " + status);
        log.trace("contentSize " + contentSize);
        StringBuffer update = new StringBuffer();
        update.append("DELETE FROM " + LOG_TABLE + " \nWHERE INSERTDATE > " + startDate.getTime() + " \nAND INSERTDATE < " + endDate.getTime());

        if (!host.equals("")) {
            update.append(" \nAND UPPER(HOST) LIKE '%" + host.toUpperCase() + "%'");
        }

        if (!userAgent.equals("")) {
            update.append(" \nAND UPPER(USERAGENT) LIKE '%" + userAgent.toUpperCase() + "%'");
        }

        if (!requestString.equals("")) {
            update.append(" \nAND UPPER(REQUESTSTRING) LIKE '%" + requestString.toUpperCase() + "%'");
        }

        if (!status.equals("")) {
            update.append(" \nAND STATUS='" + status + "'");
        }

        if (!contentSize.equals("")) {
            update.append(" \nAND CONTENTSIZE='" + contentSize + "'");
        }

        log.trace("Update: " + update.toString());

        return update.toString();
    }

    public void executeDeleteLogDataUpdate(String update) {

        LogDataJdbcConnection logDataJdbcConnection = new LogDataJdbcConnection();
        Connection connection = null;
        Statement statement =  null;

        try {
            connection = logDataJdbcConnection.getConnection(Operation.WRITE);
            statement = connection.createStatement();
            statement.executeUpdate(update);

        } catch (Exception e) {
            log.error(e.getMessage(), e);
        } finally {
            logDataJdbcConnection.closeStatement(statement);
            logDataJdbcConnection.closeConnection(connection);
        }

    }

    /**
     * 
     * @param numberOfDays
     *            - Any data older than the supplied number of days will be deleted.
     */
    public void shrinkLogData(int numberOfDays) throws Exception {
        log.trace("Entering shrinkLogData with numberOf Days " + numberOfDays);

        Calendar cal = Calendar.getInstance();
        cal.add(Calendar.DAY_OF_YEAR, ((-1) * numberOfDays));
        Timestamp date = new Timestamp(cal.getTimeInMillis());

        String update = "DELETE FROM " + LOG_TABLE + " WHERE INSERTDATE < " + date.getTime();
        log.trace("Update: " + update);

        LogDataJdbcConnection logDataJdbcConnection = new LogDataJdbcConnection();
        Connection connection = null;
        Statement statement =  null;

        try {
            connection = logDataJdbcConnection.getConnection(Operation.WRITE);
            statement = connection.createStatement();
            statement.executeUpdate(update);

        } catch (Exception e) {
            log.error(e.getMessage(), e);
        } finally {
            logDataJdbcConnection.closeStatement(statement);
            logDataJdbcConnection.closeConnection(connection);
        }
    }

    /**
     * 
     * @return the number of log entries in the database.
     */
    public int getNumberOfEntries() {
        log.trace("Entering getNumberOfEntries");

        LogDataJdbcConnection logDataJdbcConnection = new LogDataJdbcConnection();
        Connection connection = null;
        Statement statement =  null;
        ResultSet resultSet = null;

        int value = 0;
        try {
            connection = logDataJdbcConnection.getConnection(Operation.READ);
            statement = connection.createStatement();

            String query = "SELECT COUNT(INSERTDATE) AS NUMROWS FROM " + LOG_TABLE;
            resultSet = statement.executeQuery(query);
            if(resultSet.next()) {
                value = resultSet.getInt("NUMROWS");
            }

            if (value == 0) {
                log.trace("There were no entries found in the table");
            }

        } catch (Exception e) {
            log.error(e.getMessage(), e);
        } finally {
            logDataJdbcConnection.closeResultSet(resultSet);
            logDataJdbcConnection.closeStatement(statement);
            logDataJdbcConnection.closeConnection(connection);
        }

        return value;
    }

    /**
     * 
     * @return the oldest time in the database.
     */
    public Timestamp getOldestTime() throws Exception {
        log.trace("Entering getOldestTime");

        LogDataJdbcConnection logDataJdbcConnection = new LogDataJdbcConnection();
        Connection connection = null;
        Statement statement =  null;
        ResultSet resultSet = null;

        long value = 0;
        Timestamp time = null;
        try {
            connection = logDataJdbcConnection.getConnection(Operation.READ);
            statement = connection.createStatement();

            String query = "SELECT MIN(INSERTDATE) AS MINTIME FROM " + LOG_TABLE;
            resultSet = statement.executeQuery(query);
            if(resultSet.next()) {
                value = resultSet.getLong("MINTIME");
            }

            if (value != 0) {
                time = new Timestamp(value * 1000);
                log.trace("Minimum Time " + time.toString());
            } else {
                log.trace("There were no entries found in the database");
            }

        } catch (Exception e) {
            log.error(e.getMessage(), e);
        } finally {
            logDataJdbcConnection.closeResultSet(resultSet);
            logDataJdbcConnection.closeStatement(statement);
            logDataJdbcConnection.closeConnection(connection);
        }

        return time;
    }

    /**
     * 
     * @return the newest time in the database
     */
    public Timestamp getNewestTime() throws Exception {
        log.trace("Entering getNewestTime");

        LogDataJdbcConnection logDataJdbcConnection = new LogDataJdbcConnection();
        Connection connection = null;
        Statement statement =  null;
        ResultSet resultSet = null;

        long value = 0;
        Timestamp time = null;
        try {
            connection = logDataJdbcConnection.getConnection(Operation.READ);
            statement = connection.createStatement();

            String query = "SELECT MAX(INSERTDATE) AS MAXTIME FROM " + LOG_TABLE;
            resultSet = statement.executeQuery(query);
            if(resultSet.next()) {
                value = resultSet.getLong("MAXTIME");
            }

            if (value != 0) {
                time = new Timestamp(value * 1000);
                log.trace("Maximum Time " + time.toString());
            } else {
                log.trace("There were no entries found in the database");
            }

        } catch (Exception e) {
            log.error(e.getMessage(), e);
        } finally {
            logDataJdbcConnection.closeResultSet(resultSet);
            logDataJdbcConnection.closeStatement(statement);
            logDataJdbcConnection.closeConnection(connection);
        }

        return time;
    }

    /**
     * Gets the total amount of transactions per hour over a day.
     * 
     * @param date
     *            - The date to query. Only the day month and year are used.
     * @param host
     *            - The host to query. The supplied host can be a substring and is not case sensitive. Can be left blank if its not required.
     * @param userAgent
     *            - The userAgent to query. The supplied user agent can be a substring and is not case sensitive. Can be left blank if its not required.
     * @param requestString
     *            - The request String to query. The supplied request string can be a substring and is not case sensitive. Can be left blank if its not required.
     * @param status
     *            - An integer with the desired status. The supplied status must exactly match the status in the database. Can be left blank if its not required.
     * @param contentSize
     *            - An integer with the desired contentSize. The supplied contentSize must exactly match the status in the database. Can be left blank if its not required.
     * @return an array with length 24 which has the amount of entries per hour.
     */
    public String generateDailyReportByHourQuery(Timestamp date, String host, String userAgent, String requestString, String status, String contentSize) throws Exception {
        // select h, count(*) as NUM from (select hour(INSERTDATE) from LOGDATA WHERE INSERTDATE < TIMESTAMP('2011-12-23 00:00:00.0') AND INSERTDATE > TIMESTAMP('2011-12-22 00:00:00.0')) as t(h) group
        // by h;
        log.trace("Entering getDailyReportByHour");
        log.trace("date " + date.toString());
        log.trace("host " + host);
        log.trace("userAgent " + userAgent);
        log.trace("requestString " + requestString);
        log.trace("status " + status);
        log.trace("contentSize " + contentSize);

        Timestamp currentTime = date;
        Calendar tempPresent = Calendar.getInstance();
        tempPresent.setTimeInMillis(currentTime.getTime() * 1000);
        tempPresent.set(Calendar.HOUR_OF_DAY, 0);
        tempPresent.set(Calendar.MINUTE, 0);
        tempPresent.set(Calendar.SECOND, 0);
        tempPresent.set(Calendar.MILLISECOND, 0);
        currentTime.setTime(tempPresent.getTimeInMillis());
        tempPresent.add(Calendar.DAY_OF_YEAR, 1);
        Timestamp futureTime = new Timestamp(tempPresent.getTimeInMillis());

        StringBuffer query = new StringBuffer();

        query.append("SELECT h, \ncount(*) as NUM \nfrom (\nSELECT strftime('%H',datetime(\nINSERTDATE\n, 'unixepoch'\n, 'localtime'\n)) as h \nFROM " + LOG_TABLE + " \nWHERE INSERTDATE < " + futureTime.getTime()
                + " \nAND INSERTDATE > " + currentTime.getTime());
        if (!host.equals("")) {
            query.append(" \nAND UPPER(HOST) LIKE '%" + host.toUpperCase() + "%'");
        }

        if (!userAgent.equals("")) {
            query.append(" \nAND UPPER(USERAGENT) LIKE '%" + userAgent.toUpperCase() + "%'");
        }

        if (!requestString.equals("")) {
            query.append(" \nAND UPPER(REQUESTSTRING) LIKE '%" + requestString.toUpperCase() + "%'");
        }

        if (!status.equals("")) {
            query.append(" \nAND STATUS='" + status + "'");
        }

        if (!contentSize.equals("")) {
            query.append(" \nAND CONTENTSIZE='" + contentSize + "'");
        }

        query.append("\n) \ngroup by h");
        log.trace("Query: " + query.toString());

       return query.toString();
    }

    public int[] executeDailyReportByHourQuery(String query) throws SQLException {

        LogDataJdbcConnection logDataJdbcConnection = new LogDataJdbcConnection();
        Connection connection = null;
        Statement statement =  null;
        ResultSet resultSet = null;

        int hourCount[] = new int[24];
        for (int i = 0; i < hourCount.length; i++) {
            hourCount[i] = 0;
        }

        try {
            connection = logDataJdbcConnection.getConnection(Operation.READ);
            statement = connection.createStatement();
            resultSet = statement.executeQuery(query);

            while(resultSet.next()) {
                hourCount[resultSet.getInt("h")] = resultSet.getInt("NUM");
            }

        } finally {
            logDataJdbcConnection.closeResultSet(resultSet);
            logDataJdbcConnection.closeStatement(statement);
            logDataJdbcConnection.closeConnection(connection);
        }

        return hourCount;
    }


    /**
     * Gets the total amount of transactions per day per month. The 0 position in the returned array should not be used.
     * 
     * @param date
     *            - The date to query. Only the month and year are used.
     * @param host
     *            - The host to query. The supplied host can be a substring and is not case sensitive. Can be left blank if its not required.
     * @param userAgent
     *            - The userAgent to query. The supplied user agent can be a substring and is not case sensitive. Can be left blank if its not required.
     * @param requestString
     *            - The request String to query. The supplied request string can be a substring and is not case sensitive. Can be left blank if its not required.
     * @param status
     *            - An integer with the desired status. The supplied status must exactly match the status in the database. Can be left blank if its not required.
     * @param contentSize
     *            - An integer with the desired contentSize. The supplied contentSize must exactly match the status in the database. Can be left blank if its not required.
     * @return an array with length that matches the amount of the days in the month +1. Each position has the amount of entries per day. The days start at 1 so position 0 is not used.
     */
    public String generateMonthlyReportByDayQuery(Timestamp date, String host, String userAgent, String requestString, String status, String contentSize) throws Exception {
        log.trace("Entering getDailyReportByHour");
        log.trace("date " + date.toString());
        log.trace("host " + host);
        log.trace("userAgent " + userAgent);
        log.trace("requestString " + requestString);
        log.trace("status " + status);
        log.trace("contentSize " + contentSize);

        Timestamp currentTime = date;
        Calendar tempPresent = Calendar.getInstance();
        tempPresent.setTimeInMillis(currentTime.getTime() * 1000);
        tempPresent.set(Calendar.DAY_OF_MONTH, 1);
        tempPresent.set(Calendar.HOUR_OF_DAY, 0);
        tempPresent.set(Calendar.MINUTE, 0);
        tempPresent.set(Calendar.SECOND, 0);
        tempPresent.set(Calendar.MILLISECOND, 0);
        currentTime.setTime(tempPresent.getTimeInMillis());
        tempPresent.add(Calendar.MONTH, 1);
        Timestamp futureTime = new Timestamp(tempPresent.getTimeInMillis());

        StringBuffer query = new StringBuffer();

        query.append("SELECT d, \ncount(*) as NUM \nfrom (\nSELECT strftime('%d',datetime(\nINSERTDATE\n, 'unixepoch'\n, 'localtime'\n)) as d \nFROM " + LOG_TABLE + " \nWHERE INSERTDATE < " + futureTime.getTime()
                + " \nAND INSERTDATE > " + currentTime.getTime());
        if (!host.equals("")) {
            query.append(" \nAND UPPER(HOST) LIKE '%" + host.toUpperCase() + "%'");
        }

        if (!userAgent.equals("")) {
            query.append(" \nAND UPPER(USERAGENT) LIKE '%" + userAgent.toUpperCase() + "%'");
        }

        if (!requestString.equals("")) {
            query.append(" \nAND UPPER(REQUESTSTRING) LIKE '%" + requestString.toUpperCase() + "%'");
        }

        if (!status.equals("")) {
            query.append(" \nAND STATUS='" + status + "'");
        }

        if (!contentSize.equals("")) {
            query.append(" \nAND CONTENTSIZE='" + contentSize + "'");
        }

        query.append("\n) \ngroup by d");
        log.trace("Query: " + query.toString());

        return query.toString();
    }

    public int[] executeMonthlyReportByDayQuery(String query, Timestamp date) throws Exception {

        Calendar tempPresent = Calendar.getInstance();
        tempPresent.setTimeInMillis(date.getTime() * 1000);
        tempPresent.set(Calendar.DAY_OF_MONTH, 1);
        tempPresent.set(Calendar.HOUR_OF_DAY, 0);
        tempPresent.set(Calendar.MINUTE, 0);
        tempPresent.set(Calendar.SECOND, 0);
        tempPresent.set(Calendar.MILLISECOND, 0);
        int numOfdaysInMonth = tempPresent.getActualMaximum(Calendar.DAY_OF_MONTH);

        LogDataJdbcConnection logDataJdbcConnection = new LogDataJdbcConnection();
        Connection connection = null;
        Statement statement =  null;
        ResultSet resultSet = null;

        // Add an additional day for the unused day position
        int dayCount[] = new int[numOfdaysInMonth + 1];
        for (int i = 0; i < dayCount.length; i++) {
            dayCount[i] = 0;
        }

        try {
            connection = logDataJdbcConnection.getConnection(Operation.READ);
            statement = connection.createStatement();
            resultSet = statement.executeQuery(query);

            while(resultSet.next()) {
                dayCount[resultSet.getInt("d")] = resultSet.getInt("NUM");
            }

        } finally {
            logDataJdbcConnection.closeResultSet(resultSet);
            logDataJdbcConnection.closeStatement(statement);
            logDataJdbcConnection.closeConnection(connection);
        }

        return dayCount;
    }

}
