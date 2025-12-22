package com.microservices.api.util;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

public class DbTestUtils {
    private static final String JDBC_URL = "jdbc:mysql://127.0.0.1:3306/javatechie";
    private static final String USER = "root";
    private static final String PASSWORD = "root";

    public static Connection getConnection() throws SQLException {
        return DriverManager.getConnection(JDBC_URL, USER, PASSWORD);
    }
}
