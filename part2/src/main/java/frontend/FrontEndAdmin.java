package frontend;
import java.rmi.Remote;
import java.rmi.RemoteException;

//Feel free to add any other methods if you think is necessary
public interface FrontEndAdmin extends Remote {
    String getCurrentSequencerName() throws RemoteException;
    void registerReplica(int id, String rmiName) throws RemoteException;
}
