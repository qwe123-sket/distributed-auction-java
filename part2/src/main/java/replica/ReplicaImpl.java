package replica;

import common.*;
import java.rmi.server.UnicastRemoteObject;
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.util.*;

public class ReplicaImpl extends UnicastRemoteObject implements ReplicatedAuction {

    private boolean isLeader = false;
    private long lastSeqAssigned = 0;
    private long lastCommitted = 0;
    private long lastApplied = 0;
    private final TreeMap<Long, LogEntry> log = new TreeMap<>();

    private final Map<Integer, String> userEmails = new HashMap<>();
    private int nextUserId = 1;

    private final Map<Integer, AuctionItem> items = new HashMap<>();
    private final Set<Integer> activeItemIds = new HashSet<>();
    private final Map<Integer, Integer> itemOwner = new HashMap<>();
    private final Map<Integer, Integer> highestBidder = new HashMap<>();
    private int nextItemId = 1;

    private final int id;
    private final String myName;

    public ReplicaImpl(int id, String rmiName) throws RemoteException {
        super();
        this.id = id;
        this.myName = rmiName;
    }

    @Override
    public synchronized AuctionItem getSpec(int itemID) {
        return items.get(itemID);
    }

    @Override
    public synchronized AuctionItem[] listItems() {
        List<AuctionItem> list = new ArrayList<>();
        for (int itemId : activeItemIds) {
            AuctionItem it = items.get(itemId);
            if (it != null) {
                list.add(it);
            }
        }
        return list.toArray(new AuctionItem[0]);
    }

    private int newAuction(int userID, AuctionSaleItem item) {
        if (!userEmails.containsKey(userID)) {
            return -1;
        }
        int itemId = nextItemId++;
        AuctionItem ai = new AuctionItem(itemId, item.name, item.description, item.reservePrice);
        items.put(itemId, ai);
        activeItemIds.add(itemId);
        itemOwner.put(itemId, userID);
        highestBidder.remove(itemId);
        return itemId;
    }

    private AuctionResult closeAuction(int userID, int itemID) {
        AuctionItem it = items.get(itemID);
        if (it == null || !activeItemIds.contains(itemID)) {
            return null;
        }
        Integer owner = itemOwner.get(itemID);
        if (owner == null || owner != userID) {
            return null;
        }
        activeItemIds.remove(itemID);
        int winner = 0;
        int price = 0;
        if (it.highestBid >= it.reservePrice) {
            Integer w = highestBidder.get(itemID);
            if (w != null) {
                winner = w;
                price = it.highestBid;
            }
        }
        return new AuctionResult(itemID, winner, price);
    }

    private boolean bid(int userID, int itemID, int price) {
        if (!userEmails.containsKey(userID)) {
            return false;
        }
        if (!activeItemIds.contains(itemID)) {
            return false;
        }
        AuctionItem it = items.get(itemID);
        if (it == null) {
            return false;
        }
        if (price <= it.highestBid || price < it.reservePrice) {
            return false;
        }
        it.highestBid = price;
        highestBidder.put(itemID, userID);
        return true;
    }

    private int register(String email) {
        int uid = nextUserId++;
        userEmails.put(uid, email);
        return uid;
    }

    @Override
    public synchronized OperationResult handleClientOperation(Operation op, List<String> memberList)
            throws RemoteException {
        if (!isLeader) {
            return OperationResult.fail("Not leader");
        }
        if (memberList == null || memberList.isEmpty()) {
            return OperationResult.fail("empty membership");
        }

        long seqNo = ++lastSeqAssigned;

        int acks = 0;
        for (String member : memberList) {
            try {
                boolean ok;
                if (member.equals(myName)) {
                    ok = propose(seqNo, op);
                } else {
                    ok = lookup(member).propose(seqNo, op);
                }
                if (ok) {
                    acks++;
                }
            } catch (Exception e) {
                // unreachable replica: no ack
            }
        }

        if (acks < majority(memberList.size())) {
            return OperationResult.fail("quorum not reached");
        }

        OperationResult clientResult = OperationResult.fail("commit failed");
        for (String member : memberList) {
            try {
                if (member.equals(myName)) {
                    clientResult = commitUpToLocal(seqNo);
                } else {
                    lookup(member).commitUpTo(seqNo);
                }
            } catch (Exception e) {
                // best-effort replication; client result still from local commit if self already done
            }
        }
        return clientResult;
    }

    private int majority(int n) {
        return (n / 2) + 1;
    }

    @Override
    public synchronized boolean propose(long seqNo, Operation op) {
        LogEntry existing = log.get(seqNo);
        if (existing == null) {
            log.put(seqNo, new LogEntry(seqNo, op));
        }
        return true;
    }

    @Override
    public synchronized boolean commitUpTo(long seqNo) {
        commitUpToLocal(seqNo);
        return true;
    }

    private OperationResult commitUpToLocal(long seqNo) {
        try {
            fillGapsUpTo(seqNo);
        } catch (Exception e) {
            return OperationResult.fail("gap fill failed: " + e.getMessage());
        }

        OperationResult resultAtSeq = OperationResult.fail("missing log entry");
        for (long s = lastApplied + 1; s <= seqNo; s++) {
            LogEntry e = log.get(s);
            if (e == null) {
                return OperationResult.fail("missing log at " + s);
            }
            e.committed = true;
            if (s > lastCommitted) {
                lastCommitted = s;
            }
            OperationResult r = apply(e.op);
            lastApplied = s;
            if (s == seqNo) {
                resultAtSeq = r;
            }
        }
        return resultAtSeq;
    }

    private void fillGapsUpTo(long seqNo) throws Exception {
        while (true) {
            long missing = -1;
            for (long s = 1; s <= seqNo; s++) {
                if (!log.containsKey(s)) {
                    missing = s;
                    break;
                }
            }
            if (missing < 0) {
                return;
            }
            String leaderName = findLeaderName();
            if (leaderName == null || leaderName.equals(myName)) {
                throw new IllegalStateException("missing seq " + missing + " but no remote leader");
            }
            ReplicatedAuction leader = lookup(leaderName);
            List<LogEntry> batch = leader.getEntriesAfter(missing - 1);
            if (batch.isEmpty()) {
                throw new IllegalStateException("leader has no entries after " + (missing - 1));
            }
            for (LogEntry le : batch) {
                if (le.seqNo > seqNo) {
                    break;
                }
                if (!log.containsKey(le.seqNo)) {
                    LogEntry copy = new LogEntry(le.seqNo, le.op);
                    copy.committed = le.committed;
                    log.put(le.seqNo, copy);
                }
            }
        }
    }

    private OperationResult apply(Operation op) {
        switch (op.type) {
            case REGISTER -> {
                int uid = register(op.email);
                return OperationResult.reg(uid);
            }
            case NEW_AUCTION -> {
                int iid = newAuction(op.userId, new AuctionSaleItem(op.name, op.description, op.reservePrice));
                return (iid > 0) ? OperationResult.newA(iid) : OperationResult.fail("unknown user");
            }
            case BID -> {
                boolean ok = bid(op.userId, op.itemId, op.reservePrice);
                return OperationResult.bid(ok);
            }
            case CLOSE -> {
                AuctionResult ar = closeAuction(op.userId, op.itemId);
                if (ar == null) {
                    return OperationResult.fail("Auction close not permitted!");
                }
                return OperationResult.close(ar.itemID, ar.winningUser, ar.price);
            }
            default -> {
                return OperationResult.fail("Unknown op");
            }
        }
    }

    @Override
    public synchronized List<LogEntry> getEntriesAfter(long fromSeq) {
        List<LogEntry> out = new ArrayList<>();
        for (var e : log.tailMap(fromSeq + 1).entrySet()) {
            LogEntry le = e.getValue();
            if (le.committed) {
                LogEntry copy = new LogEntry(le.seqNo, le.op);
                copy.committed = true;
                out.add(copy);
            }
        }
        return out;
    }

    /**
     * Apply committed history from the sequencer (join / catch-up before registerReplica).
     */
    public synchronized void replayCommittedHistory(List<LogEntry> entries) {
        if (entries == null || entries.isEmpty()) {
            return;
        }
        List<LogEntry> sorted = new ArrayList<>(entries);
        sorted.sort(Comparator.comparingLong(e -> e.seqNo));
        for (LogEntry le : sorted) {
            LogEntry copy = new LogEntry(le.seqNo, le.op);
            copy.committed = true;
            log.put(le.seqNo, copy);
        }
        for (LogEntry le : sorted) {
            if (le.seqNo <= lastApplied) {
                continue;
            }
            OperationResult unused = apply(le.op);
            lastApplied = le.seqNo;
            lastCommitted = Math.max(lastCommitted, le.seqNo);
        }
    }

    private String findLeaderName() throws Exception {
        Registry reg = LocateRegistry.getRegistry();
        frontend.FrontEndAdmin fe = (frontend.FrontEndAdmin) reg.lookup("FrontEnd");
        return fe.getCurrentSequencerName();
    }

    private ReplicatedAuction lookup(String rmiName) throws Exception {
        Registry reg = LocateRegistry.getRegistry();
        return (ReplicatedAuction) reg.lookup(rmiName);
    }

    @Override
    public synchronized long getLastCommittedSeqNo() {
        return lastCommitted;
    }

    @Override
    public synchronized boolean isSequencer() {
        return isLeader;
    }

    @Override
    public synchronized void setSequencer(boolean leader) {
        this.isLeader = leader;
        if (leader) {
            this.lastSeqAssigned = this.lastCommitted;
        }
    }

    public String getMyName() {
        return myName;
    }

    public int getReplicaId() {
        return id;
    }
}
