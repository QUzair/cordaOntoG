import java.util.HashMap;
import java.util.Map;

public class MainGenerator {
    public static void main(String[] args) throws Exception {

        Map<String, String> map = new HashMap<>();
        map.put("statePackage", "com.template.states");
        map.put("contractPackage", "com.template.contract");
        map.put("flowPackage", "com.template.flows");

        StateCompilation.main(map);
        ContractCompilation.main(map);
        BaseFlowCompilation.main(map);
        FlowCompilation.main(map);
    }
}
