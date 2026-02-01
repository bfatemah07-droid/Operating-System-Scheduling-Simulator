/**
 * Operating Systems Scheduling Simulator
 * CPCS361 Group Project
 * 
 * Team Members:
 * - Fatimah Saleh Baothman (2307298)
 * - Rafal Abdullah Riri (2308220) 
 * - Sidrah Faisal Alyamani (2311603)
 * 
 */

package os.simulator;

public class Process {
    // Process states
    public static final int READY = 0;
    public static final int RUNNING = 1;
    public static final int HOLD = 2;
    public static final int TERMINATED = 3;

    private long PID;
    private long arrivalTime;
    private long completionTime;
    private long memoryReq;
    private int devReq;
    private long burstTime;
    private long remainingTime;
    private int priority;
    private boolean memoryAllocated;
    private int state;
    private long waitingTime;

    // NEW: flag to mark rejected jobs
    private boolean rejected;

    public Process(long PID, long arrivalTime, long memoryReq, int devReq,
                   long burstTime, int priority) {
        this.PID = PID;
        this.arrivalTime = arrivalTime;
        this.memoryReq = memoryReq;
        this.devReq = devReq;
        this.burstTime = burstTime;
        this.remainingTime = burstTime;
        this.priority = priority;
        this.state = HOLD; // start in hold
        this.memoryAllocated = false;
        this.waitingTime = 0;
        this.rejected = false; // by default, not rejected
    }

    // Getters
    public long getPID() { return PID; }
    public long getArrivalTime() { return arrivalTime; }
    public long getCompletionTime() { return completionTime; }
    public long getMemoryReq() { return memoryReq; }
    public int getDevReq() { return devReq; }
    public long getBurstTime() { return burstTime; }
    public long getRemainingTime() { return remainingTime; }
    public int getPriority() { return priority; }
    public boolean isMemoryAllocated() { return memoryAllocated; }
    public int getState() { return state; }
    public long getWaitingTime() { return waitingTime; }

    // NEW: rejected flag getter
    public boolean isRejected() { return rejected; }

    // Setters
    public void setCompletionTime(long time) { this.completionTime = time; }
    public void setRemainingTime(long time) { this.remainingTime = time; }
    public void setMemoryAllocated(boolean allocated) { this.memoryAllocated = allocated; }
    public void setState(int state) { this.state = state; }
    public void setWaitingTime(long waitingTime) { this.waitingTime = waitingTime; }

    // NEW: mark job as rejected (never admitted to system)
    public void setRejected(boolean rejected) { this.rejected = rejected; }
}
