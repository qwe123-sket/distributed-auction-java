package common;
import java.io.Serializable;

public class OperationResult implements Serializable {
    public final boolean ok;
    public final String error;       
    public final Integer userId;
    public final Integer itemId;
    public final Boolean bidOk;
    public final Integer closeItem;
    public final Integer closeWinner;
    public final Integer closePrice;

    private OperationResult(boolean ok, String error, Integer userId, Integer itemId, Boolean bidOk,
                            Integer closeItem, Integer closeWinner, Integer closePrice) {
        this.ok=ok; this.error=error;
        this.userId=userId; this.itemId=itemId; this.bidOk=bidOk;
        this.closeItem=closeItem; this.closeWinner=closeWinner; this.closePrice=closePrice;
    }
    /* Helpers for building typed OperationResult instances (you don't have to use them).
     * Methods:
     *  - reg(uid):        success result for REGISTER; sets userId=uid.
     *  - newA(item):      success result for NEW_AUCTION; sets itemId=item.
     *  - bid(ok):         success result for BID; sets bidOk=ok.
     *  - close(item,w,p): success result for CLOSE; sets closeItem, closeWinner, closePrice.
     *  - fail(msg):       failure result; ok=false and error=msg; all other fields null.
     */

    public static OperationResult reg(int uid) { 
        return new OperationResult(true,null,uid,null,null,null,null,null); 
    }

    public static OperationResult newA(int item) { 
        return new OperationResult(true,null,null,item,null,null,null,null); 
    }

    public static OperationResult bid(boolean ok) { 
        return new OperationResult(true,null,null,null,ok,null,null,null); 
    }

    public static OperationResult close(int item,int winner,int price){
        return new OperationResult(true,null,null,null,null,item,winner,price);
    }

    public static OperationResult fail(String msg) { 
        return new OperationResult(false,msg,null,null,null,null,null,null); 
    }
}

