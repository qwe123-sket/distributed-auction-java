package common;
import java.io.Serializable;

// This class represents each state-mutating operations (register, new_auction, bid, and close) as an operation object

public class Operation implements Serializable {
    public enum Type { REGISTER, NEW_AUCTION, BID, CLOSE }
    public final Type type;
    public final int userId;
    public final String name;        // for new_auction
    public final String description; // for new_auction
    public final int reservePrice; // reserve for new_auction
    public final int itemId;         // for bid/close
    public final String email;

    private Operation(Type t, int userId, String email, String name, String desc, int reserve, int itemId) {
        this.type=t; this.userId=userId; this.email=email;
        this.name=name; this.description=desc; this.reservePrice=reserve; this.itemId=itemId;
    }
    public static Operation register(String email) {
        return new Operation(Type.REGISTER, 0, email, null, null, 0, 0);
    }
    public static Operation newAuction(int userId, String n, String d, int reserve) {
        return new Operation(Type.NEW_AUCTION, userId, null, n, d, reserve, 0);
    }
    public static Operation bid(int userId, int itemId, int price) {
        return new Operation(Type.BID, userId, null, null, null, price, itemId);
    }
    public static Operation close(int userId, int itemId) {
        return new Operation(Type.CLOSE, userId, null, null, null, 0, itemId);
    }
}
