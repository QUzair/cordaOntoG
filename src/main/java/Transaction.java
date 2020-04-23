import java.util.ArrayList;
import java.util.List;

public class Transaction {
    public List<InputState> inputStates;
    public List<OutputState> outputStates;
    public String commandName;
    boolean cashOutput;

    public Transaction() {
        this.inputStates = new ArrayList<>();
        this.outputStates = new ArrayList<>();
        this.commandName = "";
        this.cashOutput = false;
    }

    class InputState {

    }

    class OutputState {

    }
}
