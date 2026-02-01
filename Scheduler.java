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
 * Scheduler interface for all CPU scheduling algorithms.
 */
package os.simulator;

public interface Scheduler {
    Process selectNextProcess(Queue readyQueue);
    int getTimeQuantum(Queue readyQueue);
    void setCurrentProcess(Process process);
}
