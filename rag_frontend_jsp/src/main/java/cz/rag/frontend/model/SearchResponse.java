package cz.rag.frontend.model;

import java.util.List;

public class SearchResponse {

    private String query;
    private String mode;
    private List<SearchHit> hits = List.of();

    public String getQuery() { return query; }
    public void setQuery(String query) { this.query = query; }

    public String getMode() { return mode; }
    public void setMode(String mode) { this.mode = mode; }

    public List<SearchHit> getHits() { return hits; }
    public void setHits(List<SearchHit> hits) { this.hits = hits; }
}
