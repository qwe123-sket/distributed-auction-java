package replica;
import java.io.Serializable;

public class AuctionSaleItem implements Serializable {
    public final String name;
    public final String description;
    public final int reservePrice;
    public AuctionSaleItem(String n, String d, int r) {
        this.name = n; this.description = d; this.reservePrice = r;
    }
}
