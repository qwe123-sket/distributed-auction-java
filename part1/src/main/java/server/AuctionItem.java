package server;
import java.io.Serializable;

public class AuctionItem implements Serializable {
    public final int itemID;
    public final String name;
    public final String description;
    public final int reservePrice;
    public int highestBid;

    public AuctionItem(int id, String n, String d, int r) {
        this.itemID = id; this.name = n; this.description = d;
        this.reservePrice = r; this.highestBid = 0;
    }
}
