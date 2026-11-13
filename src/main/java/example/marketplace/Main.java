package example.marketplace;
import example.infrai.InfraiClient;
public final class Main {
  public static void main(String[] args) throws Exception {
    MarketplaceService service = new MarketplaceService(new InfraiClient());
    System.out.println(service.handoff("asset-42", "buyer-paid"));
  }
}
