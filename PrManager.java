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
/**
 * Process Manager.
 * Manages job admission, hold queues, ready queue, and CPU dispatching.
 */
package os.simulator;

import java.io.PrintWriter;
import java.util.*;

public class PrManager {
    private Queue readyQ;
    private Queue submitQ;
    private Queue holdQ1;
    private Queue holdQ2;

    private Process[] processTable;
    private OtherKerServices kernelServices;
    private Scheduler scheduler;
    private Process currentProcess;
    private long nextDecisionTime;
    private long internalClock;
    private long timeQuantumRemaining;

    // Exposed for simulation controller if needed
    public Queue getReadyQ() { return readyQ; }
    public Queue getSubmitQ() { return submitQ; }
    public Queue getHoldQ1() { return holdQ1; }
    public Queue getHoldQ2() { return holdQ2; }

    public PrManager() {
        this(new DRoundRobinScheduler());
    }

    public PrManager(Scheduler scheduler) {
        this.readyQ = new Queue("ReadyQ", false);
        this.submitQ = new Queue("SubmitQ", false);
        this.holdQ1 = new Queue("HoldQ1", true);   // HQ1 sorted by memory
        this.holdQ2 = new Queue("HoldQ2", false);  // HQ2 FIFO
        this.processTable = new Process[100];
        this.scheduler = scheduler;
        this.nextDecisionTime = Long.MAX_VALUE;
        this.internalClock = 0;
        this.timeQuantumRemaining = 0;
    }

    public void initialize(OtherKerServices kernelServices) {
        this.kernelServices = kernelServices;
    }

    /**
     * Handle new arriving job (A event).
     */
    public void procArrivingRoutine(long arrivalTime, long PID, long memoryReq,
                                    int devReq, long burstTime, int priority) {
        if (PID < 0 || PID >= 100) {
            System.err.println("ERROR: Invalid PID " + PID + ", must be between 0-99");
            return;
        }

        Process process = new Process(PID, arrivalTime, memoryReq, devReq, burstTime, priority);
        processTable[(int) PID] = process;
        getSubmitQ().enqueue(process);
        processJobAdmission();
    }

    /**
     * Job admission logic:
     * 1) Reject if more than TOTAL memory/devices.
     * 2) If cannot allocate NOW → send to HoldQ1/HoldQ2.
     * 3) If can allocate NOW → send to ReadyQ.
     */
    private void processJobAdmission() {
        List<Process> toRemove = new ArrayList<>();

        for (Process process : getSubmitQ().getProcesses()) {

            // Case 1: reject if exceeds TOTAL capacity
            if (!kernelServices.canAllocate(process.getMemoryReq(), process.getDevReq())) {
                process.setState(Process.TERMINATED);
                process.setCompletionTime(internalClock);
                process.setRejected(true);       // ****** FIX: mark rejected ******
                toRemove.add(process);

                System.out.printf("REJECTED: PID=%d exceeds total system resources%n",
                        process.getPID());
                continue;
            }

            // Case 2: not enough available → put in hold queues
            if (!kernelServices.hasAvailableResources(process.getMemoryReq(), process.getDevReq())) {
                if (process.getPriority() == 1) {
                    getHoldQ1().enqueue(process);
                } else {
                    getHoldQ2().enqueue(process);
                }
                process.setState(Process.HOLD);
                toRemove.add(process);
                continue;
            }

            // Case 3: admitted to ready queue immediately
            kernelServices.allocateMemory(process);
            kernelServices.reserveDevices(process.getDevReq());
            getReadyQ().enqueue(process);
            process.setState(Process.READY);
            toRemove.add(process);
        }

        for (Process p : toRemove) {
            getSubmitQ().removeProcess(p);
        }
    }

    // Time handling
    public void cpuTimeAdvance(long time) {
        if (time < internalClock) {
            time = internalClock;
        }
        internalClock = time;
    }

    public long getNextDecisionTime() {
        return nextDecisionTime;
    }

    public boolean hasInternalEvents() {
        return nextDecisionTime < Long.MAX_VALUE;
    }

    /**
     * Handle internal events: job completion or quantum expiration.
     */
    public void handleInternalEvent() {
        if (currentProcess != null) {
            long timeToUse = Math.min(timeQuantumRemaining, currentProcess.getRemainingTime());
            currentProcess.setRemainingTime(currentProcess.getRemainingTime() - timeToUse);
            timeQuantumRemaining -= timeToUse;

            if (currentProcess.getRemainingTime() <= 0) {
                handleJobCompletion();
            } else if (timeQuantumRemaining <= 0) {
                handleTimeQuantumExpiration();
            } else {
                nextDecisionTime = internalClock
                        + Math.min(currentProcess.getRemainingTime(), timeQuantumRemaining);
            }
        }

        promoteFromHoldQueues();
        dispatch();
    }

    private void handleJobCompletion() {
        if (currentProcess != null) {
            currentProcess.setCompletionTime(internalClock);
            currentProcess.setState(Process.TERMINATED);

            kernelServices.deallocateMemory(currentProcess);
            kernelServices.releaseDevices(currentProcess.getDevReq());

            currentProcess = null;
            timeQuantumRemaining = 0;

            promoteFromHoldQueues();
        }
    }

    private void handleTimeQuantumExpiration() {
        if (currentProcess != null && currentProcess.getRemainingTime() > 0) {
            currentProcess.setState(Process.READY);
            getReadyQ().enqueue(currentProcess);
            currentProcess = null;
            timeQuantumRemaining = 0;
            nextDecisionTime = Long.MAX_VALUE;
        }
    }

    /**
     * Dispatch next process according to scheduler.
     */
    public void dispatch() {
        if (currentProcess == null && !getReadyQ().isEmpty()) {
            currentProcess = scheduler.selectNextProcess(readyQ);
            if (currentProcess != null) {
                currentProcess.setState(Process.RUNNING);
                scheduler.setCurrentProcess(currentProcess);
                int tq = scheduler.getTimeQuantum(readyQ);
                timeQuantumRemaining = tq;
                nextDecisionTime = internalClock
                        + Math.min(currentProcess.getRemainingTime(), timeQuantumRemaining);
            }
        } else if (currentProcess != null && currentProcess.getRemainingTime() > 0) {
            nextDecisionTime = internalClock
                    + Math.min(currentProcess.getRemainingTime(), timeQuantumRemaining);
        } else {
            nextDecisionTime = Long.MAX_VALUE;
        }
    }

    /**
     * Promotion from HoldQ1 then HoldQ2 (HQ1 has higher priority).
     */
    private void promoteFromHoldQueues() {
        boolean promoted;
        do {
            promoted = false;
            promoted = promoteFromHoldQueue(getHoldQ1());
            if (promoted) continue;
            promoted = promoteFromHoldQueue(getHoldQ2());
        } while (promoted);
    }

    private boolean promoteFromHoldQueue(Queue holdQueue) {
        List<Process> promoted = new ArrayList<>();
        List<Process> copy = new ArrayList<>(holdQueue.getProcesses());

        for (Process process : copy) {
            if (kernelServices.hasAvailableResources(process.getMemoryReq(), process.getDevReq())) {
                kernelServices.allocateMemory(process);
                kernelServices.reserveDevices(process.getDevReq());
                getReadyQ().enqueue(process);
                process.setState(Process.READY);
                promoted.add(process);
                break;
            }
        }

        for (Process p : promoted) {
            holdQueue.removeProcess(p);
        }

        return !promoted.isEmpty();
    }

    /**
     * Print system state exactly like required output.
     */
    public void displaySystemState(PrintWriter output) {
    output.println("-------------------------------------------------------");
    output.println("System Status:");
    output.println("-------------------------------------------------------");
    output.printf("          Time: %.2f%n", (double) internalClock);
    output.printf("  Total Memory: %d%n", kernelServices.getMemorySize());
    output.printf(" Avail. Memory: %d%n", kernelServices.getAvailableMemory());
    output.printf(" Total Devices: %d%n", kernelServices.getNoDevs());
    output.printf("Avail. Devices: %d%n", kernelServices.getAvailableDevices());
    output.println();

    // Ready Queue
    displayQueue(output, "Jobs in Ready List", getReadyQ());

    // Long Job List = submitQ (instead of always printing EMPTY)
    displayQueue(output, "Jobs in Long Job List", getSubmitQ());

    // Hold Queues
    displayQueue(output, "Jobs in Hold List 1", getHoldQ1());
    displayQueue(output, "Jobs in Hold List 2", getHoldQ2());

    // dump the full process table (all jobs + state)
    displayProcessTable(output);

    // Finished jobs + system-level statistics (handled below)
    displayFinishedJobs(output);
}

    private void displayQueue(PrintWriter output, String queueName, Queue queue) {
        output.println(queueName);
        output.println("--------------------------------------------------------");
        if (queue.isEmpty()) {
            output.println("  EMPTY");
        } else {
            for (Process process : queue.getProcesses()) {
                output.printf("Job ID %d , %.2f Cycles left to completion.%n",
                        process.getPID(), (double) process.getRemainingTime());
            }
        }
        output.println();
    }
    
    /**
 * Dumps the process table: every job that has entered the system with its state.
 */
private void displayProcessTable(PrintWriter output) {
    output.println("Process Table (All Jobs)");
    output.println("--------------------------------------------------------");
    output.println(" PID   State        Arrival     Complete     Remaining");
    output.println("--------------------------------------------------------");

    boolean any = false;

    for (int i = 0; i < processTable.length; i++) {
        Process process = processTable[i];
        if (process == null) continue;

        any = true;

        String stateStr;
        switch (process.getState()) {
            case Process.READY:
                stateStr = "READY";
                break;
            case Process.RUNNING:
                stateStr = "RUNNING";
                break;
            case Process.HOLD:
                stateStr = "HOLD";
                break;
            case Process.TERMINATED:
                stateStr = "TERMINATED";
                break;
            default:
                stateStr = "UNKNOWN";
        }

        double arrival   = (double) process.getArrivalTime();
        double complete  = (double) process.getCompletionTime();
        double remaining = (double) process.getRemainingTime();

        output.printf(" %-4d %-11s %9.2f %11.2f %11.2f%n",
                process.getPID(),
                stateStr,
                arrival,
                complete,
                remaining);
    }

    if (!any) {
        output.println("  EMPTY");
    }

    output.println();
}

    /**
     * Finished jobs table – excluding rejected jobs.
     */
    /**
 * Finished jobs table – excluding rejected jobs.
 * Also prints system-level statistics (average turnaround & waiting time).
 */
private void displayFinishedJobs(PrintWriter output) {
    output.println("Finished Jobs (detailed)");
    output.println("--------------------------------------------------------");
    output.println("  Job    ArrivalTime     CompleteTime     TurnaroundTime    WaitingTime");
    output.println("------------------------------------------------------------------------");

    int completedCount = 0;
    long totalTurnaround = 0;
    long totalWaiting = 0;

    for (int i = 0; i < processTable.length; i++) {
        Process process = processTable[i];

        if (process != null
                && process.getState() == Process.TERMINATED
                && !process.isRejected()
                && process.getCompletionTime() >= process.getArrivalTime()) {

            long turnaround = process.getCompletionTime() - process.getArrivalTime();
            long waiting = turnaround - process.getBurstTime();
            if (waiting < 0) waiting = 0;

            totalTurnaround += turnaround;
            totalWaiting += waiting;
            completedCount++;

            output.printf("  %-3d   %11.2f   %13.2f   %15.2f   %12.2f%n",
                    process.getPID(),
                    (double) process.getArrivalTime(),
                    (double) process.getCompletionTime(),
                    (double) turnaround,
                    (double) waiting);
        }
    }

    if (completedCount == 0) {
        output.println("  EMPTY");
        output.println();
        output.println();
    } else {
        output.println("------------------------------------------------------------------------");
        output.printf("Total Finished Jobs:             %d%n", completedCount);

        double avgTurnaround = (double) totalTurnaround / completedCount;
        double avgWaiting = (double) totalWaiting / completedCount;

        // System-level statistics (you can rename labels as your doctor prefers)
        output.printf("Average Turnaround Time:     %.2f%n", avgTurnaround);
        output.printf("Average Waiting Time:        %.2f%n", avgWaiting);

        output.println();
        output.println();
    }
}

    public boolean hasActiveProcesses() {
        if (currentProcess != null && currentProcess.getRemainingTime() > 0) return true;
        if (!getReadyQ().isEmpty()) return true;
        if (!getHoldQ1().isEmpty()) return true;
        if (!getHoldQ2().isEmpty()) return true;
        if (!getSubmitQ().isEmpty()) return true;
        return false;
    }
}
