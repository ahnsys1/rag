package cz.rag.frontend.model;

import java.util.List;

public class AskResponse {

    private String question;
    private String answer;
    private String model;
    private List<SearchHit> sources = List.of();

    public String getQuestion() { return question; }
    public void setQuestion(String question) { this.question = question; }

    public String getAnswer() { return answer; }
    public void setAnswer(String answer) { this.answer = answer; }

    public String getModel() { return model; }
    public void setModel(String model) { this.model = model; }

    public List<SearchHit> getSources() { return sources; }
    public void setSources(List<SearchHit> sources) { this.sources = sources; }
}
