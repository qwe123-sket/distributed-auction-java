package frontend;

import io.grpc.Server;
import io.grpc.ServerBuilder;

// No need to modify this file - it start the front-end on port 50055
public class FrontEndServer {
    public static void main(String[] args) throws Exception {
        FrontEndImpl service = new FrontEndImpl();
        Server server = ServerBuilder.forPort(50055).addService(service).build();
        server.start();
        System.out.println("FrontEnd gRPC server started on port 50055");
        server.awaitTermination();
    }
}

