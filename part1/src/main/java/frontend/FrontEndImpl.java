package frontend;

import io.grpc.stub.StreamObserver;
import server.*;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;

public class FrontEndImpl extends AuctionServiceGrpc.AuctionServiceImplBase {
    private final Auction auction;

    public FrontEndImpl() throws Exception {
        Registry reg = LocateRegistry.getRegistry();
        this.auction = (Auction) reg.lookup("AuctionServer");
    }

    @Override
    public void register(RegisterRequest req, StreamObserver<RegisterReply> resp) {
        try {
            int id = auction.register(req.getEmail());
            resp.onNext(RegisterReply.newBuilder().setUserId(id).build());
            resp.onCompleted();
        } catch (Exception e) {
            resp.onError(e);
        }
    }

    @Override
    public void newAuction(NewAuctionRequest req, StreamObserver<NewAuctionReply> resp) {
        try {
            AuctionSaleItem item = new AuctionSaleItem(
                    req.getName(),
                    req.getDescription(),
                    req.getReservePrice());
            int itemId = auction.newAuction(req.getUserId(), item);
            resp.onNext(NewAuctionReply.newBuilder().setItemId(itemId).build());
            resp.onCompleted();
        } catch (Exception e) {
            resp.onError(e);
        }
    }

    @Override
    public void bid(BidRequest req, StreamObserver<BidReply> resp) {
        try {
            boolean ok = auction.bid(req.getUserId(), req.getItemId(), req.getPrice());
            resp.onNext(BidReply.newBuilder().setSuccess(ok).build());
            resp.onCompleted();
        } catch (Exception e) {
            resp.onError(e);
        }
    }

    @Override
    public void listItems(Empty req, StreamObserver<ListReply> resp) {
        try {
            AuctionItem[] items = auction.listItems();
            ListReply.Builder lb = ListReply.newBuilder();
            for (AuctionItem it : items) {
                lb.addItems(toGrpcItem(it));
            }
            resp.onNext(lb.build());
            resp.onCompleted();
        } catch (Exception e) {
            resp.onError(e);
        }
    }

    @Override
    public void getSpec(GetSpecRequest req, StreamObserver<Item> resp) {
        try {
            AuctionItem it = auction.getSpec(req.getItemId());
            if (it == null) {
                resp.onNext(Item.getDefaultInstance());
            } else {
                resp.onNext(toGrpcItem(it));
            }
            resp.onCompleted();
        } catch (Exception e) {
            resp.onError(e);
        }
    }

    @Override
    public void closeAuction(CloseRequest req, StreamObserver<AuctionResult> resp) {
        try {
            server.AuctionResult ar = auction.closeAuction(req.getUserId(), req.getItemId());
            if (ar == null) {
                resp.onNext(frontend.AuctionResult.getDefaultInstance());
            } else {
                resp.onNext(frontend.AuctionResult.newBuilder()
                        .setItemId(ar.itemID)
                        .setWinningUser(ar.winningUser)
                        .setPrice(ar.price)
                        .build());
            }
            resp.onCompleted();
        } catch (Exception e) {
            resp.onError(e);
        }
    }

    private static Item toGrpcItem(AuctionItem it) {
        return Item.newBuilder()
                .setItemId(it.itemID)
                .setName(it.name)
                .setDescription(it.description)
                .setReservePrice(it.reservePrice)
                .setHighestBid(it.highestBid)
                .build();
    }
}
