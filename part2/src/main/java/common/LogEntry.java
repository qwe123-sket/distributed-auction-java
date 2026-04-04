package common;
import java.io.Serializable;

public class LogEntry implements Serializable {
    public final long seqNo;
    public final Operation op; 
    public boolean committed; // is the operation committed?
    public LogEntry(long s, Operation o) { 
        this.seqNo=s; 
        this.op=o; 
        this.committed=false; 
    }
}
