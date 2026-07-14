package model;

import java.sql.Timestamp;

public class PromptTechnique {

    private int techniqueId;
    private String techniqueName;
    private String systemPromptFile;
    private String userPromptFile;
    private String promptVersion;
    private String description;
    private Timestamp createdAt;

    public PromptTechnique() {
    }

    public int getTechniqueId() {
        return techniqueId;
    }

    public void setTechniqueId(int techniqueId) {
        this.techniqueId = techniqueId;
    }

    public String getTechniqueName() {
        return techniqueName;
    }

    public void setTechniqueName(String techniqueName) {
        this.techniqueName = techniqueName;
    }

    public String getSystemPromptFile() {
        return systemPromptFile;
    }

    public void setSystemPromptFile(String systemPromptFile) {
        this.systemPromptFile = systemPromptFile;
    }

    public String getUserPromptFile() {
        return userPromptFile;
    }

    public void setUserPromptFile(String userPromptFile) {
        this.userPromptFile = userPromptFile;
    }

    public String getPromptVersion() {
        return promptVersion;
    }

    public void setPromptVersion(String promptVersion) {
        this.promptVersion = promptVersion;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public Timestamp getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Timestamp createdAt) {
        this.createdAt = createdAt;
    }

    @Override
    public String toString() {
        return techniqueId + " - " + techniqueName;
    }
}
