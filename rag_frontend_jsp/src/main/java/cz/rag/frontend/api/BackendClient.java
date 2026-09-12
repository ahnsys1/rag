package cz.rag.frontend.api;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.json.JsonMapper;

import cz.rag.frontend.model.AskResponse;
import cz.rag.frontend.model.DownloadedFile;
import cz.rag.frontend.model.HealthResponse;
import cz.rag.frontend.model.RagDocument;
import cz.rag.frontend.model.SearchRequest;
import cz.rag.frontend.model.SearchResponse;
import cz.rag.frontend.model.UploadResponse;

/** Thin HTTP client for the RAG backend REST API (same endpoints the Angular app used). */
public final class BackendClient {

    private static final Map<Integer, String> STATUS_TEXT = Map.of(
            400, "Bad Request", 404, "Not Found", 413, "Request Entity Too Large",
            415, "Unsupported Media Type", 422, "Unprocessable Entity",
            500, "Internal Server Error", 502, "Bad Gateway", 503, "Service Unavailable",
            504, "Gateway Timeout");

    private static final BackendClient INSTANCE = new BackendClient(resolveBaseUrl());

    private final String baseUrl;
    private final HttpClient http = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(3))
            .followRedirects(HttpClient.Redirect.NEVER)
            .build();
    private final ObjectMapper mapper = JsonMapper.builder()
            .propertyNamingStrategy(PropertyNamingStrategies.SNAKE_CASE)
            .disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES)
            .build();

    private BackendClient(String baseUrl) {
        this.baseUrl = baseUrl;
    }

    public static BackendClient get() {
        return INSTANCE;
    }

    private static String resolveBaseUrl() {
        String url = System.getenv().getOrDefault("RAG_BACKEND_URL", "http://localhost:8000");
        return url.endsWith("/") ? url.substring(0, url.length() - 1) : url;
    }

    public HealthResponse health() {
        return send(get("/api/health", Duration.ofSeconds(3)), HealthResponse.class);
    }

    public List<RagDocument> listDocuments() {
        return send(get("/api/documents", Duration.ofSeconds(15)), new TypeReference<>() { });
    }

    public UploadResponse upload(String filename, String contentType, byte[] data) {
        String boundary = "----rag" + UUID.randomUUID();
        String safeName = filename.replace("\"", "%22").replace("\r", "").replace("\n", "");
        String head = "--" + boundary + "\r\n"
                + "Content-Disposition: form-data; name=\"file\"; filename=\"" + safeName + "\"\r\n"
                + "Content-Type: " + (contentType == null ? "application/octet-stream" : contentType)
                + "\r\n\r\n";
        String tail = "\r\n--" + boundary + "--\r\n";
        HttpRequest req = HttpRequest.newBuilder(URI.create(baseUrl + "/api/documents"))
                .timeout(Duration.ofMinutes(5))
                .header("Content-Type", "multipart/form-data; boundary=" + boundary)
                .POST(HttpRequest.BodyPublishers.ofByteArrays(List.of(
                        head.getBytes(StandardCharsets.UTF_8), data,
                        tail.getBytes(StandardCharsets.UTF_8))))
                .build();
        return send(req, UploadResponse.class);
    }

    public void delete(String id) {
        HttpRequest req = HttpRequest.newBuilder(URI.create(baseUrl + "/api/documents/" + id))
                .timeout(Duration.ofSeconds(30))
                .DELETE()
                .build();
        exchange(req, HttpResponse.BodyHandlers.ofByteArray());
    }

    public DownloadedFile download(String id) {
        HttpRequest req = get("/api/documents/" + id + "/download", Duration.ofMinutes(5));
        HttpResponse<byte[]> resp = exchange(req, HttpResponse.BodyHandlers.ofByteArray());
        return new DownloadedFile(
                resp.headers().firstValue("Content-Type").orElse("application/octet-stream"),
                resp.headers().firstValue("Content-Disposition").orElse(null),
                resp.body());
    }

    public SearchResponse search(SearchRequest request) {
        return send(postJson("/api/search", request, Duration.ofSeconds(60)), SearchResponse.class);
    }

    public AskResponse ask(SearchRequest request) {
        return send(postJson("/api/ask", request, Duration.ofMinutes(5)), AskResponse.class);
    }

    // ---- helpers -----------------------------------------------------------------------

    private HttpRequest get(String path, Duration timeout) {
        return HttpRequest.newBuilder(URI.create(baseUrl + path)).timeout(timeout).GET().build();
    }

    private HttpRequest postJson(String path, Object body, Duration timeout) {
        try {
            return HttpRequest.newBuilder(URI.create(baseUrl + path))
                    .timeout(timeout)
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofByteArray(mapper.writeValueAsBytes(body)))
                    .build();
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    private <T> T send(HttpRequest req, Class<T> type) {
        return parse(exchange(req, HttpResponse.BodyHandlers.ofByteArray()).body(), type);
    }

    private <T> T send(HttpRequest req, TypeReference<T> type) {
        try {
            return mapper.readValue(exchange(req, HttpResponse.BodyHandlers.ofByteArray()).body(), type);
        } catch (IOException e) {
            throw new BackendException(0, "Neplatná odpověď backendu.");
        }
    }

    private <T> T parse(byte[] body, Class<T> type) {
        try {
            return mapper.readValue(body, type);
        } catch (IOException e) {
            throw new BackendException(0, "Neplatná odpověď backendu.");
        }
    }

    private <T> HttpResponse<T> exchange(HttpRequest req, HttpResponse.BodyHandler<T> handler) {
        HttpResponse<T> resp;
        try {
            resp = http.send(req, handler);
        } catch (IOException e) {
            throw new BackendException(0, "Backend není dostupný.");
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new BackendException(0, "Požadavek byl přerušen.");
        }
        if (resp.statusCode() >= 400) {
            throw new BackendException(resp.statusCode(), describeError(resp));
        }
        return resp;
    }

    /** Mirrors the Angular `describe()` helper: FastAPI `detail` string, else "status reason". */
    private String describeError(HttpResponse<?> resp) {
        Object body = resp.body();
        if (body instanceof byte[] bytes && bytes.length > 0) {
            try {
                JsonNode detail = mapper.readTree(bytes).path("detail");
                if (detail.isTextual()) {
                    return detail.asText();
                }
            } catch (IOException ignored) {
                // fall through to generic message
            }
        }
        int status = resp.statusCode();
        return status + " " + STATUS_TEXT.getOrDefault(status, "Error");
    }
}
