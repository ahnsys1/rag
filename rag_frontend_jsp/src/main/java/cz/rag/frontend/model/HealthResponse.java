package cz.rag.frontend.model;

public class HealthResponse {

    private String status;
    private String embeddingModel;
    private int embeddingDim;
    private String llmProvider;
    private String llmModel;

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public String getEmbeddingModel() { return embeddingModel; }
    public void setEmbeddingModel(String embeddingModel) { this.embeddingModel = embeddingModel; }

    public int getEmbeddingDim() { return embeddingDim; }
    public void setEmbeddingDim(int embeddingDim) { this.embeddingDim = embeddingDim; }

    public String getLlmProvider() { return llmProvider; }
    public void setLlmProvider(String llmProvider) { this.llmProvider = llmProvider; }

    public String getLlmModel() { return llmModel; }
    public void setLlmModel(String llmModel) { this.llmModel = llmModel; }

    /** Last path segment of the model id, e.g. "paraphrase-multilingual-MiniLM-L12-v2". */
    public String getEmbeddingModelShort() {
        if (embeddingModel == null) {
            return "";
        }
        int slash = embeddingModel.lastIndexOf('/');
        return slash < 0 ? embeddingModel : embeddingModel.substring(slash + 1);
    }

    public boolean isLlmAvailable() {
        return llmProvider != null && !"none".equalsIgnoreCase(llmProvider);
    }
}
