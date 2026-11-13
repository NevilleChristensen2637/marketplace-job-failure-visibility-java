package example.infrai;

import com.sun.net.httpserver.HttpServer;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

public final class InfraiClientTest {
  public static void main(String[] args) throws Exception {
    Map<String, Object> parsed = Envelope.parse("{\"ok\": false,\"data\":null,\"error\":{\"code\":\"BAD\"},\"metadata\":{}}");
    check(Boolean.FALSE.equals(parsed.get("ok")) && parsed.get("error") instanceof Map);
    parsed = Envelope.parse("{\"ok\":true,\"data\":{\"items\":[1,true,null,\"a\\n\\u4e2d\"]}}");
    check(parsed.get("data") instanceof Map);
    for (String invalid : new String[]{"{}", "not json", "{\"ok\":true} trailing", "{\"ok\":true,\"data\":[1,]}"}) {
      try { Envelope.parse(invalid); throw new AssertionError("Accepted invalid envelope"); }
      catch (IllegalArgumentException expected) { }
    }
    String special = "quote\" slash\\ newline\n tab\t control\u0001 中";
    AtomicInteger attempts = new AtomicInteger();
    AtomicInteger checked = new AtomicInteger();
    HttpServer server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
    server.createContext("/", exchange -> {
      String path = exchange.getRequestURI().getPath();
      String body = new String(exchange.getRequestBody().readAllBytes(), StandardCharsets.UTF_8);
      int status = 200;
      String response = "{\"ok\":true,\"data\":{\"message_id\":\"id\"}}";
      if (path.equals("/retry") && attempts.incrementAndGet() == 1) {
        status = 429; response = "{\"ok\": false,\"error\":{\"code\":\"RATE_LIMIT\"}}";
      } else if (path.equals("/fail")) {
        response = "{\"ok\": false,\"error\":{\"code\":\"BAD\"}}";
      } else if (path.startsWith("/v1/")) {
        Map<String, Object> request = (Map<String, Object>) Envelope.parse("{\"ok\":true,\"data\":" + body + "}").get("data");
        if (path.endsWith("cron/create")) check(special.equals(request.get("task")) && special.equals(request.get("cron_expr")));
        else {
          check(special.equals(request.get("queue")));
          if (path.endsWith("publish")) check(special.equals(((Map<?, ?>) request.get("payload")).get("message")));
          if (path.endsWith("ack")) check(special.equals(request.get("message_id")));
        }
        checked.incrementAndGet();
      }
      byte[] bytes = response.getBytes(StandardCharsets.UTF_8);
      exchange.sendResponseHeaders(status, bytes.length);
      exchange.getResponseBody().write(bytes);
      exchange.close();
    });
    server.start();
    try {
      InfraiClient client = new InfraiClient("http://127.0.0.1:" + server.getAddress().getPort(), "test");
      check(client.call("POST", "/retry", "{}").get("data") instanceof Map);
      check(attempts.get() == 2);
      try { client.call("POST", "/fail", "{}"); throw new AssertionError("False success"); }
      catch (InfraiException expected) { check(expected.getMessage().contains("BAD")); }
      client.cronCreate(special, special);
      client.queuePublish(special, special);
      client.queueConsume(special, 1, 30);
      client.queueAck(special, special);
      check(checked.get() == 4);
    } finally { server.stop(0); }
    System.out.println("Envelope, retry, failure and JSON escaping checks passed");
  }
  private static void check(boolean condition) { if (!condition) throw new AssertionError(); }
}
