package cz.rag.frontend.model;

/** One row in the upload result list shown after submitting files (Angular `UploadItem`). */
public class UploadItem {

    private final String name;
    private final String state; // uploading | accepted | duplicate | error
    private final String message;

    public UploadItem(String name, String state, String message) {
        this.name = name;
        this.state = state;
        this.message = message;
    }

    public String getName() { return name; }
    public String getState() { return state; }
    public String getMessage() { return message; }
}
