package models;

public class StateAndContract {
    public String stateName;
    public String  contractName;

    public StateAndContract(String stateName, String contractName) {
        this.stateName = stateName;
        this.contractName = contractName;
    }

    public StateAndContract() {
        this.stateName = "";
        this.contractName = "";
    }
}