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
 * Dynamic Round Robin Scheduler.
 * Time quantum = average remaining time of current + ready queue.
 */
package os.simulator;

public class DRoundRobinScheduler implements Scheduler {
    private int timeQuantum;
    private Process currentProcess;

    @Override
    public void setCurrentProcess(Process process) {
        this.currentProcess = process;
    }

    @Override
    public Process selectNextProcess(Queue readyQueue) {
        return readyQueue.dequeue();
    }

    @Override
    public int getTimeQuantum(Queue readyQueue) {
        long sr = 0; // Sum of remaining times
        int count = 0;

        if (currentProcess != null && currentProcess.getRemainingTime() > 0) {
            sr += currentProcess.getRemainingTime();
            count++;
        }

        for (Process process : readyQueue.getProcesses()) {
            sr += process.getRemainingTime();
            count++;
        }

        if (count == 0) {
            timeQuantum = 1;
            return timeQuantum;
        }

        long ar = sr / count;
        timeQuantum = (int) Math.max(1, ar);

        System.out.printf("Dynamic RR Calc: SR=%d, Count=%d, AR=%d, Quantum=%d%n",
                sr, count, ar, timeQuantum);

        return timeQuantum;
    }
}
