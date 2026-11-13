package example.infrai;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.Map;

public final class Envelope {
  private Envelope() {}

  @SuppressWarnings("unchecked")
  public static Map<String, Object> parse(String body) {
    Parser parser = new Parser(body);
    Object value = parser.value();
    parser.space();
    if (parser.pos != body.length() || !(value instanceof Map)
        || !(((Map<?, ?>) value).get("ok") instanceof Boolean)) {
      throw new IllegalArgumentException("Invalid JSON envelope");
    }
    return (Map<String, Object>) value;
  }

  static String quote(String value) {
    if (value == null) return "null";
    StringBuilder result = new StringBuilder("\"");
    for (int i = 0; i < value.length(); i++) {
      char c = value.charAt(i);
      switch (c) {
        case '"': result.append("\\\""); break;
        case '\\': result.append("\\\\"); break;
        case '\b': result.append("\\b"); break;
        case '\f': result.append("\\f"); break;
        case '\n': result.append("\\n"); break;
        case '\r': result.append("\\r"); break;
        case '\t': result.append("\\t"); break;
        default:
          if (c < 0x20) result.append(String.format("\\u%04x", (int) c));
          else result.append(c);
      }
    }
    return result.append('"').toString();
  }

  private static final class Parser {
    private final String text;
    private int pos;
    Parser(String text) { this.text = text; }
    void space() {
      while (pos < text.length() && " \n\r\t".indexOf(text.charAt(pos)) >= 0) pos++;
    }
    IllegalArgumentException invalid() { return new IllegalArgumentException("Invalid JSON at " + pos); }
    boolean take(char c) {
      space();
      if (pos < text.length() && text.charAt(pos) == c) { pos++; return true; }
      return false;
    }
    void require(char c) { if (!take(c)) throw invalid(); }
    Object value() {
      space();
      if (pos >= text.length()) throw invalid();
      char c = text.charAt(pos);
      if (c == '"') return string();
      if (take('{')) {
        Map<String, Object> map = new LinkedHashMap<>();
        if (take('}')) return map;
        do {
          space();
          String key = string();
          require(':');
          map.put(key, value());
        } while (take(','));
        require('}');
        return map;
      }
      if (take('[')) {
        ArrayList<Object> list = new ArrayList<>();
        if (take(']')) return list;
        do { list.add(value()); } while (take(','));
        require(']');
        return list;
      }
      String[] literals = {"true", "false", "null"};
      Object[] values = {Boolean.TRUE, Boolean.FALSE, null};
      for (int i = 0; i < literals.length; i++) {
        if (text.startsWith(literals[i], pos)) { pos += literals[i].length(); return values[i]; }
      }
      int start = pos;
      if (pos < text.length() && text.charAt(pos) == '-') pos++;
      if (pos < text.length() && text.charAt(pos) == '0') pos++;
      else digits();
      if (pos < text.length() && text.charAt(pos) == '.') { pos++; digits(); }
      if (pos < text.length() && "eE".indexOf(text.charAt(pos)) >= 0) {
        pos++;
        if (pos < text.length() && "+-".indexOf(text.charAt(pos)) >= 0) pos++;
        digits();
      }
      try { return new BigDecimal(text.substring(start, pos)); }
      catch (NumberFormatException e) { throw invalid(); }
    }
    void digits() {
      int start = pos;
      while (pos < text.length() && text.charAt(pos) >= '0' && text.charAt(pos) <= '9') pos++;
      if (start == pos) throw invalid();
    }
    String string() {
      require('"');
      StringBuilder result = new StringBuilder();
      while (pos < text.length()) {
        char c = text.charAt(pos++);
        if (c == '"') return result.toString();
        if (c < 0x20) throw invalid();
        if (c == '\\') {
          if (pos == text.length()) throw invalid();
          c = text.charAt(pos++);
          switch (c) {
            case '"': case '\\': case '/': break;
            case 'b': c = '\b'; break;
            case 'f': c = '\f'; break;
            case 'n': c = '\n'; break;
            case 'r': c = '\r'; break;
            case 't': c = '\t'; break;
            case 'u':
              if (pos + 4 > text.length()) throw invalid();
              int code = 0;
              for (int i = 0; i < 4; i++) {
                int digit = Character.digit(text.charAt(pos++), 16);
                if (digit < 0) throw invalid();
                code = code * 16 + digit;
              }
              c = (char) code;
              break;
            default: throw invalid();
          }
        }
        result.append(c);
      }
      throw invalid();
    }
  }
}
