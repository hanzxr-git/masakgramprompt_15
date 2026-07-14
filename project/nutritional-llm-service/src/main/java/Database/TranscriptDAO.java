package Database;

import model.Transcript;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.List;

public class TranscriptDAO {

    public Transcript findById(int transcriptId) throws SQLException {
        String sql = "SELECT * FROM transcript WHERE transcript_id = ?";

        try (Connection connection = DBConnection.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {

            statement.setInt(1, transcriptId);
            try (ResultSet rs = statement.executeQuery()) {
                if (rs.next()) {
                    return mapTranscript(rs, connection);
                }
            }
        }
        return null;
    }

    public List<Transcript> findAll() throws SQLException {
        List<Transcript> transcripts = new ArrayList<>();
        String sql = "SELECT * FROM transcript ORDER BY transcript_id";

        try (Connection connection = DBConnection.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql);
             ResultSet rs = statement.executeQuery()) {

            while (rs.next()) {
                transcripts.add(mapTranscript(rs, connection));
            }
        }
        return transcripts;
    }

    /**
     * Gets actual transcript text. It supports these cases:
     * 1. Your transcript table has transcript_text/text/content/full_text column.
     * 2. Your transcript table only stores file_path, so this method reads the .txt file.
     */
    public String getTranscriptTextById(int transcriptId) throws SQLException {
        Transcript transcript = findById(transcriptId);
        if (transcript == null) {
            return null;
        }

        if (isNotBlank(transcript.getTranscriptText())) {
            return transcript.getTranscriptText();
        }

        if (isNotBlank(transcript.getFilePath())) {
            try {
                Path path = Paths.get(transcript.getFilePath());
                if (Files.exists(path)) {
                    return new String(Files.readAllBytes(path), StandardCharsets.UTF_8);
                }
            } catch (IOException e) {
                throw new SQLException("Transcript file exists in DB but cannot be read: " + transcript.getFilePath(), e);
            }
        }

        return null;
    }

    /**
     * Optional insert helper for testing. It will insert transcript_text only if the column exists.
     */
    public int insert(Transcript transcript) throws SQLException {
        try (Connection connection = DBConnection.getConnection()) {
            boolean hasTextColumn = hasAnyColumn(connection, "transcript", "transcript_text", "text", "content", "full_text");
            String textColumn = findFirstExistingColumn(connection, "transcript", "transcript_text", "text", "content", "full_text");

            StringBuilder sql = new StringBuilder("INSERT INTO transcript (reel_id, audio_id, file_name, file_path, file_size_bytes, file_format");
            if (hasColumn(connection, "transcript", "file_created_at")) sql.append(", file_created_at");
            if (hasColumn(connection, "transcript", "audio_transcript_consistent")) sql.append(", audio_transcript_consistent");
            if (hasColumn(connection, "transcript", "verified_by_matric")) sql.append(", verified_by_matric");
            if (hasColumn(connection, "transcript", "verified_by_name")) sql.append(", verified_by_name");
            if (hasColumn(connection, "transcript", "verified_at")) sql.append(", verified_at");
            if (hasTextColumn) sql.append(", ").append(textColumn);
            sql.append(") VALUES (?, ?, ?, ?, ?, ?");
            if (hasColumn(connection, "transcript", "file_created_at")) sql.append(", ?");
            if (hasColumn(connection, "transcript", "audio_transcript_consistent")) sql.append(", ?");
            if (hasColumn(connection, "transcript", "verified_by_matric")) sql.append(", ?");
            if (hasColumn(connection, "transcript", "verified_by_name")) sql.append(", ?");
            if (hasColumn(connection, "transcript", "verified_at")) sql.append(", ?");
            if (hasTextColumn) sql.append(", ?");
            sql.append(")");

            try (PreparedStatement statement = connection.prepareStatement(sql.toString(), Statement.RETURN_GENERATED_KEYS)) {
                int index = 1;
                statement.setInt(index++, transcript.getReelId());
                statement.setInt(index++, transcript.getAudioId());
                statement.setString(index++, transcript.getFileName());
                statement.setString(index++, transcript.getFilePath());
                statement.setLong(index++, transcript.getFileSizeBytes());
                statement.setString(index++, transcript.getFileFormat());

                if (hasColumn(connection, "transcript", "file_created_at")) statement.setTimestamp(index++, transcript.getFileCreatedAt());
                if (hasColumn(connection, "transcript", "audio_transcript_consistent")) statement.setBoolean(index++, transcript.isAudioTranscriptConsistent());
                if (hasColumn(connection, "transcript", "verified_by_matric")) statement.setString(index++, transcript.getVerifiedByMatric());
                if (hasColumn(connection, "transcript", "verified_by_name")) statement.setString(index++, transcript.getVerifiedByName());
                if (hasColumn(connection, "transcript", "verified_at")) statement.setTimestamp(index++, transcript.getVerifiedAt());
                if (hasTextColumn) statement.setString(index++, transcript.getTranscriptText());

                statement.executeUpdate();
                try (ResultSet keys = statement.getGeneratedKeys()) {
                    if (keys.next()) {
                        int id = keys.getInt(1);
                        transcript.setTranscriptId(id);
                        return id;
                    }
                }
            }
        }
        throw new SQLException("Failed to insert transcript; no generated key returned.");
    }

    private Transcript mapTranscript(ResultSet rs, Connection connection) throws SQLException {
        Transcript transcript = new Transcript();
        transcript.setTranscriptId(getInt(rs, "transcript_id"));
        transcript.setReelId(getInt(rs, "reel_id"));
        transcript.setAudioId(getInt(rs, "audio_id"));
        transcript.setFileName(getString(rs, "file_name"));
        transcript.setFilePath(getString(rs, "file_path"));
        transcript.setFileCreatedAt(getTimestamp(rs, "file_created_at"));
        transcript.setFileSizeBytes(getLong(rs, "file_size_bytes"));
        transcript.setFileFormat(getString(rs, "file_format"));
        transcript.setAudioTranscriptConsistent(getBoolean(rs, "audio_transcript_consistent"));
        transcript.setVerifiedByMatric(getString(rs, "verified_by_matric"));
        transcript.setVerifiedByName(getString(rs, "verified_by_name"));
        transcript.setVerifiedAt(getTimestamp(rs, "verified_at"));

        String textColumn = findFirstExistingColumn(connection, "transcript", "transcript_text", "text", "content", "full_text");
        if (textColumn != null) {
            transcript.setTranscriptText(getString(rs, textColumn));
        }
        return transcript;
    }

    private static boolean isNotBlank(String text) {
        return text != null && !text.trim().isEmpty();
    }

    private boolean hasAnyColumn(Connection connection, String tableName, String... columnNames) throws SQLException {
        return findFirstExistingColumn(connection, tableName, columnNames) != null;
    }

    private String findFirstExistingColumn(Connection connection, String tableName, String... columnNames) throws SQLException {
        for (String columnName : columnNames) {
            if (hasColumn(connection, tableName, columnName)) {
                return columnName;
            }
        }
        return null;
    }

    private boolean hasColumn(Connection connection, String tableName, String columnName) throws SQLException {
        DatabaseMetaData metaData = connection.getMetaData();
        try (ResultSet columns = metaData.getColumns(connection.getCatalog(), null, tableName, columnName)) {
            return columns.next();
        }
    }

    private int getInt(ResultSet rs, String column) {
        try {
            return rs.getInt(column);
        } catch (SQLException e) {
            return 0;
        }
    }

    private long getLong(ResultSet rs, String column) {
        try {
            return rs.getLong(column);
        } catch (SQLException e) {
            return 0L;
        }
    }

    private boolean getBoolean(ResultSet rs, String column) {
        try {
            return rs.getBoolean(column);
        } catch (SQLException e) {
            return false;
        }
    }

    private String getString(ResultSet rs, String column) {
        try {
            return rs.getString(column);
        } catch (SQLException e) {
            return null;
        }
    }

    private Timestamp getTimestamp(ResultSet rs, String column) {
        try {
            return rs.getTimestamp(column);
        } catch (SQLException e) {
            return null;
        }
    }
}
