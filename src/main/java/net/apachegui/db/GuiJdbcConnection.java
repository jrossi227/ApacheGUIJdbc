package net.apachegui.db;

import java.sql.Connection;

public class GuiJdbcConnection extends JdbcConnection{

    private final static String GUI_DATABASE_FILE = "apachegui-gui-database.db";

    public GuiJdbcConnection() {
        super("GuiJdbc.lock");
    }

    public Connection getConnection() {
        return super.getConnection(GUI_DATABASE_FILE);
    }
}
