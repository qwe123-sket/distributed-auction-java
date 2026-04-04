package replica;

import frontend.FrontEndAdmin;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;

public class ReplicaMain {
    public static void main(String[] args) throws Exception {
        if (args.length < 1) {
            System.err.println("Usage: ReplicaMain <id>");
            System.exit(1);
        }
        int id = Integer.parseInt(args[0]);
        String name = "replica" + id;

        ReplicaImpl replica = new ReplicaImpl(id, name);
        Registry reg = LocateRegistry.getRegistry();
        reg.rebind(name, replica);
        System.out.println("Replica " + id + " bound as " + name);

        FrontEndAdmin fe = (FrontEndAdmin) reg.lookup("FrontEnd");
        String sequencer = fe.getCurrentSequencerName();
        if (sequencer != null && !sequencer.equals(name)) {
            ReplicatedAuction leader = (ReplicatedAuction) reg.lookup(sequencer);
            var history = leader.getEntriesAfter(0);
            replica.replayCommittedHistory(history);
            System.out.println("Synced " + history.size() + " committed entries from " + sequencer);
        }

        fe.registerReplica(id, name);
        System.out.println("Registered with FrontEnd as " + name);
    }
}
