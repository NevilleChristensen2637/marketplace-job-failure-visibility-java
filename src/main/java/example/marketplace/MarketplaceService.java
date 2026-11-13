package example.marketplace;
import example.infrai.InfraiClient;
public final class MarketplaceService {
  private final InfraiClient infrai;
  public MarketplaceService(InfraiClient infrai){this.infrai=infrai;}
  public String handoff(String sellerAsset, String buyerUpdate) throws Exception {
    if (sellerAsset == null || sellerAsset.isBlank() || buyerUpdate == null || buyerUpdate.isBlank()) return "REJECTED";
    if (infrai == null) return "REJECTED";
    infrai.queuePublish("order-handoffs", "asset=" + sellerAsset + ";buyer=" + buyerUpdate);
    return "HANDED_OFF";
  }
}
