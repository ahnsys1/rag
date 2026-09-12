package cz.rag.frontend.model;

public class UploadResponse {

    private RagDocument document;
    private boolean duplicate;

    public RagDocument getDocument() { return document; }
    public void setDocument(RagDocument document) { this.document = document; }

    public boolean isDuplicate() { return duplicate; }
    public void setDuplicate(boolean duplicate) { this.duplicate = duplicate; }
}
