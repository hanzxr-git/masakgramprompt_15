package Database;

import model.Experiment;
import model.LLMModel;
import model.PromptTechnique;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.List;

public class ExperimentDAO {

    public int createExperiment(int transcriptId, int modelId, int techniqueId, boolean ragEnabled) throws SQLException {
        String sql = "INSERT INTO experiment (transcript_id, model_id, technique_id, rag_enabled, status, executed_at) "
                + "VALUES (?, ?, ?, ?, ?, NOW())";

        try (Connection connection = DBConnection.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {

            statement.setInt(1, transcriptId);
            statement.setInt(2, modelId);
            statement.setInt(3, techniqueId);
            statement.setBoolean(4, ragEnabled);
            statement.setString(5, "pending");
            statement.executeUpdate();

            try (ResultSet keys = statement.getGeneratedKeys()) {
                if (keys.next()) {
                    return keys.getInt(1);
                }
            }
        }
        throw new SQLException("Failed to create experiment; no generated key returned.");
    }

    /**
     * Reuses an existing experiment for the same transcript/model/technique/RAG combination,
     * otherwise creates a new row. This prevents duplicate experiment rows during testing.
     */
    public int getOrCreateExperiment(int transcriptId, int modelId, int techniqueId, boolean ragEnabled) throws SQLException {
        String findSql = "SELECT experiment_id FROM experiment "
                + "WHERE transcript_id = ? AND model_id = ? AND technique_id = ? AND rag_enabled = ? "
                + "ORDER BY experiment_id DESC LIMIT 1";

        try (Connection connection = DBConnection.getConnection();
             PreparedStatement statement = connection.prepareStatement(findSql)) {

            statement.setInt(1, transcriptId);
            statement.setInt(2, modelId);
            statement.setInt(3, techniqueId);
            statement.setBoolean(4, ragEnabled);

            try (ResultSet rs = statement.executeQuery()) {
                if (rs.next()) {
                    int experimentId = rs.getInt("experiment_id");
                    updateStatus(experimentId, "pending");
                    return experimentId;
                }
            }
        }
        return createExperiment(transcriptId, modelId, techniqueId, ragEnabled);
    }

    public void updateStatus(int experimentId, String status) throws SQLException {
        String sql = "UPDATE experiment SET status = ?, executed_at = NOW() WHERE experiment_id = ?";

        try (Connection connection = DBConnection.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {

            statement.setString(1, status);
            statement.setInt(2, experimentId);
            statement.executeUpdate();
        }
    }

    public Experiment findById(int experimentId) throws SQLException {
        String sql = "SELECT * FROM experiment WHERE experiment_id = ?";

        try (Connection connection = DBConnection.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {

            statement.setInt(1, experimentId);
            try (ResultSet rs = statement.executeQuery()) {
                if (rs.next()) {
                    return mapExperiment(rs);
                }
            }
        }
        return null;
    }

    public List<Experiment> findAll() throws SQLException {
        List<Experiment> experiments = new ArrayList<>();
        String sql = "SELECT * FROM experiment ORDER BY experiment_id";

        try (Connection connection = DBConnection.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql);
             ResultSet rs = statement.executeQuery()) {

            while (rs.next()) {
                experiments.add(mapExperiment(rs));
            }
        }
        return experiments;
    }

    public LLMModel findModelById(int modelId) throws SQLException {
        String sql = "SELECT * FROM llm_model WHERE model_id = ?";

        try (Connection connection = DBConnection.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {

            statement.setInt(1, modelId);
            try (ResultSet rs = statement.executeQuery()) {
                if (rs.next()) {
                    LLMModel model = new LLMModel();
                    model.setModelId(rs.getInt("model_id"));
                    model.setModelName(rs.getString("model_name"));
                    model.setModelTag(rs.getString("model_tag"));
                    model.setProvider(rs.getString("provider"));
                    model.setDescription(rs.getString("description"));
                    try {
                        model.setCreatedAt(rs.getTimestamp("created_at"));
                    } catch (SQLException ignored) {
                    }
                    return model;
                }
            }
        }
        return null;
    }

    public String getModelTagById(int modelId) throws SQLException {
        LLMModel model = findModelById(modelId);
        return model == null ? null : model.getModelTag();
    }

    public PromptTechnique findPromptTechniqueById(int techniqueId) throws SQLException {
        String sql = "SELECT * FROM prompt_technique WHERE technique_id = ?";

        try (Connection connection = DBConnection.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {

            statement.setInt(1, techniqueId);
            try (ResultSet rs = statement.executeQuery()) {
                if (rs.next()) {
                    PromptTechnique technique = new PromptTechnique();
                    technique.setTechniqueId(rs.getInt("technique_id"));
                    technique.setTechniqueName(rs.getString("technique_name"));
                    technique.setSystemPromptFile(rs.getString("system_prompt_file"));
                    technique.setUserPromptFile(rs.getString("user_prompt_file"));
                    technique.setPromptVersion(rs.getString("prompt_version"));
                    technique.setDescription(rs.getString("description"));
                    try {
                        technique.setCreatedAt(rs.getTimestamp("created_at"));
                    } catch (SQLException ignored) {
                    }
                    return technique;
                }
            }
        }
        return null;
    }

    public List<LLMModel> findAllModels() throws SQLException {
        List<LLMModel> models = new ArrayList<>();
        String sql = "SELECT * FROM llm_model ORDER BY model_id";

        try (Connection connection = DBConnection.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql);
             ResultSet rs = statement.executeQuery()) {

            while (rs.next()) {
                LLMModel model = new LLMModel();
                model.setModelId(rs.getInt("model_id"));
                model.setModelName(rs.getString("model_name"));
                model.setModelTag(rs.getString("model_tag"));
                model.setProvider(rs.getString("provider"));
                model.setDescription(rs.getString("description"));
                models.add(model);
            }
        }
        return models;
    }

    public List<PromptTechnique> findAllPromptTechniques() throws SQLException {
        List<PromptTechnique> techniques = new ArrayList<>();
        String sql = "SELECT * FROM prompt_technique ORDER BY technique_id";

        try (Connection connection = DBConnection.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql);
             ResultSet rs = statement.executeQuery()) {

            while (rs.next()) {
                PromptTechnique technique = new PromptTechnique();
                technique.setTechniqueId(rs.getInt("technique_id"));
                technique.setTechniqueName(rs.getString("technique_name"));
                technique.setSystemPromptFile(rs.getString("system_prompt_file"));
                technique.setUserPromptFile(rs.getString("user_prompt_file"));
                technique.setPromptVersion(rs.getString("prompt_version"));
                technique.setDescription(rs.getString("description"));
                techniques.add(technique);
            }
        }
        return techniques;
    }

    private Experiment mapExperiment(ResultSet rs) throws SQLException {
        Experiment experiment = new Experiment();
        experiment.setExperimentId(rs.getInt("experiment_id"));
        experiment.setTranscriptId(rs.getInt("transcript_id"));
        experiment.setModelId(rs.getInt("model_id"));
        experiment.setTechniqueId(rs.getInt("technique_id"));
        experiment.setRagEnabled(rs.getBoolean("rag_enabled"));
        experiment.setStatus(rs.getString("status"));
        Timestamp executedAt = null;
        try {
            executedAt = rs.getTimestamp("executed_at");
        } catch (SQLException ignored) {
        }
        experiment.setExecutedAt(executedAt);
        return experiment;
    }
    public String getExperimentStatus(int experimentId) {
        String sql = "SELECT status FROM experiment WHERE experiment_id = ?";

        try (
                Connection conn = DBConnection.getConnection();
                PreparedStatement stmt = conn.prepareStatement(sql)
        ) {
            stmt.setInt(1, experimentId);

            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return rs.getString("status");
                }
            }

        } catch (SQLException e) {
            System.out.println("Error checking experiment status: " + e.getMessage());
        }

        return "";
    }
}
