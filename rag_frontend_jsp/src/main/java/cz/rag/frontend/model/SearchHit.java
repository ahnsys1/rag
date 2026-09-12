package cz.rag.frontend.model;

public class SearchHit {

    private long chunkId;
    private String documentId;
    private String filename;
    private int chunkIndex;
    private Integer page;
    private String content;
    private double score;

    public long getChunkId() { return chunkId; }
    public void setChunkId(long chunkId) { this.chunkId = chunkId; }

    public String getDocumentId() { return documentId; }
    public void setDocumentId(String documentId) { this.documentId = documentId; }

    public String getFilename() { return filename; }
    public void setFilename(String filename) { this.filename = filename; }

    public int getChunkIndex() { return chunkIndex; }
    public void setChunkIndex(int chunkIndex) { this.chunkIndex = chunkIndex; }

    public Integer getPage() { return page; }
    public void setPage(Integer page) { this.page = page; }

    public String getContent() { return content; }
    public void setContent(String content) { this.content = content; }

    public double getScore() { return score; }
    public void setScore(double score) { this.score = score; }
}
