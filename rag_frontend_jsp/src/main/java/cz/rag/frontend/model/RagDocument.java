package cz.rag.frontend.model;

import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;

public class RagDocument {

    private static final DateTimeFormatter CREATED_FMT = DateTimeFormatter.ofPattern("d.M.yyyy HH:mm");

    private String id;
    private String filename;
    private String contentType;
    private long sizeBytes;
    private String status;
    private String error;
    private int chunkCount;
    private Integer pageCount;
    private String createdAt;
    private String indexedAt;

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getFilename() { return filename; }
    public void setFilename(String filename) { this.filename = filename; }

    public String getContentType() { return contentType; }
    public void setContentType(String contentType) { this.contentType = contentType; }

    public long getSizeBytes() { return sizeBytes; }
    public void setSizeBytes(long sizeBytes) { this.sizeBytes = sizeBytes; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public String getError() { return error; }
    public void setError(String error) { this.error = error; }

    public int getChunkCount() { return chunkCount; }
    public void setChunkCount(int chunkCount) { this.chunkCount = chunkCount; }

    public Integer getPageCount() { return pageCount; }
    public void setPageCount(Integer pageCount) { this.pageCount = pageCount; }

    public String getCreatedAt() { return createdAt; }
    public void setCreatedAt(String createdAt) { this.createdAt = createdAt; }

    public String getIndexedAt() { return indexedAt; }
    public void setIndexedAt(String indexedAt) { this.indexedAt = indexedAt; }

    public long getSizeKb() {
        return Math.round(sizeBytes / 1024.0);
    }

    public boolean isProcessing() {
        return "processing".equals(status);
    }

    public boolean isIndexed() {
        return "indexed".equals(status);
    }

    /** Backend returns ISO-8601 with offset; shown in the server's local zone like Angular's DatePipe. */
    public String getCreatedAtFormatted() {
        if (createdAt == null) {
            return "";
        }
        try {
            return OffsetDateTime.parse(createdAt).atZoneSameInstant(ZoneId.systemDefault()).format(CREATED_FMT);
        } catch (DateTimeParseException e) {
            return createdAt;
        }
    }
}
