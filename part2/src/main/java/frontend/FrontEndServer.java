package frontend;

import io.grpc.Server;
import io.grpc.ServerBuilder;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.rmi.server.UnicastRemoteObject;

// No need to make any changes to this file
// It registers the FrontEnd with rmiregistry and starts the gRPC service
public class FrontEndServer {
    public static void main(String[] args) throws Exception {
        FrontEndImpl svc = new FrontEndImpl();
        Registry reg = LocateRegistry.getRegistry();

        FrontEndAdmin stub = (FrontEndAdmin) UnicastRemoteObject.exportObject(svc, 0);
        reg.rebind("FrontEnd", stub);
        System.out.println("FrontEndAdmin bound as 'FrontEnd'");

        // Start gRPC (do not change the port number, keep it 50055)
        Server s = ServerBuilder.forPort(50055).addService(svc).build();
        s.start();
        System.out.println("FrontEnd started (RMI: FrontEnd, gRPC:50055)");
        s.awaitTermination();
    }
}
