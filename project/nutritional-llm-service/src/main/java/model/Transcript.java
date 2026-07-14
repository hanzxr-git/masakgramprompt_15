package model;

import java.sql.Timestamp;

public class Transcript {

    private int transcriptId;
    private int reelId;
    private int audioId;
    private String fileName;
    private String filePath;
    private Timestamp fileCreatedAt;
    private long fileSizeBytes;
    private String fileFormat;
    private boolean audioTranscriptConsistent;
    private String verifiedByMatric;
    private String verifiedByName;
    private Timestamp verifiedAt;

    /**
     * Some database versions include transcript_text/content, while the PDF version
     * focuses on file metadata. This field is used if your DB has the text column.
     */
    private String transcriptText;

    public Transcript() {
    }

    public int getTranscriptId() {
        return transcriptId;
    }

    public void setTranscriptId(int transcriptId) {
        this.transcriptId = transcriptId;
    }

    public int getReelId() {
        return reelId;
    }

    public void setReelId(int reelId) {
        this.reelId = reelId;
    }

    public int getAudioId() {
        return audioId;
    }

    public void setAudioId(int audioId) {
        this.audioId = audioId;
    }

    public String getFileName() {
        return fileName;
    }

    public void setFileName(String fileName) {
        this.fileName = fileName;
    }

    public String getFilePath() {
        return filePath;
    }

    public void setFilePath(String filePath) {
        this.filePath = filePath;
    }

    public Timestamp getFileCreatedAt() {
        return fileCreatedAt;
    }

    public void setFileCreatedAt(Timestamp fileCreatedAt) {
        this.fileCreatedAt = fileCreatedAt;
    }

    public long getFileSizeBytes() {
        return fileSizeBytes;
    }

    public void setFileSizeBytes(long fileSizeBytes) {
        this.fileSizeBytes = fileSizeBytes;
    }

    public String getFileFormat() {
        return fileFormat;
    }

    public void setFileFormat(String fileFormat) {
        this.fileFormat = fileFormat;
    }

    public boolean isAudioTranscriptConsistent() {
        return audioTranscriptConsistent;
    }

    public void setAudioTranscriptConsistent(boolean audioTranscriptConsistent) {
        this.audioTranscriptConsistent = audioTranscriptConsistent;
    }

    public String getVerifiedByMatric() {
        return verifiedByMatric;
    }

    public void setVerifiedByMatric(String verifiedByMatric) {
        this.verifiedByMatric = verifiedByMatric;
    }

    public String getVerifiedByName() {
        return verifiedByName;
    }

    public void setVerifiedByName(String verifiedByName) {
        this.verifiedByName = verifiedByName;
    }

    public Timestamp getVerifiedAt() {
        return verifiedAt;
    }

    public void setVerifiedAt(Timestamp verifiedAt) {
        this.verifiedAt = verifiedAt;
    }

    public String getTranscriptText() {
        return transcriptText;
    }

    public void setTranscriptText(String transcriptText) {
        this.transcriptText = transcriptText;
    }

    @Override
    public String toString() {
        return "Transcript{" +
                "transcriptId=" + transcriptId +
                ", reelId=" + reelId +
                ", audioId=" + audioId +
                ", fileName='" + fileName + '\'' +
                ", filePath='" + filePath + '\'' +
                '}';
    }
}
