package net.apachegui.db;

import net.apachegui.locks.Operation;

import java.sql.Connection;

public class GuiJdbcConnection extends JdbcConnection{

    private final static String GUI_DATABASE_FILE = "apachegui-gui-database.db";

    public GuiJdbcConnection() {
        super("apachegui-gui-database.lock");
    }

    public Connection getConnection(Operation operation) {
        return super.getConnection(GUI_DATABASE_FILE, operation);
    }
}
