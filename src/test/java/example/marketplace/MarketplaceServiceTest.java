package example.marketplace;
import example.infrai.InfraiClient;
public final class MarketplaceServiceTest {
  public static void main(String[] args) throws Exception {
    if (!"REJECTED".equals(new MarketplaceService(null).handoff("", "buyer"))) throw new AssertionError();
    System.out.println("decision: REJECTED");
  }
}
