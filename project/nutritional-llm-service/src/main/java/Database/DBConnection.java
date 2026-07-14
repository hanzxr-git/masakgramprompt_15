package Database;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

/**
 * Central database connection class.
 *
 * Change DB_NAME, USERNAME and PASSWORD according to your local MySQL/XAMPP setup.
 * Default XAMPP is usually username "root" with an empty password.
 */
public class DBConnection {

    private static final String DB_NAME = "masakgramprompt";
    private static final String HOST = "localhost";
    private static final String PORT = "3306";

    private static final String URL = "jdbc:mysql://" + HOST + ":" + PORT + "/" + DB_NAME
            + "?useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=Asia/Kuala_Lumpur";

    private static final String USERNAME = "root";
    private static final String PASSWORD = "";

    private DBConnection() {
        // Utility class, do not instantiate.
    }

    public static Connection getConnection() throws SQLException {
        try {
            Class.forName("com.mysql.cj.jdbc.Driver");
        } catch (ClassNotFoundException e) {
            throw new SQLException("MySQL JDBC Driver not found. Check mysql-connector-j dependency in pom.xml", e);
        }
        return DriverManager.getConnection(URL, USERNAME, PASSWORD);
    }

    public static boolean testConnection() {
        try (Connection connection = getConnection()) {
            return connection != null && !connection.isClosed();
        } catch (SQLException e) {
            System.err.println("Database connection failed: " + e.getMessage());
            return false;
        }
    }
}
