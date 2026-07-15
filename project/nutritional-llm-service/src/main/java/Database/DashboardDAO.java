package Database;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.sql.ResultSetMetaData;
import java.util.ArrayList;
import java.util.List;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;


public class DashboardDAO {

    public String getDashboardSummary() {
        StringBuilder summary = new StringBuilder();

        try (Connection conn = DBConnection.getConnection()) {

            int totalReels = count(conn, "SELECT COUNT(*) FROM reel");
            int totalTranscripts = count(conn, "SELECT COUNT(*) FROM transcript");
            int totalExperiments = count(conn, "SELECT COUNT(*) FROM experiment");
            int completed = count(conn, "SELECT COUNT(*) FROM experiment WHERE status = 'completed'");
            int running = count(conn, "SELECT COUNT(*) FROM experiment WHERE status = 'running'");
            int failed = count(conn, "SELECT COUNT(*) FROM experiment WHERE status = 'failed'");

            summary.append("TOTAL_REELS=").append(totalReels).append("\n");
            summary.append("TOTAL_TRANSCRIPTS=").append(totalTranscripts).append("\n");
            summary.append("TOTAL_EXPERIMENTS=").append(totalExperiments).append("\n");
            summary.append("COMPLETED=").append(completed).append("\n");
            summary.append("RUNNING=").append(running).append("\n");
            summary.append("FAILED=").append(failed).append("\n");

        } catch (SQLException e) {
            return "ERROR loading dashboard summary: " + e.getMessage();
        }

        return summary.toString();
    }

    public String getReelListPreview() {
        StringBuilder result = new StringBuilder();

        String sql =
                "SELECT " +
                "r.reel_id, " +
                "r.reel_id_instagram, " +
                "r.reel_url, " +
                "i.instagram_account, " +
                "t.transcript_id, " +
                "t.file_name AS transcript_file, " +
                "gtr.gt_reel_id " +
                "FROM reel r " +
                "LEFT JOIN influencer i ON r.influencer_id = i.influencer_id " +
                "LEFT JOIN transcript t ON r.reel_id = t.reel_id " +
                "LEFT JOIN ground_truth_reel gtr ON t.transcript_id = gtr.transcript_id " +
                "ORDER BY r.reel_id DESC " +
                "LIMIT 15";

        try (
                Connection conn = DBConnection.getConnection();
                PreparedStatement stmt = conn.prepareStatement(sql);
                ResultSet rs = stmt.executeQuery()
        ) {
            result.append("Latest Reels\n");
            result.append("==================================================\n\n");

            while (rs.next()) {
                int reelId = rs.getInt("reel_id");
                String instagramId = rs.getString("reel_id_instagram");
                String url = rs.getString("reel_url");
                String handle = rs.getString("instagram_account");
                int transcriptId = rs.getInt("transcript_id");
                String transcriptFile = rs.getString("transcript_file");
                int gtId = rs.getInt("gt_reel_id");

                boolean hasTranscript = transcriptId > 0;
                boolean hasGroundTruth = gtId > 0;

                result.append("Reel ID: ").append(reelId).append("\n");
                result.append("Instagram ID: ").append(nullToDash(instagramId)).append("\n");
                result.append("Influencer: @").append(nullToDash(handle)).append("\n");
                result.append("Transcript: ").append(hasTranscript ? "Available" : "Missing").append("\n");
                result.append("Transcript File: ").append(nullToDash(transcriptFile)).append("\n");
                result.append("Ground Truth: ").append(hasGroundTruth ? "Available" : "Missing").append("\n");
                result.append("URL: ").append(nullToDash(url)).append("\n");
                result.append("--------------------------------------------------\n");
            }

        } catch (SQLException e) {
            return "ERROR loading reel list: " + e.getMessage();
        }

        return result.toString();
    }

    public String getRecentExperiments() {
        StringBuilder result = new StringBuilder();

        String sql =
                "SELECT " +
                "e.experiment_id, " +
                "e.transcript_id, " +
                "m.model_name, " +
                "pt.technique_name, " +
                "e.rag_enabled, " +
                "e.status, " +
                "e.executed_at " +
                "FROM experiment e " +
                "LEFT JOIN llm_model m ON e.model_id = m.model_id " +
                "LEFT JOIN prompt_technique pt ON e.technique_id = pt.technique_id " +
                "ORDER BY e.experiment_id DESC " +
                "LIMIT 20";

        try (
                Connection conn = DBConnection.getConnection();
                PreparedStatement stmt = conn.prepareStatement(sql);
                ResultSet rs = stmt.executeQuery()
        ) {
            result.append("Recent Experiments\n");
            result.append("==================================================\n\n");

            boolean hasData = false;

            while (rs.next()) {
                hasData = true;

                result.append("Experiment ID: ").append(rs.getInt("experiment_id")).append("\n");
                result.append("Transcript ID: ").append(rs.getInt("transcript_id")).append("\n");
                result.append("Model: ").append(nullToDash(rs.getString("model_name"))).append("\n");
                result.append("Technique: ").append(nullToDash(rs.getString("technique_name"))).append("\n");
                result.append("RAG: ").append(rs.getBoolean("rag_enabled") ? "Enabled" : "Disabled").append("\n");
                result.append("Status: ").append(nullToDash(rs.getString("status"))).append("\n");
                result.append("Executed At: ").append(nullToDash(rs.getString("executed_at"))).append("\n");
                result.append("--------------------------------------------------\n");
            }

            if (!hasData) {
                result.append("No experiments found yet.\n");
            }

        } catch (SQLException e) {
            return "ERROR loading recent experiments: " + e.getMessage();
        }

        return result.toString();
    }

    private int count(Connection conn, String sql) throws SQLException {
        try (
                PreparedStatement stmt = conn.prepareStatement(sql);
                ResultSet rs = stmt.executeQuery()
        ) {
            if (rs.next()) {
                return rs.getInt(1);
            }
        }

        return 0;
    }

    private String nullToDash(String value) {
        if (value == null || value.trim().isEmpty()) {
            return "-";
        }

        return value;
    }
    
    public String getLatestNutritionFactSheet() {
        StringBuilder result = new StringBuilder();

        String resultSql =
                "SELECT " +
                "nr.result_id, " +
                "nr.experiment_id, " +
                "nr.recipe_name, " +
                "nr.servings_estimated, " +
                "nr.serving_calories, " +
                "nr.serving_total_fat_g, " +
                "nr.serving_saturated_fat_g, " +
                "nr.serving_cholesterol_mg, " +
                "nr.serving_sodium_mg, " +
                "nr.serving_carbohydrate_g, " +
                "nr.serving_fiber_g, " +
                "nr.serving_sugars_g, " +
                "nr.serving_protein_g, " +
                "nr.serving_vitamin_d_mcg, " +
                "nr.serving_calcium_mg, " +
                "nr.serving_iron_mg, " +
                "nr.serving_potassium_mg, " +
                "nr.total_calories, " +
                "nr.total_fat_g, " +
                "nr.total_saturated_fat_g, " +
                "nr.total_cholesterol_mg, " +
                "nr.total_sodium_mg, " +
                "nr.total_carbohydrate_g, " +
                "nr.total_fiber_g, " +
                "nr.total_sugars_g, " +
                "nr.total_protein_g, " +
                "nr.total_vitamin_d_mcg, " +
                "nr.total_calcium_mg, " +
                "nr.total_iron_mg, " +
                "nr.total_potassium_mg, " +
                "nr.json_valid, " +
                "e.transcript_id, " +
                "e.status, " +
                "m.model_name, " +
                "pt.technique_name " +
                "FROM nutrition_result nr " +
                "JOIN experiment e ON nr.experiment_id = e.experiment_id " +
                "JOIN llm_model m ON e.model_id = m.model_id " +
                "JOIN prompt_technique pt ON e.technique_id = pt.technique_id " +
                "WHERE e.status = 'completed' " +
                "ORDER BY nr.result_id DESC " +
                "LIMIT 1";

        try (
                Connection conn = DBConnection.getConnection();
                PreparedStatement stmt = conn.prepareStatement(resultSql);
                ResultSet rs = stmt.executeQuery()
        ) {
            if (!rs.next()) {
                return "No completed nutrition result found yet.";
            }

            int resultId = rs.getInt("result_id");

            result.append("LATEST NUTRITION FACT SHEET\n");
            result.append("==================================================\n\n");

            result.append("Result ID: ").append(resultId).append("\n");
            result.append("Experiment ID: ").append(rs.getInt("experiment_id")).append("\n");
            result.append("Transcript ID: ").append(rs.getInt("transcript_id")).append("\n");
            result.append("Model: ").append(nullToDash(rs.getString("model_name"))).append("\n");
            result.append("Prompt Technique: ").append(nullToDash(rs.getString("technique_name"))).append("\n");
            result.append("Recipe Name: ").append(nullToDash(rs.getString("recipe_name"))).append("\n");
            result.append("Servings Estimated: ").append(rs.getInt("servings_estimated")).append("\n");
            result.append("JSON Valid: ").append(rs.getBoolean("json_valid") ? "Yes" : "No").append("\n\n");

            result.append("AMOUNT PER SERVING\n");
            result.append("--------------------------------------------------\n");
            result.append("Calories: ").append(rs.getFloat("serving_calories")).append(" kcal\n");
            result.append("Total Fat: ").append(rs.getFloat("serving_total_fat_g")).append(" g\n");
            result.append("Saturated Fat: ").append(rs.getFloat("serving_saturated_fat_g")).append(" g\n");
            result.append("Cholesterol: ").append(rs.getFloat("serving_cholesterol_mg")).append(" mg\n");
            result.append("Sodium: ").append(rs.getFloat("serving_sodium_mg")).append(" mg\n");
            result.append("Carbohydrate: ").append(rs.getFloat("serving_carbohydrate_g")).append(" g\n");
            result.append("Fiber: ").append(rs.getFloat("serving_fiber_g")).append(" g\n");
            result.append("Sugars: ").append(rs.getFloat("serving_sugars_g")).append(" g\n");
            result.append("Protein: ").append(rs.getFloat("serving_protein_g")).append(" g\n");
            result.append("Vitamin D: ").append(rs.getFloat("serving_vitamin_d_mcg")).append(" mcg\n");
            result.append("Calcium: ").append(rs.getFloat("serving_calcium_mg")).append(" mg\n");
            result.append("Iron: ").append(rs.getFloat("serving_iron_mg")).append(" mg\n");
            result.append("Potassium: ").append(rs.getFloat("serving_potassium_mg")).append(" mg\n\n");

            result.append("TOTAL RECIPE NUTRITION\n");
            result.append("--------------------------------------------------\n");
            result.append("Total Calories: ").append(rs.getFloat("total_calories")).append(" kcal\n");
            result.append("Total Fat: ").append(rs.getFloat("total_fat_g")).append(" g\n");
            result.append("Total Saturated Fat: ").append(rs.getFloat("total_saturated_fat_g")).append(" g\n");
            result.append("Total Cholesterol: ").append(rs.getFloat("total_cholesterol_mg")).append(" mg\n");
            result.append("Total Sodium: ").append(rs.getFloat("total_sodium_mg")).append(" mg\n");
            result.append("Total Carbohydrate: ").append(rs.getFloat("total_carbohydrate_g")).append(" g\n");
            result.append("Total Fiber: ").append(rs.getFloat("total_fiber_g")).append(" g\n");
            result.append("Total Sugars: ").append(rs.getFloat("total_sugars_g")).append(" g\n");
            result.append("Total Protein: ").append(rs.getFloat("total_protein_g")).append(" g\n");
            result.append("Total Vitamin D: ").append(rs.getFloat("total_vitamin_d_mcg")).append(" mcg\n");
            result.append("Total Calcium: ").append(rs.getFloat("total_calcium_mg")).append(" mg\n");
            result.append("Total Iron: ").append(rs.getFloat("total_iron_mg")).append(" mg\n");
            result.append("Total Potassium: ").append(rs.getFloat("total_potassium_mg")).append(" mg\n\n");

            result.append(getIngredientResults(conn, resultId));

        } catch (SQLException e) {
            return "ERROR loading nutrition fact sheet: " + e.getMessage();
        }

        return result.toString();
    }
    
    private String getIngredientResults(Connection conn, int resultId) throws SQLException {
        StringBuilder result = new StringBuilder();

        String sql =
                "SELECT " +
                "ingredient_id, " +
                "name_original, " +
                "name_en, " +
                "quantity_value, " +
                "unit_original, " +
                "unit_en, " +
                "estimated_weight_g, " +
                "calories, " +
                "total_fat_g, " +
                "saturated_fat_g, " +
                "cholesterol_mg, " +
                "sodium_mg, " +
                "total_carbohydrate_g, " +
                "dietary_fiber_g, " +
                "total_sugars_g, " +
                "protein_g, " +
                "vitamin_d_mcg, " +
                "calcium_mg, " +
                "iron_mg, " +
                "potassium_mg " +
                "FROM ingredient_result " +
                "WHERE result_id = ? " +
                "ORDER BY ingredient_id";

        try (
                PreparedStatement stmt = conn.prepareStatement(sql)
        ) {
            stmt.setInt(1, resultId);

            try (ResultSet rs = stmt.executeQuery()) {
                result.append("EXTRACTED INGREDIENTS\n");
                result.append("==================================================\n\n");

                boolean hasIngredient = false;

                while (rs.next()) {
                    hasIngredient = true;

                    result.append("Ingredient ID: ").append(rs.getInt("ingredient_id")).append("\n");
                    result.append("Name Original: ").append(nullToDash(rs.getString("name_original"))).append("\n");
                    result.append("Name English: ").append(nullToDash(rs.getString("name_en"))).append("\n");
                    result.append("Quantity: ").append(rs.getFloat("quantity_value")).append(" ")
                            .append(nullToDash(rs.getString("unit_original"))).append("\n");
                    result.append("Unit English: ").append(nullToDash(rs.getString("unit_en"))).append("\n");
                    result.append("Estimated Weight: ").append(rs.getFloat("estimated_weight_g")).append(" g\n");

                    result.append("Nutrition:\n");
                    result.append("  Calories: ").append(rs.getFloat("calories")).append(" kcal\n");
                    result.append("  Total Fat: ").append(rs.getFloat("total_fat_g")).append(" g\n");
                    result.append("  Saturated Fat: ").append(rs.getFloat("saturated_fat_g")).append(" g\n");
                    result.append("  Cholesterol: ").append(rs.getFloat("cholesterol_mg")).append(" mg\n");
                    result.append("  Sodium: ").append(rs.getFloat("sodium_mg")).append(" mg\n");
                    result.append("  Carbohydrate: ").append(rs.getFloat("total_carbohydrate_g")).append(" g\n");
                    result.append("  Fiber: ").append(rs.getFloat("dietary_fiber_g")).append(" g\n");
                    result.append("  Sugars: ").append(rs.getFloat("total_sugars_g")).append(" g\n");
                    result.append("  Protein: ").append(rs.getFloat("protein_g")).append(" g\n");
                    result.append("  Vitamin D: ").append(rs.getFloat("vitamin_d_mcg")).append(" mcg\n");
                    result.append("  Calcium: ").append(rs.getFloat("calcium_mg")).append(" mg\n");
                    result.append("  Iron: ").append(rs.getFloat("iron_mg")).append(" mg\n");
                    result.append("  Potassium: ").append(rs.getFloat("potassium_mg")).append(" mg\n");
                    result.append("--------------------------------------------------\n");
                }

                if (!hasIngredient) {
                    result.append("No ingredient results found for this nutrition result.\n");
                }
            }
        }

        return result.toString();
    }
    
    public String exportCsv(String exportType) {
        String fileName;
        String sql;

        if ("exact_match".equals(exportType)) {
            fileName = "layer1a_exact_match.csv";
            sql =
                    "SELECT " +
                    "e.experiment_id, e.transcript_id, m.model_name, pt.technique_name, e.rag_enabled, " +
                    "ir.name_original AS pred_name_original, ir.name_en AS pred_name_en, " +
                    "ir.unit_original AS pred_unit_original, ir.unit_en AS pred_unit_en " +
                    "FROM experiment e " +
                    "JOIN llm_model m ON e.model_id = m.model_id " +
                    "JOIN prompt_technique pt ON e.technique_id = pt.technique_id " +
                    "JOIN nutrition_result nr ON e.experiment_id = nr.experiment_id " +
                    "JOIN ingredient_result ir ON nr.result_id = ir.result_id " +
                    "WHERE e.status = 'completed' " +
                    "ORDER BY e.experiment_id, ir.ingredient_id";

        } else if ("text_similarity".equals(exportType)) {
            fileName = "layer1b_text_similarity.csv";
            sql =
                    "SELECT " +
                    "e.experiment_id, e.transcript_id, m.model_name, pt.technique_name, e.rag_enabled, " +
                    "ir.name_original AS pred_name_original, ir.name_en AS pred_name_en " +
                    "FROM experiment e " +
                    "JOIN llm_model m ON e.model_id = m.model_id " +
                    "JOIN prompt_technique pt ON e.technique_id = pt.technique_id " +
                    "JOIN nutrition_result nr ON e.experiment_id = nr.experiment_id " +
                    "JOIN ingredient_result ir ON nr.result_id = ir.result_id " +
                    "WHERE e.status = 'completed' " +
                    "ORDER BY e.experiment_id, ir.ingredient_id";

        } else if ("numeric_quantity".equals(exportType)) {
            fileName = "layer2a_numeric_quantity.csv";
            sql =
                    "SELECT " +
                    "e.experiment_id, e.transcript_id, m.model_name, pt.technique_name, e.rag_enabled, " +
                    "ir.quantity_value, ir.estimated_weight_g " +
                    "FROM experiment e " +
                    "JOIN llm_model m ON e.model_id = m.model_id " +
                    "JOIN prompt_technique pt ON e.technique_id = pt.technique_id " +
                    "JOIN nutrition_result nr ON e.experiment_id = nr.experiment_id " +
                    "JOIN ingredient_result ir ON nr.result_id = ir.result_id " +
                    "WHERE e.status = 'completed' " +
                    "ORDER BY e.experiment_id, ir.ingredient_id";

        } else if ("numeric_nutrition".equals(exportType)) {
            fileName = "layer2b_numeric_nutrition.csv";
            sql =
                    "SELECT " +
                    "e.experiment_id, e.transcript_id, m.model_name, pt.technique_name, e.rag_enabled, " +
                    "ir.calories, ir.protein_g, ir.total_fat_g, ir.total_carbohydrate_g, " +
                    "ir.sodium_mg, ir.total_sugars_g, ir.calcium_mg, ir.iron_mg, ir.potassium_mg " +
                    "FROM experiment e " +
                    "JOIN llm_model m ON e.model_id = m.model_id " +
                    "JOIN prompt_technique pt ON e.technique_id = pt.technique_id " +
                    "JOIN nutrition_result nr ON e.experiment_id = nr.experiment_id " +
                    "JOIN ingredient_result ir ON nr.result_id = ir.result_id " +
                    "WHERE e.status = 'completed' " +
                    "ORDER BY e.experiment_id, ir.ingredient_id";

        } else if ("nutrition_totals".equals(exportType)) {
            fileName = "layer2c_nutrition_totals.csv";
            sql =
                    "SELECT " +
                    "e.experiment_id, e.transcript_id, m.model_name, pt.technique_name, e.rag_enabled, " +
                    "nr.recipe_name, nr.total_calories, nr.total_protein_g, nr.total_fat_g, " +
                    "nr.total_carbohydrate_g, nr.total_sodium_mg, nr.total_sugars_g " +
                    "FROM experiment e " +
                    "JOIN llm_model m ON e.model_id = m.model_id " +
                    "JOIN prompt_technique pt ON e.technique_id = pt.technique_id " +
                    "JOIN nutrition_result nr ON e.experiment_id = nr.experiment_id " +
                    "WHERE e.status = 'completed' " +
                    "ORDER BY e.experiment_id";

        } else if ("json_validity".equals(exportType)) {
            fileName = "layer3a_json_validity.csv";
            sql =
                    "SELECT " +
                    "m.model_name, pt.technique_name, e.rag_enabled, " +
                    "COUNT(*) AS total_runs, " +
                    "SUM(CASE WHEN nr.json_valid = TRUE THEN 1 ELSE 0 END) AS valid_count, " +
                    "SUM(CASE WHEN nr.json_valid = FALSE THEN 1 ELSE 0 END) AS invalid_count, " +
                    "ROUND( " +
                    "SUM(CASE WHEN nr.json_valid = TRUE THEN 1 ELSE 0 END) * 100.0 / COUNT(*), 2 " +
                    ") AS validity_rate_pct " +
                    "FROM experiment e " +
                    "JOIN llm_model m ON e.model_id = m.model_id " +
                    "JOIN prompt_technique pt ON e.technique_id = pt.technique_id " +
                    "JOIN nutrition_result nr ON e.experiment_id = nr.experiment_id " +
                    "WHERE e.status = 'completed' " +
                    "GROUP BY m.model_name, pt.technique_name, e.rag_enabled " +
                    "ORDER BY m.model_name, pt.technique_name";

        } else if ("hallucination".equals(exportType)) {
            return exportHallucinationCsv();

        } else if ("ingredient_detection".equals(exportType)) {
            return exportIngredientDetectionCsv();

        } else if ("human_evaluation".equals(exportType)) {
            fileName = "layer4_human_evaluation.csv";
            sql =
                    "SELECT " +
                    "nr.result_id, e.experiment_id, e.transcript_id, m.model_name, pt.technique_name, " +
                    "nr.recipe_name, nr.json_valid, e.status " +
                    "FROM nutrition_result nr " +
                    "JOIN experiment e ON nr.experiment_id = e.experiment_id " +
                    "JOIN llm_model m ON e.model_id = m.model_id " +
                    "JOIN prompt_technique pt ON e.technique_id = pt.technique_id " +
                    "ORDER BY nr.result_id";

        } else if ("condition_scores".equals(exportType)) {
            fileName = "layer5_condition_scores.csv";
            sql =
                    "SELECT " +
                    "e.transcript_id, m.model_name, pt.technique_name, e.rag_enabled, " +
                    "nr.json_valid, nr.total_calories, nr.total_protein_g, nr.total_fat_g, " +
                    "nr.total_carbohydrate_g, COUNT(ir.ingredient_id) AS ingredient_count " +
                    "FROM experiment e " +
                    "JOIN llm_model m ON e.model_id = m.model_id " +
                    "JOIN prompt_technique pt ON e.technique_id = pt.technique_id " +
                    "JOIN nutrition_result nr ON e.experiment_id = nr.experiment_id " +
                    "LEFT JOIN ingredient_result ir ON nr.result_id = ir.result_id " +
                    "WHERE e.status = 'completed' " +
                    "GROUP BY e.transcript_id, m.model_name, pt.technique_name, e.rag_enabled, " +
                    "nr.json_valid, nr.total_calories, nr.total_protein_g, nr.total_fat_g, nr.total_carbohydrate_g " +
                    "ORDER BY e.transcript_id, m.model_name, pt.technique_name";

        } else {
            return "ERROR: Unknown export type: " + exportType;
        }

        return exportQueryToCsv(sql, fileName);
    }
    
    private String exportQueryToCsv(String sql, String fileName) {
        File exportFolder = new File("exports");

        if (!exportFolder.exists()) {
            exportFolder.mkdirs();
        }

        File csvFile = new File(exportFolder, fileName);

        try (
                Connection conn = DBConnection.getConnection();
                PreparedStatement stmt = conn.prepareStatement(sql);
                ResultSet rs = stmt.executeQuery();
                BufferedWriter writer = new BufferedWriter(new FileWriter(csvFile))
        ) {
            ResultSetMetaData metaData = rs.getMetaData();
            int columnCount = metaData.getColumnCount();

            for (int i = 1; i <= columnCount; i++) {
                writer.write(escapeCsv(metaData.getColumnLabel(i)));

                if (i < columnCount) {
                    writer.write(",");
                }
            }

            writer.newLine();

            int rowCount = 0;

            while (rs.next()) {
                for (int i = 1; i <= columnCount; i++) {
                    Object value = rs.getObject(i);
                    writer.write(escapeCsv(value == null ? "" : String.valueOf(value)));

                    if (i < columnCount) {
                        writer.write(",");
                    }
                }

                writer.newLine();
                rowCount++;
            }

            return "CSV exported successfully.\n"
                    + "File: " + csvFile.getAbsolutePath() + "\n"
                    + "Rows: " + rowCount;

        } catch (SQLException e) {
            return "ERROR exporting CSV: " + e.getMessage();

        } catch (IOException e) {
            return "ERROR writing CSV file: " + e.getMessage();
        }
    }

    private String exportHallucinationCsv() {
        String fileName = "layer3b_hallucination.csv";
        File exportFolder = new File("exports");
        if (!exportFolder.exists()) {
            exportFolder.mkdirs();
        }
        File csvFile = new File(exportFolder, fileName);
        
        String sql =
                "SELECT " +
                "e.experiment_id, e.transcript_id, m.model_name, pt.technique_name, e.rag_enabled, nr.result_id " +
                "FROM experiment e " +
                "JOIN llm_model m ON e.model_id = m.model_id " +
                "JOIN prompt_technique pt ON e.technique_id = pt.technique_id " +
                "JOIN nutrition_result nr ON e.experiment_id = nr.experiment_id " +
                "WHERE e.status = 'completed' " +
                "ORDER BY e.experiment_id";

        try (
                Connection conn = DBConnection.getConnection();
                PreparedStatement stmt = conn.prepareStatement(sql);
                ResultSet rs = stmt.executeQuery();
                BufferedWriter writer = new BufferedWriter(new FileWriter(csvFile))
        ) {
            writer.write("experiment_id,transcript_id,model_name,technique_name,rag_enabled,name_original,name_en,is_hallucinated");
            writer.newLine();

            int rowCount = 0;
            while (rs.next()) {
                int experimentId = rs.getInt("experiment_id");
                int transcriptId = rs.getInt("transcript_id");
                String modelName = rs.getString("model_name");
                String techniqueName = rs.getString("technique_name");
                boolean ragEnabled = rs.getBoolean("rag_enabled");
                int resultId = rs.getInt("result_id");

                List<String[]> gtRows = getGroundTruthIngredientRows(conn, transcriptId);
                List<String[]> llmRows = getPredictedIngredientRows(conn, resultId);

                for (String[] llm : llmRows) {
                    String nameOriginal = llm[0];
                    String nameEn = llm[1];
                    boolean found = false;

                    for (String[] gt : gtRows) {
                        if (isIngredientMatch(nameEn, gt[1])) {
                            found = true;
                            break;
                        }
                    }

                    String isHallucinated = found ? "0" : "1";

                    writer.write(escapeCsv(String.valueOf(experimentId)) + "," +
                            escapeCsv(String.valueOf(transcriptId)) + "," +
                            escapeCsv(modelName) + "," +
                            escapeCsv(techniqueName) + "," +
                            (ragEnabled ? "1" : "0") + "," +
                            escapeCsv(nameOriginal) + "," +
                            escapeCsv(nameEn) + "," +
                            isHallucinated);
                    writer.newLine();
                    rowCount++;
                }
            }
            return "CSV exported successfully.\nFile: " + csvFile.getAbsolutePath() + "\nRows: " + rowCount;
        } catch (Exception e) {
            return "ERROR exporting CSV: " + e.getMessage();
        }
    }

    private String exportIngredientDetectionCsv() {
        String fileName = "layer3c_ingredient_detection.csv";
        File exportFolder = new File("exports");
        if (!exportFolder.exists()) {
            exportFolder.mkdirs();
        }
        File csvFile = new File(exportFolder, fileName);
        
        String sql =
                "SELECT " +
                "e.experiment_id, e.transcript_id, m.model_name, pt.technique_name, e.rag_enabled, nr.result_id " +
                "FROM experiment e " +
                "JOIN llm_model m ON e.model_id = m.model_id " +
                "JOIN prompt_technique pt ON e.technique_id = pt.technique_id " +
                "JOIN nutrition_result nr ON e.experiment_id = nr.experiment_id " +
                "WHERE e.status = 'completed' " +
                "ORDER BY e.experiment_id";

        try (
                Connection conn = DBConnection.getConnection();
                PreparedStatement stmt = conn.prepareStatement(sql);
                ResultSet rs = stmt.executeQuery();
                BufferedWriter writer = new BufferedWriter(new FileWriter(csvFile))
        ) {
            writer.write("experiment_id,transcript_id,model_name,technique_name,rag_enabled,predicted_ingredient_count,non_hallucinated_count,hallucinated_count");
            writer.newLine();

            int rowCount = 0;
            while (rs.next()) {
                int experimentId = rs.getInt("experiment_id");
                int transcriptId = rs.getInt("transcript_id");
                String modelName = rs.getString("model_name");
                String techniqueName = rs.getString("technique_name");
                boolean ragEnabled = rs.getBoolean("rag_enabled");
                int resultId = rs.getInt("result_id");

                int hallucinatedCount = countHallucinatedIngredients(conn, transcriptId, resultId);
                int predictedCount = countPredictedIngredients(conn, resultId);
                int nonHallucinatedCount = predictedCount - hallucinatedCount;

                writer.write(escapeCsv(String.valueOf(experimentId)) + "," +
                        escapeCsv(String.valueOf(transcriptId)) + "," +
                        escapeCsv(modelName) + "," +
                        escapeCsv(techniqueName) + "," +
                        (ragEnabled ? "1" : "0") + "," +
                        predictedCount + "," +
                        nonHallucinatedCount + "," +
                        hallucinatedCount);
                writer.newLine();
                rowCount++;
            }
            return "CSV exported successfully.\nFile: " + csvFile.getAbsolutePath() + "\nRows: " + rowCount;
        } catch (Exception e) {
            return "ERROR exporting CSV: " + e.getMessage();
        }
    }

    private String escapeCsv(String value) {
        if (value == null) {
            return "";
        }

        String escaped = value.replace("\"", "\"\"");

        if (escaped.contains(",") || escaped.contains("\"") || escaped.contains("\n")) {
            return "\"" + escaped + "\"";
        }

        return escaped;
    }
    
    public List<String[]> getReelRows() {
    List<String[]> rows = new ArrayList<String[]>();

    String sql =
            "SELECT " +
            "r.reel_id, " +
            "r.reel_id_instagram, " +
            "r.reel_url, " +
            "i.instagram_account, " +
            "t.transcript_id, " +
            "gtr.gt_reel_id, " +
            "COUNT(e.experiment_id) AS total_experiments, " +
            "SUM(CASE WHEN e.status = 'completed' THEN 1 ELSE 0 END) AS completed_count, " +
            "SUM(CASE WHEN e.status = 'running' THEN 1 ELSE 0 END) AS running_count, " +
            "SUM(CASE WHEN e.status = 'failed' THEN 1 ELSE 0 END) AS failed_count, " +
            "SUM(CASE WHEN e.status = 'pending' THEN 1 ELSE 0 END) AS pending_count " +
            "FROM reel r " +
            "LEFT JOIN influencer i ON r.influencer_id = i.influencer_id " +
            "LEFT JOIN transcript t ON r.reel_id = t.reel_id " +
            "LEFT JOIN ground_truth_reel gtr ON t.transcript_id = gtr.transcript_id " +
            "LEFT JOIN experiment e ON t.transcript_id = e.transcript_id " +
            "GROUP BY " +
            "r.reel_id, " +
            "r.reel_id_instagram, " +
            "r.reel_url, " +
            "i.instagram_account, " +
            "t.transcript_id, " +
            "gtr.gt_reel_id " +
            "ORDER BY r.reel_id DESC";

    try (
            Connection conn = DBConnection.getConnection();
            PreparedStatement stmt = conn.prepareStatement(sql);
            ResultSet rs = stmt.executeQuery()
    ) {
        while (rs.next()) {
            String reelId = String.valueOf(rs.getInt("reel_id"));
            String instagramId = nullToDash(rs.getString("reel_id_instagram"));
            String handle = "@" + nullToDash(rs.getString("instagram_account"));

            String transcript = rs.getInt("transcript_id") > 0 ? "Available" : "Missing";
            String groundTruth = rs.getInt("gt_reel_id") > 0 ? "Available" : "Missing";

            int totalExperiments = rs.getInt("total_experiments");
            int completed = rs.getInt("completed_count");
            int running = rs.getInt("running_count");
            int failed = rs.getInt("failed_count");
            int pending = rs.getInt("pending_count");

            String analysisStatus;

            if (totalExperiments == 0) {
                analysisStatus = "Not Started";
            } else if (running > 0) {
                analysisStatus = "Running";
            } else if (pending > 0) {
                analysisStatus = "Pending";
            } else if (failed > 0) {
                analysisStatus = "Completed with Failed";
            } else if (completed == totalExperiments) {
                analysisStatus = "Completed";
            } else {
                analysisStatus = "In Progress";
            }

            String url = nullToDash(rs.getString("reel_url"));

            rows.add(new String[] {
                    reelId,
                    instagramId,
                    handle,
                    transcript,
                    groundTruth,
                    String.valueOf(totalExperiments),
                    String.valueOf(completed),
                    String.valueOf(running),
                    String.valueOf(failed),
                    analysisStatus,
                    url
            });
        }

    } catch (SQLException e) {
        rows.add(new String[] {
                "ERROR",
                e.getMessage(),
                "-",
                "-",
                "-",
                "-",
                "-",
                "-",
                "-",
                "-",
                "-"
        });
    }

    return rows;
}
    
    public List<String[]> getExperimentRows() {
        List<String[]> rows = new ArrayList<String[]>();

        String sql =
                "SELECT " +
                "e.experiment_id, " +
                "e.transcript_id, " +
                "m.model_name, " +
                "pt.technique_name, " +
                "e.rag_enabled, " +
                "e.status, " +
                "e.executed_at " +
                "FROM experiment e " +
                "LEFT JOIN llm_model m ON e.model_id = m.model_id " +
                "LEFT JOIN prompt_technique pt ON e.technique_id = pt.technique_id " +
                "ORDER BY e.experiment_id DESC";

        try (
                Connection conn = DBConnection.getConnection();
                PreparedStatement stmt = conn.prepareStatement(sql);
                ResultSet rs = stmt.executeQuery()
        ) {
            while (rs.next()) {
                rows.add(new String[] {
                        String.valueOf(rs.getInt("experiment_id")),
                        String.valueOf(rs.getInt("transcript_id")),
                        nullToDash(rs.getString("model_name")),
                        nullToDash(rs.getString("technique_name")),
                        rs.getBoolean("rag_enabled") ? "Enabled" : "Disabled",
                        nullToDash(rs.getString("status")),
                        nullToDash(rs.getString("executed_at"))
                });
            }

        } catch (SQLException e) {
            rows.add(new String[] {
                    "ERROR",
                    "-",
                    e.getMessage(),
                    "-",
                    "-",
                    "-",
                    "-"
            });
        }

        return rows;
    }
    
    public List<String[]> getExperimentRowsFiltered(
            String transcriptId,
            String model,
            String technique) {

        List<String[]> rows = new ArrayList<>();

        String sql =
                "SELECT " +
                "e.experiment_id, " +
                "e.transcript_id, " +
                "m.model_name, " +
                "pt.technique_name, " +
                "e.rag_enabled, " +
                "e.status, " +
                "e.executed_at " +
                "FROM experiment e " +
                "LEFT JOIN llm_model m ON e.model_id = m.model_id " +
                "LEFT JOIN prompt_technique pt ON e.technique_id = pt.technique_id " +
                "WHERE (? = 'All' OR e.transcript_id = ?) " +
                "AND (? = 'All' OR m.model_name = ?) " +
                "AND (? = 'All' OR pt.technique_name = ?) " +
                "ORDER BY e.experiment_id DESC";

        try (
                Connection conn = DBConnection.getConnection();
                PreparedStatement stmt = conn.prepareStatement(sql)
        ) {

            stmt.setString(1, transcriptId);
            stmt.setString(2, transcriptId);

            stmt.setString(3, model);
            stmt.setString(4, model);

            stmt.setString(5, technique);
            stmt.setString(6, technique);

            ResultSet rs = stmt.executeQuery();

            while (rs.next()) {

                rows.add(new String[]{
                        String.valueOf(rs.getInt("experiment_id")),
                        String.valueOf(rs.getInt("transcript_id")),
                        nullToDash(rs.getString("model_name")),
                        nullToDash(rs.getString("technique_name")),
                        rs.getBoolean("rag_enabled") ? "Enabled" : "Disabled",
                        nullToDash(rs.getString("status")),
                        nullToDash(rs.getString("executed_at"))
                });
            }

        } catch (SQLException e) {

            rows.add(new String[]{
                    "ERROR",
                    "-",
                    e.getMessage(),
                    "-",
                    "-",
                    "-",
                    "-"
            });
        }

        return rows;
    }
    
    public List<String> getTranscriptIds() {

        List<String> list = new ArrayList<>();

        list.add("All");

        String sql =
                "SELECT DISTINCT transcript_id " +
                "FROM experiment " +
                "ORDER BY transcript_id";

        try (
                Connection conn = DBConnection.getConnection();
                PreparedStatement stmt = conn.prepareStatement(sql);
                ResultSet rs = stmt.executeQuery()
        ) {

            while (rs.next()) {
                list.add(String.valueOf(rs.getInt("transcript_id")));
            }

        } catch (SQLException e) {
            e.printStackTrace();
        }

        return list;
    }
    
    public List<String> getModels() {

        List<String> list = new ArrayList<>();

        list.add("All");

        String sql =
                "SELECT model_name " +
                "FROM llm_model " +
                "ORDER BY model_name";

        try (
                Connection conn = DBConnection.getConnection();
                PreparedStatement stmt = conn.prepareStatement(sql);
                ResultSet rs = stmt.executeQuery()
        ) {

            while (rs.next()) {
                list.add(rs.getString("model_name"));
            }

        } catch (SQLException e) {
            e.printStackTrace();
        }

        return list;
    }
    
    public List<String> getTechniques() {

        List<String> list = new ArrayList<>();

        list.add("All");

        String sql =
                "SELECT technique_name " +
                "FROM prompt_technique " +
                "ORDER BY technique_name";

        try (
                Connection conn = DBConnection.getConnection();
                PreparedStatement stmt = conn.prepareStatement(sql);
                ResultSet rs = stmt.executeQuery()
        ) {

            while (rs.next()) {
                list.add(rs.getString("technique_name"));
            }

        } catch (SQLException e) {
            e.printStackTrace();
        }

        return list;
    }
    
    public java.util.List<String[]> getMetricsRows() {
    java.util.List<String[]> rows = new java.util.ArrayList<String[]>();

    String sql =
            "SELECT " +
            "m.model_name, " +
            "pt.technique_name, " +
            "COUNT(DISTINCT e.experiment_id) AS total_runs, " +
            "COUNT(DISTINCT CASE WHEN e.status = 'completed' THEN e.experiment_id END) AS completed_runs, " +
            "COUNT(DISTINCT CASE WHEN e.status = 'failed' THEN e.experiment_id END) AS failed_runs, " +
            "COUNT(DISTINCT CASE WHEN e.status = 'running' THEN e.experiment_id END) AS running_runs, " +
            "COUNT(DISTINCT nr.result_id) AS total_results, " +
            "COUNT(DISTINCT CASE WHEN nr.json_valid = TRUE THEN nr.result_id END) AS valid_json, " +
            "COUNT(ir.ingredient_id) AS ingredient_count, MAX(nr.result_id) AS result_id, MAX(e.transcript_id) AS transcript_id " +
            "FROM experiment e " +
            "JOIN llm_model m ON e.model_id = m.model_id " +
            "JOIN prompt_technique pt ON e.technique_id = pt.technique_id " +
            "LEFT JOIN nutrition_result nr ON e.experiment_id = nr.experiment_id " +
            "LEFT JOIN ingredient_result ir ON nr.result_id = ir.result_id " +
            "GROUP BY m.model_name, pt.technique_name " +
            "ORDER BY m.model_name, pt.technique_name";

    try (
            Connection conn = DBConnection.getConnection();
            PreparedStatement stmt = conn.prepareStatement(sql);
            ResultSet rs = stmt.executeQuery()
    ) {
        while (rs.next()) {
            int totalResults = rs.getInt("total_results");
            int validJson = rs.getInt("valid_json");

            int ingredientCount = rs.getInt("ingredient_count");

            int resultId = rs.getInt("result_id");
            int transcriptId = rs.getInt("transcript_id");
//*COUNT HALLUCINATE
            int hallucinated = 0;

            if (resultId > 0) {
                hallucinated = countHallucinatedIngredients(
                        conn,
                        transcriptId,
                        resultId
                );
            }

            double hallucinationRate = 0;

            if (ingredientCount > 0) {
                hallucinationRate = hallucinated * 100.0 / ingredientCount;
            }
            
            double jsonValidityRate = 0.0;
            if (totalResults > 0) {
                jsonValidityRate = (validJson * 100.0) / totalResults;
            }

            rows.add(new String[] {
            	    nullToDash(rs.getString("model_name")),
            	    nullToDash(rs.getString("technique_name")),
            	    String.valueOf(rs.getInt("total_runs")),
            	    String.valueOf(rs.getInt("completed_runs")),
            	    String.valueOf(rs.getInt("failed_runs")),
            	    String.valueOf(rs.getInt("running_runs")),
            	    String.format("%.2f%%", jsonValidityRate),
            	    String.valueOf(ingredientCount),
            	    String.valueOf(hallucinated),
            	    String.format("%.2f%%", hallucinationRate)
            	});
        }

    } catch (SQLException e) {
        rows.add(new String[] {
                "ERROR",
                e.getMessage(),
                "-",
                "-",
                "-",
                "-",
                "-",
                "-",
                "-",
                "-"
        });
    }

    return rows;
}
    
    public String getMetricsOverview() {
    	String sql =
    	        "SELECT " +
    	        "COUNT(DISTINCT e.experiment_id) AS total_runs, " +
    	        "COUNT(DISTINCT CASE WHEN e.status = 'completed' THEN e.experiment_id END) AS completed_runs, " +
    	        "COUNT(DISTINCT CASE WHEN e.status = 'failed' THEN e.experiment_id END) AS failed_runs, " +
    	        "COUNT(DISTINCT nr.result_id) AS total_results, " +
    	        "COUNT(DISTINCT CASE WHEN nr.json_valid = TRUE THEN nr.result_id END) AS valid_json " +
    	        "FROM experiment e " +
    	        "LEFT JOIN nutrition_result nr ON e.experiment_id = nr.experiment_id";

    try (
            Connection conn = DBConnection.getConnection();
            PreparedStatement stmt = conn.prepareStatement(sql);
            ResultSet rs = stmt.executeQuery()
    ) {
        if (rs.next()) {
            int totalRuns = rs.getInt("total_runs");
            int completedRuns = rs.getInt("completed_runs");
            int failedRuns = rs.getInt("failed_runs");

            int totalResults = rs.getInt("total_results");
            int validJson = rs.getInt("valid_json");

            double completionRate = totalRuns > 0 ? (completedRuns * 100.0) / totalRuns : 0.0;
            double jsonRate = totalResults > 0 ? (validJson * 100.0) / totalResults : 0.0;

            return ""
            + "METRICS OVERVIEW\n"
            + "==================================================\n\n"
            + "Total Experiment Runs: " + totalRuns + "\n"
            + "Completed Runs: " + completedRuns + "\n"
            + "Failed Runs: " + failedRuns + "\n"
            + "Completion Rate: " + String.format("%.2f%%", completionRate) + "\n\n"
            + "Total Nutrition Results: " + totalResults + "\n"
            + "Valid JSON Outputs: " + validJson + "\n"
            + "JSON Validity Rate: " + String.format("%.2f%%", jsonRate) + "\n";
        }

    } catch (SQLException e) {
        return "ERROR loading metrics overview: " + e.getMessage();
    }

    return "No metrics data available yet.";
}
    public String getLatestNutritionComparisonOverview() {
        String sql =
                "SELECT " +
                "nr.result_id, " +
                "nr.experiment_id, " +
                "nr.recipe_name, " +
                "nr.servings_estimated, " +
                "e.transcript_id, " +
                "m.model_name, " +
                "pt.technique_name " +
                "FROM nutrition_result nr " +
                "JOIN experiment e ON nr.experiment_id = e.experiment_id " +
                "JOIN llm_model m ON e.model_id = m.model_id " +
                "JOIN prompt_technique pt ON e.technique_id = pt.technique_id " +
                "WHERE e.status = 'completed' " +
                "ORDER BY nr.result_id DESC " +
                "LIMIT 1";

        try (
                Connection conn = DBConnection.getConnection();
                PreparedStatement stmt = conn.prepareStatement(sql);
                ResultSet rs = stmt.executeQuery()
        ) {
            if (!rs.next()) {
                return "No completed nutrition result found yet.";
            }

            int resultId = rs.getInt("result_id");
            int transcriptId = rs.getInt("transcript_id");

            int groundTruthCount = countGroundTruthIngredients(conn, transcriptId);
            int predictedCount = countPredictedIngredients(conn, resultId);

            return ""
                    + "NUTRITION FACT SHEET COMPARISON\n"
                    + "==================================================\n\n"
                    + "Result ID: " + resultId + "\n"
                    + "Experiment ID: " + rs.getInt("experiment_id") + "\n"
                    + "Transcript ID: " + transcriptId + "\n"
                    + "Model: " + nullToDash(rs.getString("model_name")) + "\n"
                    + "Prompt Technique: " + nullToDash(rs.getString("technique_name")) + "\n"
                    + "Recipe Name: " + nullToDash(rs.getString("recipe_name")) + "\n"
                    + "Servings Estimated: " + rs.getInt("servings_estimated") + "\n\n"
                    + "Ground Truth Ingredients: " + groundTruthCount + "\n"
                    + "LLM Predicted Ingredients: " + predictedCount + "\n\n"
                    + "Note: This page displays ground truth and LLM output side-by-side by row order.\n"
                    + "Exact matching and scoring are handled in the Metrics / CSV evaluation layer.\n";

        } catch (SQLException e) {
            return "ERROR loading nutrition comparison overview: " + e.getMessage();
        }
    }
    
    public List<String[]> getLatestNutritionComparisonRows() {
        List<String[]> finalRows = new ArrayList<String[]>();

        String latestSql =
                "SELECT " +
                "nr.result_id, " +
                "e.transcript_id " +
                "FROM nutrition_result nr " +
                "JOIN experiment e ON nr.experiment_id = e.experiment_id " +
                "WHERE e.status = 'completed' " +
                "ORDER BY nr.result_id DESC " +
                "LIMIT 1";

        try (
                Connection conn = DBConnection.getConnection();
                PreparedStatement latestStmt = conn.prepareStatement(latestSql);
                ResultSet latestRs = latestStmt.executeQuery()
        ) {
            if (!latestRs.next()) {
                finalRows.add(new String[] {
                        "-",
                        "No completed nutrition result found.",
                        "-",
                        "-",
                        "-",
                        "-",
                        "-",
                        "-",
                        "-",
                        "-",
                        "-",
                        "-",
                        "-"
                });

                return finalRows;
            }

            int resultId = latestRs.getInt("result_id");
            int transcriptId = latestRs.getInt("transcript_id");

            List<String[]> groundTruthRows = getGroundTruthIngredientRows(conn, transcriptId);
            List<String[]> predictedRows = getPredictedIngredientRows(conn, resultId);

            int maxRows = Math.max(groundTruthRows.size(), predictedRows.size());

            if (maxRows == 0) {
                finalRows.add(new String[] {
                        "-",
                        "No ingredients found.",
                        "-",
                        "-",
                        "-",
                        "-",
                        "-",
                        "-",
                        "-",
                        "-",
                        "-",
                        "-",
                        "-"
                });

                return finalRows;
            }

            for (int i = 0; i < maxRows; i++) {
                String[] gt = i < groundTruthRows.size()
                        ? groundTruthRows.get(i)
                        : new String[] { "-", "-", "-", "-", "-", "-" };

                String[] pred = i < predictedRows.size()
                        ? predictedRows.get(i)
                        : new String[] { "-", "-", "-", "-", "-", "-" };

                finalRows.add(new String[] {
                        String.valueOf(i + 1),

                        gt[0],
                        gt[1],
                        gt[2],
                        gt[3],
                        gt[4],
                        gt[5],

                        pred[0],
                        pred[1],
                        pred[2],
                        pred[3],
                        pred[4],
                        pred[5]
                });
            }

        } catch (SQLException e) {
            finalRows.add(new String[] {
                    "ERROR",
                    e.getMessage(),
                    "-",
                    "-",
                    "-",
                    "-",
                    "-",
                    "-",
                    "-",
                    "-",
                    "-",
                    "-",
                    "-"
            });
        }

        return finalRows;
    }
    
    private List<String[]> getGroundTruthIngredientRows(Connection conn, int transcriptId) throws SQLException {
        List<String[]> rows = new ArrayList<String[]>();

        String sql =
                "SELECT " +
                "gti.name_original, " +
                "gti.name_en, " +
                "gti.quantity_expression, " +
                "gti.quantity_value_culinary, " +
                "gti.quantity_unit_culinary, " +
                "gti.estimated_weight_g, " +
                "gti.calories " +
                "FROM ground_truth_reel gtr " +
                "JOIN ground_truth_ingredient gti ON gtr.gt_reel_id = gti.gt_reel_id " +
                "WHERE gtr.transcript_id = ? " +
                "ORDER BY gti.gt_ingredient_id";

        try (
                PreparedStatement stmt = conn.prepareStatement(sql)
        ) {
            stmt.setInt(1, transcriptId);

            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    String quantityValue = valueToDash(rs.getObject("quantity_value_culinary"));
                    String quantityUnit = valueToDash(rs.getObject("quantity_unit_culinary"));

                    rows.add(new String[] {
                            nullToDash(rs.getString("name_original")),
                            nullToDash(rs.getString("name_en")),
                            nullToDash(rs.getString("quantity_expression")),
                            quantityValue + " " + quantityUnit,
                            valueToDash(rs.getObject("estimated_weight_g")),
                            valueToDash(rs.getObject("calories"))
                    });
                }
            }
        }

        return rows;
    }
    
    private List<String[]> getPredictedIngredientRows(Connection conn, int resultId) throws SQLException {
        List<String[]> rows = new ArrayList<String[]>();

        String sql =
                "SELECT " +
                "name_original, " +
                "name_en, " +
                "quantity_value, " +
                "unit_original, " +
                "unit_en, " +
                "estimated_weight_g, " +
                "calories " +
                "FROM ingredient_result " +
                "WHERE result_id = ? " +
                "ORDER BY ingredient_id";

        try (
                PreparedStatement stmt = conn.prepareStatement(sql)
        ) {
            stmt.setInt(1, resultId);

            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    String quantityValue = valueToDash(rs.getObject("quantity_value"));
                    String unitOriginal = valueToDash(rs.getObject("unit_original"));
                    String unitEnglish = valueToDash(rs.getObject("unit_en"));

                    rows.add(new String[] {
                            nullToDash(rs.getString("name_original")),
                            nullToDash(rs.getString("name_en")),
                            quantityValue,
                            unitOriginal + " / " + unitEnglish,
                            valueToDash(rs.getObject("estimated_weight_g")),
                            valueToDash(rs.getObject("calories"))
                    });
                }
            }
        }

        return rows;
    }
    
    private int countGroundTruthIngredients(Connection conn, int transcriptId) throws SQLException {
        String sql =
                "SELECT COUNT(*) " +
                "FROM ground_truth_reel gtr " +
                "JOIN ground_truth_ingredient gti ON gtr.gt_reel_id = gti.gt_reel_id " +
                "WHERE gtr.transcript_id = ?";

        try (
                PreparedStatement stmt = conn.prepareStatement(sql)
        ) {
            stmt.setInt(1, transcriptId);

            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return rs.getInt(1);
                }
            }
        }

        return 0;
    }

    private int countPredictedIngredients(Connection conn, int resultId) throws SQLException {
        String sql =
                "SELECT COUNT(*) " +
                "FROM ingredient_result " +
                "WHERE result_id = ?";

        try (
                PreparedStatement stmt = conn.prepareStatement(sql)
        ) {
            stmt.setInt(1, resultId);

            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return rs.getInt(1);
                }
            }
        }

        return 0;
    }
    
    private int countHallucinatedIngredients(Connection conn,
            int transcriptId,
            int resultId) throws SQLException {

    	List<String[]> gtRows = getGroundTruthIngredientRows(conn, transcriptId);
    	List<String[]> llmRows = getPredictedIngredientRows(conn, resultId);

    	int hallucinated = 0;

    	for (String[] llm : llmRows) {

    		boolean found = false;

    		String llmName = llm[1]; // English name

    		for (String[] gt : gtRows) {

    			String gtName = gt[1];

    			if (isIngredientMatch(llmName, gtName)) {
    				found = true;
    				break;
    			}
    		}

    		if (!found) {
    			hallucinated++;
    		}
    	}

    	return hallucinated;
    }

    private String valueToDash(Object value) {
        if (value == null) {
            return "-";
        }

        String text = String.valueOf(value);

        if (text.trim().isEmpty()) {
            return "-";
        }

        return text;
    }
    
    public String getNutritionComparisonOverviewByExperiment(int experimentId) {
        String sql =
                "SELECT " +
                "nr.result_id, " +
                "nr.experiment_id, " +
                "nr.recipe_name, " +
                "nr.servings_estimated, " +
                "e.transcript_id, " +
                "m.model_name, " +
                "pt.technique_name, " +
                "e.status " +
                "FROM experiment e " +
                "JOIN nutrition_result nr ON e.experiment_id = nr.experiment_id " +
                "JOIN llm_model m ON e.model_id = m.model_id " +
                "JOIN prompt_technique pt ON e.technique_id = pt.technique_id " +
                "WHERE e.experiment_id = ? " +
                "LIMIT 1";

        try (
                Connection conn = DBConnection.getConnection();
                PreparedStatement stmt = conn.prepareStatement(sql)
        ) {
            stmt.setInt(1, experimentId);

            try (ResultSet rs = stmt.executeQuery()) {
                if (!rs.next()) {
                    return "No nutrition result found for Experiment ID: " + experimentId;
                }

                int resultId = rs.getInt("result_id");
                int transcriptId = rs.getInt("transcript_id");

                int groundTruthCount = countGroundTruthIngredients(conn, transcriptId);
                int predictedCount = countPredictedIngredients(conn, resultId);

                return ""
                        + "NUTRITION FACT SHEET COMPARISON\n"
                        + "==================================================\n\n"
                        + "Result ID: " + resultId + "\n"
                        + "Experiment ID: " + rs.getInt("experiment_id") + "\n"
                        + "Transcript ID: " + transcriptId + "\n"
                        + "Status: " + nullToDash(rs.getString("status")) + "\n"
                        + "Model: " + nullToDash(rs.getString("model_name")) + "\n"
                        + "Prompt Technique: " + nullToDash(rs.getString("technique_name")) + "\n"
                        + "Recipe Name: " + nullToDash(rs.getString("recipe_name")) + "\n"
                        + "Servings Estimated: " + rs.getInt("servings_estimated") + "\n\n"
                        + "Ground Truth Ingredients: " + groundTruthCount + "\n"
                        + "LLM Predicted Ingredients: " + predictedCount + "\n\n"
                        + "This fact sheet belongs to Experiment ID: " + experimentId + "\n";
            }

        } catch (SQLException e) {
            return "ERROR loading nutrition comparison overview: " + e.getMessage();
        }
    }
    
    public java.util.List<String[]> getNutritionComparisonRowsByExperiment(int experimentId) {
        java.util.List<String[]> finalRows = new java.util.ArrayList<String[]>();

        String sql =
                "SELECT " +
                "nr.result_id, " +
                "e.transcript_id " +
                "FROM experiment e " +
                "JOIN nutrition_result nr ON e.experiment_id = nr.experiment_id " +
                "WHERE e.experiment_id = ? " +
                "LIMIT 1";

        try (
                Connection conn = DBConnection.getConnection();
                PreparedStatement stmt = conn.prepareStatement(sql)
        ) {
            stmt.setInt(1, experimentId);

            try (ResultSet rs = stmt.executeQuery()) {
                if (!rs.next()) {
                    finalRows.add(new String[] {
                            "-",
                            "No nutrition result found for Experiment ID: " + experimentId,
                            "-",
                            "-",
                            "-",
                            "-",
                            "-",
                            "-",
                            "-",
                            "-",
                            "-",
                            "-",
                            "-"
                    });

                    return finalRows;
                }

                int resultId = rs.getInt("result_id");
                int transcriptId = rs.getInt("transcript_id");

                java.util.List<String[]> groundTruthRows = getGroundTruthIngredientRows(conn, transcriptId);
                java.util.List<String[]> predictedRows = getPredictedIngredientRows(conn, resultId);

                int maxRows = Math.max(groundTruthRows.size(), predictedRows.size());

                if (maxRows == 0) {
                    finalRows.add(new String[] {
                            "-",
                            "No ingredients found.",
                            "-",
                            "-",
                            "-",
                            "-",
                            "-",
                            "-",
                            "-",
                            "-",
                            "-",
                            "-",
                            "-"
                    });

                    return finalRows;
                }

                for (int i = 0; i < maxRows; i++) {
                    String[] gt = i < groundTruthRows.size()
                            ? groundTruthRows.get(i)
                            : new String[] { "-", "-", "-", "-", "-", "-" };

                    String[] pred = i < predictedRows.size()
                            ? predictedRows.get(i)
                            : new String[] { "-", "-", "-", "-", "-", "-" };

                    finalRows.add(new String[] {
                            String.valueOf(i + 1),

                            gt[0],
                            gt[1],
                            gt[2],
                            gt[3],
                            gt[4],
                            gt[5],

                            pred[0],
                            pred[1],
                            pred[2],
                            pred[3],
                            pred[4],
                            pred[5]
                    });
                }
            }

        } catch (SQLException e) {
            finalRows.add(new String[] {
                    "ERROR",
                    e.getMessage(),
                    "-",
                    "-",
                    "-",
                    "-",
                    "-",
                    "-",
                    "-",
                    "-",
                    "-",
                    "-",
                    "-"
            });
        }

        return finalRows;
    }
    public String getTranscriptPreviewByReelId(int reelId) {
        StringBuilder result = new StringBuilder();

        try (Connection conn = DBConnection.getConnection()) {
            boolean hasTranscriptText = hasColumn(conn, "transcript", "transcript_text");
            boolean hasLanguageTag = hasColumn(conn, "transcript", "language_tag");

            String sql =
                    "SELECT " +
                    "transcript_id, " +
                    "file_name, " +
                    "file_path " +
                    (hasTranscriptText ? ", transcript_text " : "") +
                    (hasLanguageTag ? ", language_tag " : "") +
                    "FROM transcript " +
                    "WHERE reel_id = ? " +
                    "LIMIT 1";

            try (PreparedStatement stmt = conn.prepareStatement(sql)) {
                stmt.setInt(1, reelId);

                try (ResultSet rs = stmt.executeQuery()) {
                    if (!rs.next()) {
                        return "No transcript found for Reel ID: " + reelId;
                    }

                    int transcriptId = rs.getInt("transcript_id");
                    String fileName = rs.getString("file_name");
                    String filePath = rs.getString("file_path");

                    String languageTag = hasLanguageTag ? rs.getString("language_tag") : "N/A";
                    String transcriptText = hasTranscriptText ? rs.getString("transcript_text") : null;

                    if (isEmptyText(transcriptText)) {
                        transcriptText = readTranscriptFile(filePath);
                    }

                    result.append("TRANSCRIPT PREVIEW\n");
                    result.append("==================================================\n\n");
                    result.append("Reel ID: ").append(reelId).append("\n");
                    result.append("Transcript ID: ").append(transcriptId).append("\n");
                    result.append("File Name: ").append(nullToDash(fileName)).append("\n");
                    result.append("Language Tag: ").append(nullToDash(languageTag)).append("\n");
                    result.append("File Path: ").append(nullToDash(filePath)).append("\n\n");

                    if (isEmptyText(transcriptText)) {
                        result.append("Transcript text is empty or file cannot be read.\n");
                        return result.toString();
                    }

                    result.append("Preview:\n");
                    result.append("--------------------------------------------------\n");
                    result.append(limitText(transcriptText, 2500));

                    return result.toString();
                }
            }

        } catch (SQLException e) {
            return "ERROR loading transcript preview: " + e.getMessage();
        }
    }
    
    private boolean hasColumn(Connection conn, String tableName, String columnName) throws SQLException {
        String sql =
                "SELECT COUNT(*) " +
                "FROM INFORMATION_SCHEMA.COLUMNS " +
                "WHERE TABLE_SCHEMA = DATABASE() " +
                "AND TABLE_NAME = ? " +
                "AND COLUMN_NAME = ?";

        try (PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, tableName);
            stmt.setString(2, columnName);

            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return rs.getInt(1) > 0;
                }
            }
        }

        return false;
    }

    private String readTranscriptFile(String filePath) {
        if (isEmptyText(filePath)) {
            return "";
        }

        try {
            Path path = Paths.get(filePath);

            if (!Files.exists(path)) {
                return "";
            }

            byte[] bytes = Files.readAllBytes(path);
            return new String(bytes, StandardCharsets.UTF_8);

        } catch (Exception e) {
            return "";
        }
    }

    private String limitText(String text, int maxLength) {
        if (text == null) {
            return "";
        }

        String cleaned = text.trim();

        if (cleaned.length() <= maxLength) {
            return cleaned;
        }

        return cleaned.substring(0, maxLength) + "\n\n... [preview shortened]";
    }

    private boolean isEmptyText(String text) {
        return text == null || text.trim().isEmpty();
    }
    
    public String exportNutritionFactSheetCsv(Integer experimentId) {
        File exportFolder = new File("exports");

        if (!exportFolder.exists()) {
            exportFolder.mkdirs();
        }

        Integer selectedExperimentId = experimentId;

        try (Connection conn = DBConnection.getConnection()) {
            if (selectedExperimentId == null) {
                selectedExperimentId = getLatestCompletedExperimentId(conn);
            }

            if (selectedExperimentId == null) {
                return "No completed nutrition fact sheet available to export.";
            }

            String fileName = "fact_sheet_experiment_" + selectedExperimentId + ".csv";
            File csvFile = new File(exportFolder, fileName);

            java.util.List<String[]> rows = getNutritionComparisonRowsByExperiment(selectedExperimentId);

            try (BufferedWriter writer = new BufferedWriter(new FileWriter(csvFile))) {
                writer.write("row_no,gt_name_original,gt_name_en,gt_quantity_expression,gt_value_unit,gt_weight_g,gt_calories,"
                        + "llm_name_original,llm_name_en,llm_quantity,llm_unit,llm_weight_g,llm_calories");
                writer.newLine();

                int rowCount = 0;

                for (String[] row : rows) {
                    for (int i = 0; i < row.length; i++) {
                        writer.write(escapeCsv(row[i]));

                        if (i < row.length - 1) {
                            writer.write(",");
                        }
                    }

                    writer.newLine();
                    rowCount++;
                }

                return "Fact sheet CSV exported successfully.\n"
                        + "Experiment ID: " + selectedExperimentId + "\n"
                        + "File: " + csvFile.getAbsolutePath() + "\n"
                        + "Rows: " + rowCount;
            }

        } catch (SQLException e) {
            return "ERROR exporting fact sheet CSV: " + e.getMessage();

        } catch (IOException e) {
            return "ERROR writing fact sheet CSV: " + e.getMessage();
        }
    }
    
    private Integer getLatestCompletedExperimentId(Connection conn) throws SQLException {
        String sql =
                "SELECT e.experiment_id " +
                "FROM experiment e " +
                "JOIN nutrition_result nr ON e.experiment_id = nr.experiment_id " +
                "WHERE e.status = 'completed' " +
                "ORDER BY nr.result_id DESC " +
                "LIMIT 1";

        try (
                PreparedStatement stmt = conn.prepareStatement(sql);
                ResultSet rs = stmt.executeQuery()
        ) {
            if (rs.next()) {
                return rs.getInt("experiment_id");
            }
        }

        return null;
    }
    
    public String markRunningExperimentsAsFailed() {
        String sql =
                "UPDATE experiment " +
                "SET status = 'failed' " +
                "WHERE status = 'running'";

        try (
                Connection conn = DBConnection.getConnection();
                PreparedStatement stmt = conn.prepareStatement(sql)
        ) {
            int updatedRows = stmt.executeUpdate();

            return "Stop applied successfully.\n"
                    + "Running experiments changed to failed: " + updatedRows;

        } catch (SQLException e) {
            return "ERROR updating running experiments to failed: " + e.getMessage();
        }
        
    }
    
    public static boolean isIngredientMatch(String llmName, String gtName) {

        if (llmName == null || gtName == null) {
            return false;
        }

        llmName = llmName.toLowerCase().trim();
        gtName = gtName.toLowerCase().trim();

        return llmName.equals(gtName)
                || llmName.contains(gtName)
                || gtName.contains(llmName);
    }
}