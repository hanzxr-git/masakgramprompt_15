package Database;

import model.IngredientResult;
import model.NutritionResult;

import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;
import java.util.StringJoiner;

public class NutritionResultDAO {

    /**
     * Saves one nutrition_result and all ingredient_result rows.
     *
     * Important: this method saves individual attributes into database columns,
     * not the whole JSON as one blob. raw_json_output is only kept for debugging.
     */
    public int save(NutritionResult result) throws SQLException {
        try (Connection connection = DBConnection.getConnection()) {
            boolean oldAutoCommit = connection.getAutoCommit();
            connection.setAutoCommit(false);

            try {
                deleteExistingResultForExperiment(connection, result.getExperimentId());

                int resultId = insertNutritionResult(connection, result);
                result.setResultId(resultId);

                if (result.getIngredientResults() != null) {
                    for (IngredientResult ingredient : result.getIngredientResults()) {
                        ingredient.setResultId(resultId);
                        insertIngredientResult(connection, ingredient);
                    }
                }

                connection.commit();
                connection.setAutoCommit(oldAutoCommit);
                return resultId;
            } catch (SQLException e) {
                connection.rollback();
                connection.setAutoCommit(oldAutoCommit);
                throw e;
            }
        }
    }

    public NutritionResult findByExperimentId(int experimentId) throws SQLException {
        String sql = "SELECT * FROM nutrition_result WHERE experiment_id = ? ORDER BY result_id DESC LIMIT 1";

        try (Connection connection = DBConnection.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {

            statement.setInt(1, experimentId);
            try (ResultSet rs = statement.executeQuery()) {
                if (rs.next()) {
                    NutritionResult result = mapNutritionResult(rs);
                    result.setIngredientResults(findIngredientsByResultId(connection, result.getResultId()));
                    return result;
                }
            }
        }
        return null;
    }

    private int insertNutritionResult(Connection connection, NutritionResult result) throws SQLException {
        List<ColumnValue> columns = new ArrayList<>();
        add(columns, connection, "nutrition_result", "experiment_id", result.getExperimentId());
        add(columns, connection, "nutrition_result", "recipe_name", result.getRecipeName());
        add(columns, connection, "nutrition_result", "servings_estimated", result.getServingsEstimated());

        add(columns, connection, "nutrition_result", "serving_calories", result.getServingCalories());
        add(columns, connection, "nutrition_result", "serving_total_fat_g", result.getServingTotalFatG());
        add(columns, connection, "nutrition_result", "serving_saturated_fat_g", result.getServingSaturatedFatG());
        add(columns, connection, "nutrition_result", "serving_cholesterol_mg", result.getServingCholesterolMg());
        add(columns, connection, "nutrition_result", "serving_sodium_mg", result.getServingSodiumMg());
        add(columns, connection, "nutrition_result", "serving_carbohydrate_g", result.getServingCarbohydrateG());
        add(columns, connection, "nutrition_result", "serving_fiber_g", result.getServingFiberG());
        add(columns, connection, "nutrition_result", "serving_sugars_g", result.getServingSugarsG());
        add(columns, connection, "nutrition_result", "serving_protein_g", result.getServingProteinG());
        add(columns, connection, "nutrition_result", "serving_vitamin_d_mcg", result.getServingVitaminDMcg());
        add(columns, connection, "nutrition_result", "serving_calcium_mg", result.getServingCalciumMg());
        add(columns, connection, "nutrition_result", "serving_iron_mg", result.getServingIronMg());
        add(columns, connection, "nutrition_result", "serving_potassium_mg", result.getServingPotassiumMg());

        // PDF database design uses total_calories, total_fat_g, etc.
        add(columns, connection, "nutrition_result", "total_calories", result.getTotalCalories());
        add(columns, connection, "nutrition_result", "total_fat_g", result.getTotalFatG());
        add(columns, connection, "nutrition_result", "total_saturated_fat_g", result.getTotalSaturatedFatG());
        add(columns, connection, "nutrition_result", "total_cholesterol_mg", result.getTotalCholesterolMg());
        add(columns, connection, "nutrition_result", "total_sodium_mg", result.getTotalSodiumMg());
        add(columns, connection, "nutrition_result", "total_carbohydrate_g", result.getTotalCarbohydrateG());
        add(columns, connection, "nutrition_result", "total_fiber_g", result.getTotalFiberG());
        add(columns, connection, "nutrition_result", "total_sugars_g", result.getTotalSugarsG());
        add(columns, connection, "nutrition_result", "total_protein_g", result.getTotalProteinG());
        add(columns, connection, "nutrition_result", "total_vitamin_d_mcg", result.getTotalVitaminDMcg());
        add(columns, connection, "nutrition_result", "total_calcium_mg", result.getTotalCalciumMg());
        add(columns, connection, "nutrition_result", "total_iron_mg", result.getTotalIronMg());
        add(columns, connection, "nutrition_result", "total_potassium_mg", result.getTotalPotassiumMg());

        // Some evaluation SQL drafts use total_energy_kcal instead of total_calories.
        add(columns, connection, "nutrition_result", "total_energy_kcal", result.getTotalCalories());
        add(columns, connection, "nutrition_result", "total_carbohydrate_g", result.getTotalCarbohydrateG());

        add(columns, connection, "nutrition_result", "raw_json_output", result.getRawJsonOutput());
        add(columns, connection, "nutrition_result", "json_valid", result.isJsonValid());

        String sql = buildInsertSql("nutrition_result", columns);

        try (PreparedStatement statement = connection.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            setValues(statement, columns);
            statement.executeUpdate();

            try (ResultSet keys = statement.getGeneratedKeys()) {
                if (keys.next()) {
                    return keys.getInt(1);
                }
            }
        }
        throw new SQLException("Failed to insert nutrition_result; no generated key returned.");
    }

    private void insertIngredientResult(Connection connection, IngredientResult ingredient) throws SQLException {
        List<ColumnValue> columns = new ArrayList<>();
        add(columns, connection, "ingredient_result", "result_id", ingredient.getResultId());
        add(columns, connection, "ingredient_result", "name_original", ingredient.getNameOriginal());
        add(columns, connection, "ingredient_result", "name_en", ingredient.getNameEn());

        add(columns, connection, "ingredient_result", "quantity_expression", ingredient.getQuantityExpression());
        add(columns, connection, "ingredient_result", "quantity_category", ingredient.getQuantityCategory());
        add(columns, connection, "ingredient_result", "quantity_value", ingredient.getQuantityValue());
        add(columns, connection, "ingredient_result", "unit_original", ingredient.getUnitOriginal());
        add(columns, connection, "ingredient_result", "unit_en", ingredient.getUnitEn());
        add(columns, connection, "ingredient_result", "language_tag", ingredient.getLanguageTag());
        add(columns, connection, "ingredient_result", "is_hallucinated", ingredient.getHallucinated());

        add(columns, connection, "ingredient_result", "estimated_weight_g", ingredient.getEstimatedWeightG());
        add(columns, connection, "ingredient_result", "calories", ingredient.getCalories());
        add(columns, connection, "ingredient_result", "energy_kcal", ingredient.getCalories());
        add(columns, connection, "ingredient_result", "total_fat_g", ingredient.getTotalFatG());
        add(columns, connection, "ingredient_result", "fat_g", ingredient.getTotalFatG());
        add(columns, connection, "ingredient_result", "saturated_fat_g", ingredient.getSaturatedFatG());
        add(columns, connection, "ingredient_result", "cholesterol_mg", ingredient.getCholesterolMg());
        add(columns, connection, "ingredient_result", "sodium_mg", ingredient.getSodiumMg());
        add(columns, connection, "ingredient_result", "total_carbohydrate_g", ingredient.getTotalCarbohydrateG());
        add(columns, connection, "ingredient_result", "carbohydrate_g", ingredient.getTotalCarbohydrateG());
        add(columns, connection, "ingredient_result", "dietary_fiber_g", ingredient.getDietaryFiberG());
        add(columns, connection, "ingredient_result", "total_sugars_g", ingredient.getTotalSugarsG());
        add(columns, connection, "ingredient_result", "protein_g", ingredient.getProteinG());
        add(columns, connection, "ingredient_result", "vitamin_d_mcg", ingredient.getVitaminDMcg());
        add(columns, connection, "ingredient_result", "calcium_mg", ingredient.getCalciumMg());
        add(columns, connection, "ingredient_result", "iron_mg", ingredient.getIronMg());
        add(columns, connection, "ingredient_result", "potassium_mg", ingredient.getPotassiumMg());

        String sql = buildInsertSql("ingredient_result", columns);

        try (PreparedStatement statement = connection.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            setValues(statement, columns);
            statement.executeUpdate();

            try (ResultSet keys = statement.getGeneratedKeys()) {
                if (keys.next()) {
                    ingredient.setIngredientId(keys.getInt(1));
                }
            }
        }
    }
    
    

    private void deleteExistingResultForExperiment(Connection connection, int experimentId) throws SQLException {
        Integer existingResultId = null;
        String findSql = "SELECT result_id FROM nutrition_result WHERE experiment_id = ?";

        try (PreparedStatement statement = connection.prepareStatement(findSql)) {
            statement.setInt(1, experimentId);
            try (ResultSet rs = statement.executeQuery()) {
                if (rs.next()) {
                    existingResultId = rs.getInt("result_id");
                }
            }
        }

        if (existingResultId != null) {
            try (PreparedStatement statement = connection.prepareStatement("DELETE FROM ingredient_result WHERE result_id = ?")) {
                statement.setInt(1, existingResultId);
                statement.executeUpdate();
            }
            try (PreparedStatement statement = connection.prepareStatement("DELETE FROM nutrition_result WHERE result_id = ?")) {
                statement.setInt(1, existingResultId);
                statement.executeUpdate();
            }
        }
    }

    private List<IngredientResult> findIngredientsByResultId(Connection connection, int resultId) throws SQLException {
        List<IngredientResult> ingredients = new ArrayList<>();
        String sql = "SELECT * FROM ingredient_result WHERE result_id = ? ORDER BY ingredient_id";

        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setInt(1, resultId);
            try (ResultSet rs = statement.executeQuery()) {
                while (rs.next()) {
                    IngredientResult ingredient = new IngredientResult();
                    ingredient.setIngredientId(getInt(rs, "ingredient_id"));
                    ingredient.setResultId(getInt(rs, "result_id"));
                    ingredient.setNameOriginal(getString(rs, "name_original"));
                    ingredient.setNameEn(getString(rs, "name_en"));
                    ingredient.setQuantityExpression(getString(rs, "quantity_expression"));
                    ingredient.setQuantityCategory(getString(rs, "quantity_category"));
                    ingredient.setQuantityValue(getDouble(rs, "quantity_value"));
                    ingredient.setUnitOriginal(getString(rs, "unit_original"));
                    ingredient.setUnitEn(getString(rs, "unit_en"));
                    ingredient.setLanguageTag(getString(rs, "language_tag"));
                    ingredient.setHallucinated(getBooleanObject(rs, "is_hallucinated"));
                    ingredient.setEstimatedWeightG(getDouble(rs, "estimated_weight_g"));
                    ingredient.setCalories(firstDouble(rs, "calories", "energy_kcal"));
                    ingredient.setTotalFatG(firstDouble(rs, "total_fat_g", "fat_g"));
                    ingredient.setSaturatedFatG(getDouble(rs, "saturated_fat_g"));
                    ingredient.setCholesterolMg(getDouble(rs, "cholesterol_mg"));
                    ingredient.setSodiumMg(getDouble(rs, "sodium_mg"));
                    ingredient.setTotalCarbohydrateG(firstDouble(rs, "total_carbohydrate_g", "carbohydrate_g"));
                    ingredient.setDietaryFiberG(getDouble(rs, "dietary_fiber_g"));
                    ingredient.setTotalSugarsG(getDouble(rs, "total_sugars_g"));
                    ingredient.setProteinG(getDouble(rs, "protein_g"));
                    ingredient.setVitaminDMcg(getDouble(rs, "vitamin_d_mcg"));
                    ingredient.setCalciumMg(getDouble(rs, "calcium_mg"));
                    ingredient.setIronMg(getDouble(rs, "iron_mg"));
                    ingredient.setPotassiumMg(getDouble(rs, "potassium_mg"));
                    ingredients.add(ingredient);
                }
            }
        }
        return ingredients;
    }

    private NutritionResult mapNutritionResult(ResultSet rs) {
        NutritionResult result = new NutritionResult();
        result.setResultId(getInt(rs, "result_id"));
        result.setExperimentId(getInt(rs, "experiment_id"));
        result.setRecipeName(getString(rs, "recipe_name"));
        result.setServingsEstimated(getInteger(rs, "servings_estimated"));
        result.setServingCalories(getDouble(rs, "serving_calories"));
        result.setServingTotalFatG(getDouble(rs, "serving_total_fat_g"));
        result.setServingSaturatedFatG(getDouble(rs, "serving_saturated_fat_g"));
        result.setServingCholesterolMg(getDouble(rs, "serving_cholesterol_mg"));
        result.setServingSodiumMg(getDouble(rs, "serving_sodium_mg"));
        result.setServingCarbohydrateG(getDouble(rs, "serving_carbohydrate_g"));
        result.setServingFiberG(getDouble(rs, "serving_fiber_g"));
        result.setServingSugarsG(getDouble(rs, "serving_sugars_g"));
        result.setServingProteinG(getDouble(rs, "serving_protein_g"));
        result.setServingVitaminDMcg(getDouble(rs, "serving_vitamin_d_mcg"));
        result.setServingCalciumMg(getDouble(rs, "serving_calcium_mg"));
        result.setServingIronMg(getDouble(rs, "serving_iron_mg"));
        result.setServingPotassiumMg(getDouble(rs, "serving_potassium_mg"));
        result.setTotalCalories(firstDouble(rs, "total_calories", "total_energy_kcal"));
        result.setTotalFatG(getDouble(rs, "total_fat_g"));
        result.setTotalSaturatedFatG(getDouble(rs, "total_saturated_fat_g"));
        result.setTotalCholesterolMg(getDouble(rs, "total_cholesterol_mg"));
        result.setTotalSodiumMg(getDouble(rs, "total_sodium_mg"));
        result.setTotalCarbohydrateG(getDouble(rs, "total_carbohydrate_g"));
        result.setTotalFiberG(getDouble(rs, "total_fiber_g"));
        result.setTotalSugarsG(getDouble(rs, "total_sugars_g"));
        result.setTotalProteinG(getDouble(rs, "total_protein_g"));
        result.setTotalVitaminDMcg(getDouble(rs, "total_vitamin_d_mcg"));
        result.setTotalCalciumMg(getDouble(rs, "total_calcium_mg"));
        result.setTotalIronMg(getDouble(rs, "total_iron_mg"));
        result.setTotalPotassiumMg(getDouble(rs, "total_potassium_mg"));
        result.setRawJsonOutput(getString(rs, "raw_json_output"));
        Boolean valid = getBooleanObject(rs, "json_valid");
        result.setJsonValid(Boolean.TRUE.equals(valid));
        return result;
    }

    private void add(List<ColumnValue> columns, Connection connection, String table, String column, Object value) throws SQLException {
        if (hasColumn(connection, table, column) && !containsColumn(columns, column)) {
            columns.add(new ColumnValue(column, value));
        }
    }

    private boolean containsColumn(List<ColumnValue> columns, String columnName) {
        for (ColumnValue column : columns) {
            if (column.name.equalsIgnoreCase(columnName)) {
                return true;
            }
        }
        return false;
    }

    private String buildInsertSql(String tableName, List<ColumnValue> columns) {
        StringJoiner columnNames = new StringJoiner(", ");
        StringJoiner placeholders = new StringJoiner(", ");

        for (ColumnValue column : columns) {
            columnNames.add(column.name);
            placeholders.add("?");
        }

        return "INSERT INTO " + tableName + " (" + columnNames + ") VALUES (" + placeholders + ")";
    }

    private void setValues(PreparedStatement statement, List<ColumnValue> columns) throws SQLException {
        for (int i = 0; i < columns.size(); i++) {
            statement.setObject(i + 1, columns.get(i).value);
        }
    }

    private boolean hasColumn(Connection connection, String tableName, String columnName) throws SQLException {
        DatabaseMetaData metaData = connection.getMetaData();
        try (ResultSet columns = metaData.getColumns(connection.getCatalog(), null, tableName, columnName)) {
            return columns.next();
        }
    }

    private static class ColumnValue {
        private final String name;
        private final Object value;

        private ColumnValue(String name, Object value) {
            this.name = name;
            this.value = value;
        }
    }

    private int getInt(ResultSet rs, String column) {
        try {
            return rs.getInt(column);
        } catch (SQLException e) {
            return 0;
        }
    }

    private Integer getInteger(ResultSet rs, String column) {
        try {
            int value = rs.getInt(column);
            return rs.wasNull() ? null : value;
        } catch (SQLException e) {
            return null;
        }
    }

    private Double getDouble(ResultSet rs, String column) {
        try {
            double value = rs.getDouble(column);
            return rs.wasNull() ? null : value;
        } catch (SQLException e) {
            return null;
        }
    }

    private Double firstDouble(ResultSet rs, String... columns) {
        for (String column : columns) {
            Double value = getDouble(rs, column);
            if (value != null) {
                return value;
            }
        }
        return null;
    }

    private String getString(ResultSet rs, String column) {
        try {
            return rs.getString(column);
        } catch (SQLException e) {
            return null;
        }
    }

    private Boolean getBooleanObject(ResultSet rs, String column) {
        try {
            boolean value = rs.getBoolean(column);
            return rs.wasNull() ? null : value;
        } catch (SQLException e) {
            return null;
        }
    }
}
