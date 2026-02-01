/**
 * Operating Systems Scheduling Simulator
 * CPCS361 Group Project
 * 
 * Team Members:
 * - Fatimah Saleh Baothman (2307298)
 * - Rafal Abdullah Riri (2308220) 
 * - Sidrah Faisal Alyamani (2311603)
 */
/**
 * Kernel services for memory and devices.
 * Handles pre-allocation and total-resource checks.
 */
package os.simulator;

public class OtherKerServices {
    private long memorySize;
    private long availableMemory;
    private int noDevs;
    private int availableDevices;

    public void initialize(long memory, int devices) {
        this.memorySize = memory;
        this.availableMemory = memory;
        this.noDevs = devices;
        this.availableDevices = devices;
    }

    // Check TOTAL system capacity (for rejection)
    public boolean canAllocate(long memory, int devices) {
        return memory <= memorySize && devices <= noDevs;
    }

    // Check CURRENT available resources (for admission/hold)
    public boolean hasAvailableResources(long memory, int devices) {
        return memory <= availableMemory && devices <= availableDevices;
    }

    public void allocateMemory(Process process) {
        if (process.getMemoryReq() <= availableMemory) {
            availableMemory -= process.getMemoryReq();
            process.setMemoryAllocated(true);
        }
    }

    public void deallocateMemory(Process process) {
        availableMemory += process.getMemoryReq();
        process.setMemoryAllocated(false);
    }

    public void reserveDevices(int count) {
        if (count <= availableDevices) {
            availableDevices -= count;
        }
    }

    public void releaseDevices(int count) {
        availableDevices += count;
    }

    // Getters
    public long getAvailableMemory() { return availableMemory; }
    public int getAvailableDevices() { return availableDevices; }
    public long getMemorySize() { return memorySize; }
    public int getNoDevs() { return noDevs; }
}
