package server;

import java.rmi.RemoteException;
import java.rmi.server.UnicastRemoteObject;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

public class AuctionImpl extends UnicastRemoteObject implements Auction {
    private final Map<Integer, String> userEmails = new ConcurrentHashMap<>();
    private final AtomicInteger nextUserId = new AtomicInteger(1);

    private final Map<Integer, AuctionItem> items = new ConcurrentHashMap<>();
    private final Set<Integer> activeItemIds = ConcurrentHashMap.newKeySet();
    private final Map<Integer, Integer> itemOwner = new ConcurrentHashMap<>();
    private final Map<Integer, Integer> highestBidder = new ConcurrentHashMap<>();
    private final AtomicInteger nextItemId = new AtomicInteger(1);

    public AuctionImpl() throws RemoteException {
        super();
    }

    @Override
    public synchronized int register(String email) throws RemoteException {
        int uid = nextUserId.getAndIncrement();
        userEmails.put(uid, email);
        return uid;
    }

    @Override
    public synchronized int newAuction(int userID, AuctionSaleItem item) throws RemoteException {
        if (!userEmails.containsKey(userID)) {
            return -1;
        }
        int itemId = nextItemId.getAndIncrement();
        AuctionItem ai = new AuctionItem(itemId, item.name, item.description, item.reservePrice);
        items.put(itemId, ai);
        activeItemIds.add(itemId);
        itemOwner.put(itemId, userID);
        highestBidder.remove(itemId);
        return itemId;
    }

    @Override
    public synchronized AuctionItem getSpec(int itemID) throws RemoteException {
        return items.get(itemID);
    }

    @Override
    public synchronized AuctionItem[] listItems() throws RemoteException {
        List<AuctionItem> list = new ArrayList<>();
        for (int id : activeItemIds) {
            AuctionItem it = items.get(id);
            if (it != null) {
                list.add(it);
            }
        }
        return list.toArray(new AuctionItem[0]);
    }

    @Override
    public synchronized boolean bid(int userID, int itemID, int price) throws RemoteException {
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

    @Override
    public synchronized AuctionResult closeAuction(int userID, int itemID) throws RemoteException {
        AuctionItem it = items.get(itemID);
        if (it == null) {
            return null;
        }
        if (!activeItemIds.contains(itemID)) {
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
}
