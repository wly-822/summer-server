package cn.cerc.db.mysql;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import cn.cerc.core.IConfig;
import cn.cerc.core.IConnection;
import cn.cerc.db.core.ServerConfig;

public abstract class SqlConnection implements IConnection, AutoCloseable {
    private static final Logger log = LoggerFactory.getLogger(SqlConnection.class);
    protected String url;
    protected String user;
    protected String pwd;
    private int tag;
    protected Connection connection;
    protected IConfig config;

    public SqlConnection() {
        config = ServerConfig.getInstance();
    }

    @Override
    public void close() throws Exception {
        try {
            if (connection != null) {
                log.debug("close connection.");
                connection.close();
                connection = null;
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    @Override
    public Connection getClient() {
        if (connection != null)
            return connection;

        try {
            if (url == null)
                url = getConnectUrl();
            log.debug("create connection for mysql: " + url);
            Class.forName("com.mysql.jdbc.Driver");
            connection = DriverManager.getConnection(url, user, pwd);
            return connection;
        } catch (SQLException | ClassNotFoundException e) {
            throw new RuntimeException(e);
        }
    }

    public boolean execute(String sql) {
        try {
            log.debug(sql);
            Statement st = getClient().createStatement();
            st.execute(sql);
            return true;
        } catch (SQLException e) {
            log.error("error sql: " + sql);
            throw new RuntimeException(e.getMessage());
        }
    }

    public IConfig getConfig() {
        return config;
    }

    @Override
    public void setConfig(IConfig config) {
        if (this.config != config)
            url = null;
        this.config = config;
    }

    public int getTag() {
        return tag;
    }

    public void setTag(int tag) {
        this.tag = tag;
    }

    protected abstract String getConnectUrl();

}
