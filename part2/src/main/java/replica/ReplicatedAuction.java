package replica;

import java.rmi.Remote; 
import java.rmi.RemoteException;
import java.util.List;
import common.*;

public interface ReplicatedAuction extends Remote {
    // Called by the FrontEnd to execute a state-mutating client operation at the sequencer.
    OperationResult handleClientOperation(Operation op, List<String> memberList) throws RemoteException;

    // Called by the sequencer during the two-phase replication protocol.
    boolean propose(long seqNo, Operation op) throws RemoteException;
    boolean commitUpTo(long seqNo) throws RemoteException;   

    // Used by joining or recovering replicas to synchronise state.
    List<LogEntry> getEntriesAfter(long fromSeq) throws RemoteException;

    // Utility methods for leader detection and promotion.
    long getLastCommittedSeqNo() throws RemoteException;
    boolean isSequencer() throws RemoteException;
    void setSequencer(boolean isLeader) throws RemoteException;

    // Read-only (non-state-mutating) methods that front-end calls on the sequencer
    AuctionItem getSpec(int itemID) throws RemoteException;
    AuctionItem[] listItems() throws RemoteException;
}
