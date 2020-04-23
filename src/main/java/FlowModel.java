import java.util.*;

public class FlowModel {
    String flowName;
    Map<String, String> properties;
    StringBuilder transaction;
    List<NewState> newStates;
    List<RetrieveState> retrieveStates;
    String amountVar;
    String payee;
    String otherParty;

    public FlowModel(String flowName) {
        this.flowName = flowName;
        this.properties = new LinkedHashMap<>();
        this.transaction = new StringBuilder();
        this.newStates = new ArrayList<>();
        this.retrieveStates = new ArrayList<>();
        this.amountVar = "";
        this.payee = "";
        this.otherParty = "";
    }
}
