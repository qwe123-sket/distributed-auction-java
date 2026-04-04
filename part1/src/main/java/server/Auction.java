package server;

import java.rmi.Remote;
import java.rmi.RemoteException;

public interface Auction extends Remote {
    int register(String email) throws RemoteException;
    AuctionItem getSpec(int itemID) throws RemoteException;
    int newAuction(int userID, AuctionSaleItem item) throws RemoteException;
    AuctionItem[] listItems() throws RemoteException;
    AuctionResult closeAuction(int userID, int itemID) throws RemoteException;
    boolean bid(int userID, int itemID, int price) throws RemoteException;
}
