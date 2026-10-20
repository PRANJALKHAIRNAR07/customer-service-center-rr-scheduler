
public class CustomerQuery {

    private static int nextId = 101;

    private final String queryId;
    private final String customerName;
    private final String issueType;
    private final String description;
    private final int burstTime;

    private int remainingTime;
    private String status;
    private String assignedAgent;

    public CustomerQuery(
            String customerName,
            String issueType,
            String description,
            int burstTime) {

        if (customerName == null || customerName.trim().isEmpty()) {
            throw new IllegalArgumentException(
                    "Customer name cannot be empty.");
        }

        if (issueType == null || issueType.trim().isEmpty()) {
            throw new IllegalArgumentException(
                    "Issue type cannot be empty.");
        }

        if (description == null || description.trim().isEmpty()) {
            throw new IllegalArgumentException(
                    "Description cannot be empty.");
        }

        if (burstTime <= 0) {
            throw new IllegalArgumentException(
                    "Handling time must be greater than zero.");
        }

        this.queryId = "Q" + nextId++;
        this.customerName = customerName.trim();
        this.issueType = issueType;
        this.description = description.trim();
        this.burstTime = burstTime;

        reset();
    }

    public String getQueryId() {
        return queryId;
    }

    public String getCustomerName() {
        return customerName;
    }

    public String getIssueType() {
        return issueType;
    }

    public String getDescription() {
        return description;
    }

    public int getBurstTime() {
        return burstTime;
    }

    public int getRemainingTime() {
        return remainingTime;
    }

    public void setRemainingTime(int remainingTime) {
        if (remainingTime < 0 || remainingTime > burstTime) {
            throw new IllegalArgumentException(
                    "Remaining time must be between zero and burst time.");
        }

        this.remainingTime = remainingTime;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getAssignedAgent() {
        return assignedAgent;
    }

    public void setAssignedAgent(String assignedAgent) {
        this.assignedAgent = assignedAgent;
    }

    public void reset() {
        remainingTime = burstTime;
        status = "Waiting";
        assignedAgent = "Not Assigned";
    }
}
