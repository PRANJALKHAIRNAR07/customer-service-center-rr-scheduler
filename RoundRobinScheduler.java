
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Queue;

public class RoundRobinScheduler {

    private final Queue<CustomerQuery> waitingQueue =
            new ArrayDeque<>();

    private final List<CustomerQuery> allQueries =
            new ArrayList<>();

    private final List<Agent> agents =
            new ArrayList<>();

    private final List<String> executionLog =
            new ArrayList<>();

    private final Map<String, Integer> arrivalTimes =
            new HashMap<>();

    private final Map<String, Integer> completionTimes =
            new HashMap<>();

    private final Map<Agent, Integer> sliceEndTimes =
            new HashMap<>();

    private final Map<Agent, Integer> sliceDurations =
            new HashMap<>();

    private final Map<Agent, Integer> sliceStartTimes =
            new HashMap<>();

    private int timeQuantum;
    private int currentTime;
    private boolean simulationStarted;

    public RoundRobinScheduler(int timeQuantum) {
        setTimeQuantum(timeQuantum);
    }

    public void setTimeQuantum(int timeQuantum) {
        if (timeQuantum <= 0) {
            throw new IllegalArgumentException(
                    "Time quantum must be greater than zero.");
        }

        if (simulationStarted) {
            throw new IllegalStateException(
                    "Cannot change the time quantum after scheduling starts.");
        }

        this.timeQuantum = timeQuantum;
    }

    public int getTimeQuantum() {
        return timeQuantum;
    }

    public int getCurrentTime() {
        return currentTime;
    }

    public void addAgent(Agent agent) {
        if (agent == null) {
            throw new IllegalArgumentException(
                    "Agent cannot be null.");
        }

        if (simulationStarted) {
            throw new IllegalStateException(
                    "Cannot add agents after scheduling starts.");
        }

        for (Agent existing : agents) {
            if (existing.getAgentId().equals(agent.getAgentId())) {
                throw new IllegalArgumentException(
                        "Agent ID already exists.");
            }
        }

        agents.add(agent);
    }

    public List<Agent> getAgents() {
        return new ArrayList<>(agents);
    }

    public List<CustomerQuery> getQueries() {
        return new ArrayList<>(allQueries);
    }

    public List<String> getExecutionLog() {
        return new ArrayList<>(executionLog);
    }

    public boolean hasWaitingQueries() {
        return !waitingQueue.isEmpty();
    }

    public int getWaitingCount() {
        return waitingQueue.size();
    }

    public int getResolvedCount() {
        int count = 0;

        for (CustomerQuery query : allQueries) {
            if ("Resolved".equals(query.getStatus())) {
                count++;
            }
        }

        return count;
    }

    public void addQuery(CustomerQuery query) {
        if (query == null) {
            throw new IllegalArgumentException(
                    "Query cannot be null.");
        }

        if (query.getBurstTime() <= 0) {
            throw new IllegalArgumentException(
                    "Query handling time must be positive.");
        }

        if (simulationStarted) {
            throw new IllegalStateException(
                    "Cannot add queries after scheduling starts.");
        }

        for (CustomerQuery existing : allQueries) {
            if (existing.getQueryId().equals(query.getQueryId())) {
                throw new IllegalArgumentException(
                        "Query already exists.");
            }
        }

        allQueries.add(query);
        arrivalTimes.put(query.getQueryId(), currentTime);
        waitingQueue.offer(query);
    }

    public void reset() {
        waitingQueue.clear();
        allQueries.clear();
        executionLog.clear();

        arrivalTimes.clear();
        completionTimes.clear();

        sliceEndTimes.clear();
        sliceDurations.clear();
        sliceStartTimes.clear();

        currentTime = 0;
        simulationStarted = false;

        for (Agent agent : agents) {
            agent.completeQuery();
        }

        agents.clear();
    }

    public void runSimulation() {
        if (agents.isEmpty()) {
            throw new IllegalStateException(
                    "Add at least one agent.");
        }

        if (allQueries.isEmpty()) {
            throw new IllegalStateException(
                    "Add at least one query.");
        }

        if (simulationStarted) {
            throw new IllegalStateException(
                    "Create a fresh scheduler before running again.");
        }

        simulationStarted = true;

        while (!waitingQueue.isEmpty() || hasBusyAgents()) {

            // Assign waiting queries to available agents.
            for (Agent agent : agents) {
                if (!agent.isAvailable() || waitingQueue.isEmpty()) {
                    continue;
                }

                CustomerQuery query = waitingQueue.poll();

                int duration = Math.min(
                        timeQuantum,
                        query.getRemainingTime());

                agent.assignQuery(query);

                query.setAssignedAgent(agent.getAgentId());
                query.setStatus("In Progress");

                sliceStartTimes.put(agent, currentTime);
                sliceDurations.put(agent, duration);
                sliceEndTimes.put(
                        agent,
                        currentTime + duration);
            }

            if (!hasBusyAgents()) {
                break;
            }

            // Advance time to the next completion event.
            int nextTime = Integer.MAX_VALUE;

            for (Agent agent : agents) {
                if (!agent.isAvailable()) {
                    nextTime = Math.min(
                            nextTime,
                            sliceEndTimes.get(agent));
                }
            }

            currentTime = nextTime;

            // Process every slice ending at this time.
            // Agent-list order breaks simultaneous-event ties.
            for (Agent agent : agents) {
                if (agent.isAvailable()
                        || sliceEndTimes.get(agent) != currentTime) {
                    continue;
                }

                CustomerQuery query = agent.getCurrentQuery();

                int duration = sliceDurations.get(agent);
                int start = sliceStartTimes.get(agent);

                query.setRemainingTime(
                        query.getRemainingTime() - duration);

                executionLog.add(
                        "Time " + start + " - " + currentTime
                        + ": " + query.getQueryId()
                        + " handled by " + agent.getAgentId()
                        + " (" + duration + " min)");

                if (query.getRemainingTime() == 0) {
                    query.setStatus("Resolved");
                    completionTimes.put(
                            query.getQueryId(),
                            currentTime);
                } else {
                    query.setStatus("Waiting");
                    waitingQueue.offer(query);
                }

                agent.completeQuery();

                sliceEndTimes.remove(agent);
                sliceDurations.remove(agent);
                sliceStartTimes.remove(agent);
            }
        }

        // Chronological display order: start time, then end time.
        executionLog.sort(new Comparator<String>() {
            @Override
            public int compare(String a, String b) {
                int startA = extractTime(a, "Time ", " - ");
                int startB = extractTime(b, "Time ", " - ");

                int comparison = Integer.compare(startA, startB);

                if (comparison != 0) {
                    return comparison;
                }

                int endA = extractTime(a, " - ", ":");
                int endB = extractTime(b, " - ", ":");

                return Integer.compare(endA, endB);
            }
        });
    }

    private int extractTime(
            String entry,
            String prefix,
            String suffix) {

        int start = entry.indexOf(prefix) + prefix.length();
        int end = entry.indexOf(suffix, start);

        return Integer.parseInt(entry.substring(start, end));
    }

    private boolean hasBusyAgents() {
        for (Agent agent : agents) {
            if (!agent.isAvailable()) {
                return true;
            }
        }

        return false;
    }

    public int getCompletionTime(CustomerQuery query) {
        Integer value = completionTimes.get(query.getQueryId());
        return value == null ? -1 : value;
    }

    public int getTurnaroundTime(CustomerQuery query) {
        Integer completion = completionTimes.get(query.getQueryId());

        if (completion == null) {
            return -1;
        }

        return completion - arrivalTimes.get(query.getQueryId());
    }

    public int getWaitingTime(CustomerQuery query) {
        int turnaround = getTurnaroundTime(query);

        if (turnaround < 0) {
            return -1;
        }

        return turnaround - query.getBurstTime();
    }

    public double getAverageWaitingTime() {
        if (completionTimes.isEmpty()) {
            return 0.0;
        }

        double total = 0;

        for (CustomerQuery query : allQueries) {
            if (completionTimes.containsKey(query.getQueryId())) {
                total += getWaitingTime(query);
            }
        }

        return total / completionTimes.size();
    }

    public double getAverageTurnaroundTime() {
        if (completionTimes.isEmpty()) {
            return 0.0;
        }

        double total = 0;

        for (CustomerQuery query : allQueries) {
            if (completionTimes.containsKey(query.getQueryId())) {
                total += getTurnaroundTime(query);
            }
        }

        return total / completionTimes.size();
    }

    public Map<String, Integer> getCompletionTimes() {
        return new LinkedHashMap<>(completionTimes);
    }
}
