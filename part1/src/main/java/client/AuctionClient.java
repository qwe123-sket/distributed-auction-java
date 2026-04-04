package client;

import frontend.*;
import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;

/**
 * Coursework demo client: exercises register, newAuction, bid, listItems, getSpec, closeAuction
 * and edge cases required before submission.
 */
public class AuctionClient {

    private static void expect(String label, boolean ok) {
        System.out.println((ok ? "[PASS] " : "[FAIL] ") + label);
    }

    private static boolean isEmptyResult(AuctionResult r) {
        return r.getItemId() == 0 && r.getWinningUser() == 0 && r.getPrice() == 0;
    }

    public static void main(String[] args) {
        ManagedChannel ch = ManagedChannelBuilder.forAddress("localhost", 50055)
                .usePlaintext()
                .build();
        try {
            var stub = AuctionServiceGrpc.newBlockingStub(ch);

            int alice = stub.register(RegisterRequest.newBuilder().setEmail("alice@lancaster.ac.uk").build()).getUserId();
            int bob = stub.register(RegisterRequest.newBuilder().setEmail("bob@lancaster.ac.uk").build()).getUserId();
            int carol = stub.register(RegisterRequest.newBuilder().setEmail("carol@lancaster.ac.uk").build()).getUserId();
            System.out.printf("Users -> alice=%d bob=%d carol=%d%n", alice, bob, carol);
            expect("three distinct user ids", alice > 0 && bob > 0 && carol > 0 && alice != bob && bob != carol);

            // 1. Start auctions
            int item1 = stub.newAuction(NewAuctionRequest.newBuilder()
                    .setUserId(alice)
                    .setName("Vintage watch")
                    .setDescription("Gold case")
                    .setReservePrice(100)
                    .build()).getItemId();
            int item2 = stub.newAuction(NewAuctionRequest.newBuilder()
                    .setUserId(bob)
                    .setName("Laptop")
                    .setDescription("Used laptop")
                    .setReservePrice(200)
                    .build()).getItemId();
            System.out.printf("Auctions -> item1=%d item2=%d%n", item1, item2);
            expect("newAuction item1", item1 > 0);
            expect("newAuction item2", item2 > 0);

            int badAuction = stub.newAuction(NewAuctionRequest.newBuilder()
                    .setUserId(999_999)
                    .setName("x")
                    .setDescription("y")
                    .setReservePrice(1)
                    .build()).getItemId();
            expect("newAuction unknown user returns failure", badAuction <= 0);

            // 2. Bids
            boolean bid1 = stub.bid(BidRequest.newBuilder().setUserId(bob).setItemId(item1).setPrice(120).build()).getSuccess();
            boolean bidLow = stub.bid(BidRequest.newBuilder().setUserId(carol).setItemId(item1).setPrice(110).build()).getSuccess();
            boolean bid2 = stub.bid(BidRequest.newBuilder().setUserId(carol).setItemId(item1).setPrice(150).build()).getSuccess();
            System.out.printf("Bids on item1 -> bob@120=%s carol@110(below high)=%s carol@150=%s%n", bid1, bidLow, bid2);
            expect("bid above reserve and above previous high", bid1);
            expect("bid below current high rejected", !bidLow);
            expect("bid raises highest", bid2);

            boolean bidBobItem = stub.bid(BidRequest.newBuilder().setUserId(alice).setItemId(item2).setPrice(250).build()).getSuccess();
            System.out.printf("Bid on item2 -> alice@250=%s%n", bidBobItem);
            expect("bid on item2 above reserve", bidBobItem);

            // 3. listItems + getSpec
            ListReply list = stub.listItems(Empty.newBuilder().build());
            System.out.println("listItems count=" + list.getItemsCount());
            expect("listItems has both auctions", list.getItemsCount() >= 2);

            Item spec = stub.getSpec(GetSpecRequest.newBuilder().setItemId(item1).build());
            System.out.printf("getSpec(item1) highestBid=%d reserve=%d%n", spec.getHighestBid(), spec.getReservePrice());
            expect("getSpec shows raised highest bid", spec.getHighestBid() == 150 && spec.getReservePrice() == 100);

            Item missing = stub.getSpec(GetSpecRequest.newBuilder().setItemId(999_999).build());
            expect("getSpec missing item returns empty", missing.getItemId() == 0);

            // 4. Close as owner
            AuctionResult closed = stub.closeAuction(CloseRequest.newBuilder().setUserId(alice).setItemId(item1).build());
            System.out.printf("close item1 by alice -> winner=%d price=%d%n", closed.getWinningUser(), closed.getPrice());
            expect("close owner succeeds with winner carol @150",
                    !isEmptyResult(closed) && closed.getWinningUser() == carol && closed.getPrice() == 150);

            // 5. Edge cases
            boolean ghostBid = stub.bid(BidRequest.newBuilder().setUserId(bob).setItemId(999_999).setPrice(500).build()).getSuccess();
            expect("bid non-existent item rejected", !ghostBid);

            int cheap = stub.newAuction(NewAuctionRequest.newBuilder()
                    .setUserId(carol)
                    .setName("Pen")
                    .setDescription("Blue ink")
                    .setReservePrice(80)
                    .build()).getItemId();
            boolean belowReserve = stub.bid(BidRequest.newBuilder().setUserId(alice).setItemId(cheap).setPrice(50).build()).getSuccess();
            expect("bid below reserve rejected", !belowReserve);

            AuctionResult closeAgain = stub.closeAuction(CloseRequest.newBuilder().setUserId(alice).setItemId(item1).build());
            expect("close already-closed auction -> empty result", isEmptyResult(closeAgain));

            AuctionResult thief = stub.closeAuction(CloseRequest.newBuilder().setUserId(carol).setItemId(item2).build());
            expect("non-owner cannot close bob's item2", isEmptyResult(thief));

            AuctionResult okClose2 = stub.closeAuction(CloseRequest.newBuilder().setUserId(bob).setItemId(item2).build());
            expect("owner bob closes item2", !isEmptyResult(okClose2) && okClose2.getWinningUser() == alice && okClose2.getPrice() == 250);

            // 6. Short summary
            System.out.println("--- Summary: exercise register/newAuction/bid/list/getSpec/close + edge cases; all [PASS] above should print for a correct server. ---");
        } finally {
            ch.shutdown();
        }
    }
}
