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
 * Static Round Robin Scheduler.
 * Fixed time quantum = 10 + team number (team 8 → quantum = 18).
 */
package os.simulator;

public class SRoundRobinScheduler implements Scheduler {
    private int timeQuantum = 18; // 10 + team number 8
    private Process currentProcess;

    @Override
    public Process selectNextProcess(Queue readyQueue) {
        return readyQueue.dequeue();
    }

    @Override
    public int getTimeQuantum(Queue readyQueue) {
        return timeQuantum;
    }

    @Override
    public void setCurrentProcess(Process process) {
        this.currentProcess = process;
    }
}
