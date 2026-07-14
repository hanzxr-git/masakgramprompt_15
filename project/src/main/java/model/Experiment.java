package model;

import java.sql.Timestamp;

public class Experiment {

    private int experimentId;
    private int transcriptId;
    private int modelId;
    private int techniqueId;
    private boolean ragEnabled;
    private String status;
    private Timestamp executedAt;

    public Experiment() {
    }

    public Experiment(int transcriptId, int modelId, int techniqueId, boolean ragEnabled, String status) {
        this.transcriptId = transcriptId;
        this.modelId = modelId;
        this.techniqueId = techniqueId;
        this.ragEnabled = ragEnabled;
        this.status = status;
    }

    public int getExperimentId() {
        return experimentId;
    }

    public void setExperimentId(int experimentId) {
        this.experimentId = experimentId;
    }

    public int getTranscriptId() {
        return transcriptId;
    }

    public void setTranscriptId(int transcriptId) {
        this.transcriptId = transcriptId;
    }

    public int getModelId() {
        return modelId;
    }

    public void setModelId(int modelId) {
        this.modelId = modelId;
    }

    public int getTechniqueId() {
        return techniqueId;
    }

    public void setTechniqueId(int techniqueId) {
        this.techniqueId = techniqueId;
    }

    public boolean isRagEnabled() {
        return ragEnabled;
    }

    public void setRagEnabled(boolean ragEnabled) {
        this.ragEnabled = ragEnabled;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public Timestamp getExecutedAt() {
        return executedAt;
    }

    public void setExecutedAt(Timestamp executedAt) {
        this.executedAt = executedAt;
    }

    @Override
    public String toString() {
        return "Experiment{" +
                "experimentId=" + experimentId +
                ", transcriptId=" + transcriptId +
                ", modelId=" + modelId +
                ", techniqueId=" + techniqueId +
                ", ragEnabled=" + ragEnabled +
                ", status='" + status + '\'' +
                ", executedAt=" + executedAt +
                '}';
    }
}
