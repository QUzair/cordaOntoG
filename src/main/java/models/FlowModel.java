package models;

import java.util.*;

public class FlowModel {
    public String flowName;
    public Map<String, String> properties;
    public StringBuilder transaction;
    public List<NewState> newStates;
    public List<RetrieveState> retrieveStates;
    public String amountVar;
    public String payee;
    public String otherParty;

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
