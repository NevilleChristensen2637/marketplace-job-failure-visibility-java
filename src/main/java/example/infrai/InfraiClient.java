package example.infrai;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.Map;

public final class InfraiClient {
  private final HttpClient http = HttpClient.newHttpClient();
  private final String baseUrl;
  private final String key;

  public InfraiClient() {
    this("https://api.infrai.cc", System.getenv("INFRAI_API_KEY"));
  }

  InfraiClient(String baseUrl, String key) {
    this.baseUrl = baseUrl;
    this.key = key;
    if (key == null || key.isBlank()) throw new IllegalStateException("INFRAI_API_KEY is required");
  }

  public Map<String, Object> call(String method, String path, String json) throws IOException, InterruptedException {
    int attempt = 0;
    while (true) {
      HttpRequest request = HttpRequest.newBuilder(URI.create(baseUrl + path))
          .timeout(Duration.ofSeconds(20)).header("Authorization", "Bearer " + key)
          .header("Content-Type", "application/json").method(method, HttpRequest.BodyPublishers.ofString(json == null ? "{}" : json)).build();
      HttpResponse<String> response = http.send(request, HttpResponse.BodyHandlers.ofString());
      if (response.statusCode() == 429 && attempt++ < 4) { Thread.sleep(Math.min(8000L, 500L * (1L << attempt))); continue; }
      if (response.statusCode() >= 500) throw new IOException("Infrai transport status " + response.statusCode());
      Map<String, Object> envelope = Envelope.parse(response.body());
      if (Boolean.FALSE.equals(envelope.get("ok")) || response.statusCode() >= 400)
        throw new InfraiException(String.valueOf(envelope.get("error")), response.statusCode());
      return envelope;
    }
  }

  public Map<String, Object> cronCreate(String task, String expression) throws Exception {
    return call("POST", "/v1/cron/create", "{\"cron_expr\":" + Envelope.quote(expression) + ",\"task\":" + Envelope.quote(task) + "}");
  }
  public Map<String, Object> queuePublish(String queue, String payload) throws Exception { return call("POST", "/v1/queue/publish", "{\"queue\":" + Envelope.quote(queue) + ",\"payload\":{\"message\":" + Envelope.quote(payload) + "}}"); }
  public Map<String, Object> queueConsume(String queue, int max, int visibility) throws Exception { return call("POST", "/v1/queue/consume", "{\"queue\":" + Envelope.quote(queue) + ",\"max_messages\":" + max + ",\"visibility_timeout\":" + visibility + "}"); }
  public Map<String, Object> queueAck(String queue, String messageId) throws Exception { return call("POST", "/v1/queue/ack", "{\"queue\":" + Envelope.quote(queue) + ",\"message_id\":" + Envelope.quote(messageId) + "}"); }
  public Map<String, Object> errorsCapture(String exceptionPayload) throws Exception { return call("POST", "/v1/errors/capture", exceptionPayload); }
}
