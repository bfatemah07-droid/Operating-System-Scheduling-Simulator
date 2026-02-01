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
 * SimulationController:
 * - Reads input file line by line (no pre-processing).
 * - Implements min(i,e) with internal events first when equal.
 * - Manages configs, arrivals, display events, and termination.
 */
package os.simulator;

import java.io.*;
import java.util.*;

public class SimulationController {
    private BufferedReader inputReader;
    private PrintWriter outputWriter;
    private PrManager processManager;
    private OtherKerServices kernelServices;
    private long currentTime;
    private boolean hasMoreEvents;
    private String nextEventLine;
    private long nextEventTime;
    private boolean simulationActive;
    private File inputFile;
    private File outputFile;

    public SimulationController(String inputFile, String outputFile) throws IOException {
        Locale.setDefault(Locale.US);
        this.inputFile = new File(inputFile);
        this.outputFile = new File(outputFile);
        this.inputReader = new BufferedReader(new FileReader(this.inputFile));
        this.outputWriter = new PrintWriter(new FileWriter(this.outputFile));
        this.kernelServices = new OtherKerServices();
        this.currentTime = 0;
        this.hasMoreEvents = true;
        this.simulationActive = false;
        readNextEvent();
    }

    // Read one event line and extract its time
   private void readNextEvent() throws IOException {
    nextEventLine = inputReader.readLine();

    // Skip ALL blank/whitespace-only lines (doctor format has many)
    while (nextEventLine != null && nextEventLine.trim().isEmpty()) {
        nextEventLine = inputReader.readLine();
    }

    // If we reached the end of file
    if (nextEventLine == null) {
        hasMoreEvents = false;
        nextEventTime = Long.MAX_VALUE;
        return;
    }

    // Extract event time normally
    String[] parts = nextEventLine.trim().split("\\s+");
    if (parts.length >= 2) {
        try {
            nextEventTime = Long.parseLong(parts[1]);
        } catch (NumberFormatException e) {
            nextEventTime = Long.MAX_VALUE;
        }
    } else {
        nextEventTime = Long.MAX_VALUE;
    }
}


    /**
     * Main simulation loop with proper min(i,e).
     */
    public void runSimulation() throws IOException {
        long maxSimulationTime = 1000000;
        int maxIterations = 10000;
        int iterationCount = 0;

        while ((hasMoreEvents
               || (simulationActive && processManager.hasInternalEvents())
               || (simulationActive && processManager.hasActiveProcesses()))
               && currentTime < maxSimulationTime
               && iterationCount < maxIterations) {

            iterationCount++;

            // Handle configuration (C)
            if (hasMoreEvents && nextEventLine != null && nextEventLine.startsWith("C")) {
                if (simulationActive) {
                    finishCurrentSimulation();
                }
                startNewSimulation();
                continue;
            }

            if (!simulationActive) {
                if (hasMoreEvents) {
                    readNextEvent();
                }
                continue;
            }

            long nextInternalTime = processManager.getNextDecisionTime();

            // min(i,e) logic
            if (nextInternalTime < nextEventTime) {
                // internal event first
                currentTime = nextInternalTime;
                processManager.cpuTimeAdvance(currentTime);
                processManager.handleInternalEvent();
            } else if (nextInternalTime > nextEventTime) {
                // external event
                currentTime = nextEventTime;
                processManager.cpuTimeAdvance(currentTime);
                processExternalEvent(nextEventLine);
                readNextEvent();
            } else {
                // equal: internal first if exists
                if (nextInternalTime != Long.MAX_VALUE) {
                    currentTime = nextInternalTime;
                    processManager.cpuTimeAdvance(currentTime);
                    processManager.handleInternalEvent();
                } else {
                    currentTime = nextEventTime;
                    processManager.cpuTimeAdvance(currentTime);
                    processExternalEvent(nextEventLine);
                    readNextEvent();
                }
            }

            // Always try dispatch after handling events
            processManager.dispatch();
        }

        if (simulationActive) {
            finishCurrentSimulation();
        }

        outputWriter.close();
        inputReader.close();
    }

    // Start simulation at a configuration line C t M= S= [SCHED=]
    private void startNewSimulation() throws IOException {
        String[] parts = nextEventLine.split("\\s+");
        long time = Long.parseLong(parts[1]);
        long memory = Long.parseLong(parts[2].split("=")[1]);
        int devices = Integer.parseInt(parts[3].split("=")[1]);

        Scheduler scheduler;
        if (parts.length > 4 && parts[4].startsWith("SCHED=")) {
            int schedulerType = Integer.parseInt(parts[4].split("=")[1]);
            if (schedulerType == 2) {
                scheduler = new SRoundRobinScheduler();
                outputWriter.printf("CONFIG at %.2f: mem=%d devices=%d scheduler=StaticRR%n%n",
                        (double) time, memory, devices);
            } else {
                scheduler = new DRoundRobinScheduler();
                outputWriter.printf("CONFIG at %.2f: mem=%d devices=%d scheduler=DynamicRR%n%n",
                        (double) time, memory, devices);
            }
        } else {
            scheduler = new DRoundRobinScheduler();
            outputWriter.printf("CONFIG at %.2f: mem=%d devices=%d scheduler=DynamicRR%n%n",
                    (double) time, memory, devices);
        }

        currentTime = time;
        kernelServices = new OtherKerServices();
        kernelServices.initialize(memory, devices);
        processManager = new PrManager(scheduler);
        processManager.initialize(kernelServices);
        simulationActive = true;

        readNextEvent();
    }

    /**
     * At the very end: just print the final line like in sample outputs.
     */
    private void finishCurrentSimulation() {
        outputWriter.printf("--- Simulation finished at time %.1f ---%n", (double) currentTime);
        simulationActive = false;
    }

    // Handle A & D external events
    private void processExternalEvent(String eventLine) {
        if (eventLine == null || eventLine.trim().isEmpty()) {
            return;
        }

        String[] parts = eventLine.split("\\s+");
        if (parts.length == 0) {
            return;
        }

        String eventType = parts[0];

        switch (eventType) {
            case "A":
                handleArrival(parts);
                break;
            case "D":
                handleDisplay(parts);
                break;
        }
    }

    private void handleArrival(String[] parts) {
        if (parts.length < 7) return;

        try {
            long PID = Long.parseLong(parts[2].split("=")[1]);
            long memoryReq = Long.parseLong(parts[3].split("=")[1]);
            int devReq = Integer.parseInt(parts[4].split("=")[1]);
            long burstTime = Long.parseLong(parts[5].split("=")[1]);
            int priority = Integer.parseInt(parts[6].split("=")[1]);

            processManager.procArrivingRoutine(currentTime, PID, memoryReq, devReq, burstTime, priority);
        } catch (Exception e) {
            System.err.println("Error parsing arrival: " + String.join(" ", parts));
        }
    }

    private void handleDisplay(String[] parts) {
        if (parts.length < 2) return;
        // NOTE: We do NOT print "<< At time ..." to match doctor output
        processManager.displaySystemState(outputWriter);
    }

    /**
     * Main: batch process all input*.txt in project folder.
     */
    public static void main(String[] args) {
        File projectDir = new File(System.getProperty("user.dir"));
        File[] inputFiles = projectDir.listFiles((dir, name) ->
                name.toLowerCase().startsWith("input") &&
                name.toLowerCase().endsWith(".txt")
        );

        if (inputFiles == null || inputFiles.length == 0) {
            System.out.println("No input files found in project directory.");
            return;
        }

        for (File inputFile : inputFiles) {
            String inputName = inputFile.getName();
            String outputName = inputName.replace("input", "output");
            try {
                new SimulationController(inputName, outputName).runSimulation();
                System.out.println("Generated: " + outputName);
            } catch (Exception e) {
                System.out.println("Failed on " + inputName + ": " + e.getMessage());
                e.printStackTrace();
            }
        }
    }
}
