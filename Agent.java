
public class Agent {

    private final String agentId;
    private final String agentName;
    private CustomerQuery currentQuery;

    public Agent(String agentId, String agentName) {
        if (agentId == null || agentId.trim().isEmpty()) {
            throw new IllegalArgumentException(
                    "Agent ID cannot be empty.");
        }

        if (agentName == null || agentName.trim().isEmpty()) {
            throw new IllegalArgumentException(
                    "Agent name cannot be empty.");
        }

        this.agentId = agentId;
        this.agentName = agentName;
        this.currentQuery = null;
    }

    public String getAgentId() {
        return agentId;
    }

    public String getAgentName() {
        return agentName;
    }

    public CustomerQuery getCurrentQuery() {
        return currentQuery;
    }

    public boolean isAvailable() {
        return currentQuery == null;
    }

    public void assignQuery(CustomerQuery query) {
        if (query == null) {
            throw new IllegalArgumentException(
                    "Query cannot be null.");
        }

        if (!isAvailable()) {
            throw new IllegalStateException(
                    "Agent is already handling a query.");
        }

        currentQuery = query;
    }

    public void completeQuery() {
        currentQuery = null;
    }
}
