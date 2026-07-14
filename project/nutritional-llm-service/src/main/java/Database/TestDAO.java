package Database;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

/**
 * Quick checker for important project tables.
 */
public class TestDAO {

    public static void main(String[] args) {
        TestDAO tester = new TestDAO();
        tester.printTableCount("transcript");
        tester.printTableCount("llm_model");
        tester.printTableCount("prompt_technique");
        tester.printTableCount("experiment");
        tester.printTableCount("nutrition_result");
        tester.printTableCount("ingredient_result");
    }

    public void printTableCount(String tableName) {
        String sql = "SELECT COUNT(*) AS total FROM " + tableName;

        try (Connection connection = DBConnection.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql);
             ResultSet resultSet = statement.executeQuery()) {

            if (resultSet.next()) {
                System.out.println(tableName + " rows: " + resultSet.getInt("total"));
            }
        } catch (SQLException e) {
            System.out.println("Cannot read table " + tableName + ": " + e.getMessage());
        }
    }
}
