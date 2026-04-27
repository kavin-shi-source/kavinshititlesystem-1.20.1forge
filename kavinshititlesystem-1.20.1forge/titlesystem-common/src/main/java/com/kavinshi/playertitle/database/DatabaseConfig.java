package com.kavinshi.playertitle.database;

public class DatabaseConfig {
    private String serverMode = "single"; // single or cluster
    private String host = "localhost";
    private int port = 3306;
    private String database = "playertitle";
    private String username = "root";
    private String password = "";
    private int poolSize = 10;
    private int timeout = 5000;

    public String getServerMode() { return serverMode; }
    public void setServerMode(String serverMode) { this.serverMode = serverMode; }

    public String getHost() { return host; }
    public void setHost(String host) { this.host = host; }

    public int getPort() { return port; }
    public void setPort(int port) { this.port = port; }

    public String getDatabase() { return database; }
    public void setDatabase(String database) { this.database = database; }

    public String getUsername() { return username; }
    public void setUsername(String username) { this.username = username; }

    public String getPassword() { return password; }
    public void setPassword(String password) { this.password = password; }

    public int getPoolSize() { return poolSize; }
    public void setPoolSize(int poolSize) { this.poolSize = poolSize; }

    public int getTimeout() { return timeout; }
    public void setTimeout(int timeout) { this.timeout = timeout; }
}
