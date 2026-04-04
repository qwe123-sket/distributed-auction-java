package server;
import java.io.Serializable;

public class AuctionResult implements Serializable {
    public final int itemID;
    public final int winningUser;
    public final int price;
    public AuctionResult(int itemID, int user, int price) {
        this.itemID = itemID; this.winningUser = user; this.price = price;
    }
}
