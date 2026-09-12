package cz.rag.frontend.model;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonProperty;

public class SearchRequest {

    @JsonProperty("query")
    private String query;
    @JsonProperty("top_k")
    private int topK;
    @JsonProperty("mode")
    private String mode;
    @JsonProperty("document_ids")
    private List<String> documentIds;

    public SearchRequest() { }

    public SearchRequest(String query, int topK, String mode, List<String> documentIds) {
        this.query = query;
        this.topK = topK;
        this.mode = mode;
        this.documentIds = documentIds;
    }

    public String getQuery() { return query; }
    public void setQuery(String query) { this.query = query; }

    public int getTopK() { return topK; }
    public void setTopK(int topK) { this.topK = topK; }

    public String getMode() { return mode; }
    public void setMode(String mode) { this.mode = mode; }

    public List<String> getDocumentIds() { return documentIds; }
    public void setDocumentIds(List<String> documentIds) { this.documentIds = documentIds; }
}
