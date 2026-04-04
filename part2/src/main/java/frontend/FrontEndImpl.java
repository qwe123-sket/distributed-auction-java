package frontend;

import common.Operation;
import common.OperationResult;
import io.grpc.Status;
import io.grpc.stub.StreamObserver;
import replica.AuctionItem;
import replica.ReplicatedAuction;

import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

public class FrontEndImpl extends AuctionServiceGrpc.AuctionServiceImplBase implements FrontEndAdmin {

    private final List<String> members = new CopyOnWriteArrayList<>();
    private volatile String sequencerName = null;

    private static final int MAX_LEADER_RETRIES = 16;

    @Override
    public String getCurrentSequencerName() {
        return sequencerName;
    }

    @Override
    public synchronized void registerReplica(int id, String rmiName) throws RemoteException {
        if (!members.contains(rmiName)) {
            members.add(rmiName);
        }
        if (sequencerName == null) {
            try {
                lookup(rmiName).setSequencer(true);
                sequencerName = rmiName;
            } catch (Exception e) {
                throw new RemoteException("failed to assign first sequencer", e);
            }
        }
        System.out.println("Registered replica " + rmiName + "; leader=" + sequencerName);
    }

    @Override
    public void getSpec(GetSpecRequest req, StreamObserver<Item> resp) {
        try {
            AuctionItem it = readWithFailover(L -> L.getSpec(req.getItemId()));
            if (it == null) {
                resp.onNext(Item.getDefaultInstance());
            } else {
                resp.onNext(toGrpcItem(it));
            }
            resp.onCompleted();
        } catch (Exception e) {
            resp.onError(Status.UNAVAILABLE.withDescription(e.getMessage()).asException());
        }
    }

    @Override
    public void listItems(Empty req, StreamObserver<ListReply> resp) {
        try {
            AuctionItem[] items = readWithFailover(ReplicatedAuction::listItems);
            ListReply.Builder lb = ListReply.newBuilder();
            for (AuctionItem it : items) {
                lb.addItems(toGrpcItem(it));
            }
            resp.onNext(lb.build());
            resp.onCompleted();
        } catch (Exception e) {
            resp.onError(Status.UNAVAILABLE.withDescription(e.getMessage()).asException());
        }
    }

    @Override
    public void register(RegisterRequest req, StreamObserver<RegisterReply> resp) {
        Operation op = Operation.register(req.getEmail());
        OperationResult r = mutateWithFailover(op, resp);
        if (r == null) {
            return;
        }
        if (!r.ok || r.userId == null) {
            resp.onError(Status.INTERNAL.withDescription(r.error != null ? r.error : "register failed").asException());
            return;
        }
        resp.onNext(RegisterReply.newBuilder().setUserId(r.userId).build());
        resp.onCompleted();
    }

    @Override
    public void newAuction(NewAuctionRequest req, StreamObserver<NewAuctionReply> resp) {
        Operation op = Operation.newAuction(
                req.getUserId(), req.getName(), req.getDescription(), req.getReservePrice());
        OperationResult r = mutateWithFailover(op, resp);
        if (r == null) {
            return;
        }
        if (!r.ok || r.itemId == null) {
            // Match Part 1 gRPC behaviour: invalid newAuction returns item_id -1 (not RPC error)
            int id = (r.itemId != null) ? r.itemId : -1;
            resp.onNext(NewAuctionReply.newBuilder().setItemId(id).build());
            resp.onCompleted();
            return;
        }
        resp.onNext(NewAuctionReply.newBuilder().setItemId(r.itemId).build());
        resp.onCompleted();
    }

    @Override
    public void bid(BidRequest req, StreamObserver<BidReply> resp) {
        Operation op = Operation.bid(req.getUserId(), req.getItemId(), req.getPrice());
        OperationResult r = mutateWithFailover(op, resp);
        if (r == null) {
            return;
        }
        if (!r.ok || r.bidOk == null) {
            resp.onError(Status.INTERNAL.withDescription(r.error != null ? r.error : "bid failed").asException());
            return;
        }
        resp.onNext(BidReply.newBuilder().setSuccess(r.bidOk).build());
        resp.onCompleted();
    }

    @Override
    public void closeAuction(CloseRequest req, StreamObserver<AuctionResult> resp) {
        Operation op = Operation.close(req.getUserId(), req.getItemId());
        OperationResult r = mutateWithFailover(op, resp);
        if (r == null) {
            return;
        }
        if (!r.ok) {
            resp.onNext(frontend.AuctionResult.getDefaultInstance());
            resp.onCompleted();
            return;
        }
        if (r.closeItem == null || r.closeWinner == null || r.closePrice == null) {
            resp.onNext(frontend.AuctionResult.getDefaultInstance());
            resp.onCompleted();
            return;
        }
        resp.onNext(frontend.AuctionResult.newBuilder()
                .setItemId(r.closeItem)
                .setWinningUser(r.closeWinner)
                .setPrice(r.closePrice)
                .build());
        resp.onCompleted();
    }

    private OperationResult mutateWithFailover(Operation op, StreamObserver<?> resp) {
        try {
            return mutateWithFailover(op);
        } catch (Exception e) {
            resp.onError(Status.UNAVAILABLE.withDescription(e.getMessage()).asException());
            return null;
        }
    }

    private OperationResult mutateWithFailover(Operation op) throws Exception {
        Exception last = null;
        for (int i = 0; i < MAX_LEADER_RETRIES; i++) {
            try {
                ReplicatedAuction leader = lookupLeader();
                List<String> snap = memberSnapshot();
                return leader.handleClientOperation(op, snap);
            } catch (Exception e) {
                last = e;
                electNewLeader();
            }
        }
        if (last != null) {
            throw last;
        }
        throw new IllegalStateException("mutation failed: no leader");
    }

    private <T> T readWithFailover(LeaderCall<T> call) throws Exception {
        Exception last = null;
        for (int i = 0; i < MAX_LEADER_RETRIES; i++) {
            try {
                ReplicatedAuction leader = lookupLeader();
                return call.apply(leader);
            } catch (Exception e) {
                last = e;
                electNewLeader();
            }
        }
        if (last != null) {
            throw last;
        }
        throw new IllegalStateException("read failed: no leader");
    }

    @FunctionalInterface
    private interface LeaderCall<T> {
        T apply(ReplicatedAuction leader) throws Exception;
    }

    private synchronized void electNewLeader() {
        String best = null;
        long bestSeq = -1;
        for (String m : members) {
            try {
                long c = lookup(m).getLastCommittedSeqNo();
                if (c > bestSeq) {
                    bestSeq = c;
                    best = m;
                }
            } catch (Exception ignored) {
            }
        }
        if (best == null) {
            sequencerName = null;
            System.out.println("Elected new sequencer: none (no reachable replica)");
            return;
        }
        for (String m : members) {
            try {
                lookup(m).setSequencer(m.equals(best));
            } catch (Exception ignored) {
            }
        }
        sequencerName = best;
        System.out.println("Elected new sequencer: " + sequencerName);
    }

    private List<String> memberSnapshot() {
        return new ArrayList<>(members);
    }

    private ReplicatedAuction lookup(String rmiName) throws Exception {
        Registry reg = LocateRegistry.getRegistry();
        return (ReplicatedAuction) reg.lookup(rmiName);
    }

    private ReplicatedAuction lookupLeader() throws Exception {
        if (sequencerName == null) {
            throw new IllegalStateException("No sequencer set");
        }
        return lookup(sequencerName);
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
