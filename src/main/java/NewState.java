import java.util.ArrayList;
import java.util.List;

public class NewState {
    public String stateName;
    public List<String> params;

    NewState(String stateName) {
        this.stateName = stateName;
        this.params = new ArrayList<>();
    }
}
