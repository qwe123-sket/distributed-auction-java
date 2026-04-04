// server/ServerMain.java
package server;

import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;

//No need to modify this file - it start the AuctionServer and registers with RMI

public class ServerMain {
    public static void main(String[] args) throws Exception {
        AuctionImpl server = new AuctionImpl();
        Registry reg = LocateRegistry.getRegistry(); 
        reg.rebind("AuctionServer", server);
        System.out.println("AuctionServer bound as 'AuctionServer'");
        Thread.currentThread().join();
    }
}
