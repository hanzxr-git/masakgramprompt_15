package Database;

import java.sql.Connection;
import java.sql.SQLException;

/**
 * Simple database connection test.
 */
public class TestDB {

    public static void main(String[] args) {
        try (Connection connection = DBConnection.getConnection()) {
            System.out.println("Database Connected!");
            System.out.println("Catalog: " + connection.getCatalog());
        } catch (SQLException e) {
            System.out.println("Database connection failed!");
            e.printStackTrace();
        }
    }
}
