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

import java.util.*;

public class Queue {
    private String name;
    private LinkedList<Process> processes;
    private boolean sortedByMemory;

    public Queue(String name, boolean sortedByMemory) {
        this.name = name;
        this.processes = new LinkedList<>();
        this.sortedByMemory = sortedByMemory;
    }

    // Enqueue with optional HQ1 sorting
    public void enqueue(Process process) {
        processes.add(process);
        if (sortedByMemory) {
            sortByMemory();
        }
    }

    public Process dequeue() {
        if (processes.isEmpty()) {
            return null;
        }
        return processes.removeFirst();
    }

    public void removeProcess(Process process) {
        processes.remove(process);
    }

    public boolean isEmpty() {
        return processes.isEmpty();
    }

    public List<Process> getProcesses() {
        return new ArrayList<>(processes);
    }

    // HQ1: sort ascending by memory, break ties by arrival (FIFO)
    private void sortByMemory() {
        Collections.sort(processes, (p1, p2) -> {
            int memoryCompare = Long.compare(p1.getMemoryReq(), p2.getMemoryReq());
            if (memoryCompare != 0) {
                return memoryCompare;
            }
            return Long.compare(p1.getArrivalTime(), p2.getArrivalTime());
        });
    }

    public String getName() {
        return name;
    }
}
