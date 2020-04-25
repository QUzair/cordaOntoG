
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import com.github.javaparser.ast.expr.BinaryExpr;
import com.github.javaparser.ast.expr.IntegerLiteralExpr;
import com.github.javaparser.ast.expr.MethodCallExpr;
import com.github.javaparser.ast.expr.StringLiteralExpr;
import com.github.javaparser.ast.stmt.ExpressionStmt;

import java.io.*;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.*;
import java.util.function.Consumer;
import java.util.stream.Collectors;

import static com.google.common.net.HttpHeaders.USER_AGENT;

public class QueryDB {

    public static String stateNameQuery = "query=PREFIX :<http://cordaO.org/> SELECT ?stateName ?contractName WHERE { \n" +
            "               ?stateprop a :State ; \n" +
            "             :stateName ?stateName ;\n" +
            "            :belongsTo ?cons .\n" +
            "            ?cons :contractName ?contractName .  \n" +
            "}";

    public static String statePropertiesQuery = "query=PREFIX :<http://cordaO.org/> SELECT DISTINCT ?stateName ?propertyName ?dataType {\n" +
            "    ?state a :State ;\n" +
            "                :stateName ?stateName ;\n" +
            "                :properties/rdf:rest*/rdf:first ?prop .\n" +
            "    ?prop :propertyName ?propertyName ;\n" +
            "          :datatype ?dataType .\n" +
            "} ORDER BY ?prop";

    public static String commandsQuery = "query=PREFIX :<http://cordaO.org/> SELECT ?name WHERE {\n" +
            "    ?command a :Command ;\n" +
            "        :commandName ?name .\n" +
            "}";

    public static String appNameQuery = "query=PREFIX :<http://cordaO.org/> SELECT DISTINCT ?appName {\n" +
            "        ?cdapp a :CordDapp ;\n" +
            "          :hasName ?appName .\n" +
            "}";

    public static String flowQuery = "query= PREFIX :<http://cordaO.org/> SELECT DISTINCT ?stateName ?contractName ?flowName ?appName {\n" +
            "        ?cdapp a :CordDapp ;\n" +
            "          :hasName ?appName .\n" +
            "        ?state a :State ;\n" +
            "                :stateName ?stateName .\n" +
            "        ?flow a :Flow ;\n" +
            "               :flowName ?flowName .\n" +
            "        ?contract a :Contract ;\n" +
            "                  :contractName ?contractName .    \n" +
            "}";

    public static String contractConditionsIntegerQuery = "query=PREFIX :<http://cordaO.org/> SELECT ?cons ?left ?bin ?right ?desc ?leftType ?rightType ?lName ?ldatatype ?lstateClass ?commandName {\n" +
            "    ?c a :Command ;\n" +
            "            :commandName ?commandName ;\n" +
            "            :hasConstraint ?cons ." +
            "\t?cons a :Constraint .\n" +
            "\t<< ?left ?bin ?right >> :belongsTo  ?cons .\n" +
            "    ?cons :hasDescription ?desc .\n" +
            "    ?leftType a :txPropertyType .\n" +
            "    ?left ?leftType ?leftProp .\n" +
            "    ?leftProp :propertyName ?lName .\n" +
            "    ?leftProp :datatype ?ldatatype .\n" +
            "    OPTIONAL {    \n" +
            "    ?leftProp ^:hasProperty ?lstate .\n" +
            "    ?lstate :stateName ?lstateClass .} \n" +
            "    BIND (datatype(?right) AS ?rightType)\n" +
            "    FILTER (!regex(str(?right), \"http\"))" +
            "}";

    public static String contractConditionsQuery = "query=PREFIX :<http://cordaO.org/> SELECT ?cons ?left ?bin ?right ?desc ?leftType ?rightType ?lName ?rName ?ldatatype ?rdatatype ?rstateClass ?lstateClass ?payee ?payeeState ?commandName{\n" +
            "    ?c a :Command ;\n" +
            "        :commandName ?commandName ;\n" +
            "        :hasConstraint ?cons ." +
            "\t?cons a :Constraint .\n" +
            "\t<< ?left ?bin ?right >> :belongsTo  ?cons .\n" +
            "    ?cons :hasDescription ?desc .\n" +
            "    ?leftType a :txPropertyType .\n" +
            "    ?rightType a :txPropertyType .\n" +
            "    ?left ?leftType ?leftProp .\n" +
            "    ?right ?rightType ?rightProp .\n" +
            "    ?leftProp :propertyName ?lName .\n" +
            "    ?rightProp :propertyName ?rName .\n" +
            "OPTIONAL {    ?rightProp ^:hasProperty ?rstate .\n" +
            "    ?leftProp ^:hasProperty ?lstate .\n" +
            "OPTIONAL {    ?leftProp :payee ?payeeProp .\n" +
            "    ?payeeProp ^:hasProperty ?state .\n" +
            "    ?state :stateName ?payeeState ." +
            "    ?payeeProp :propertyName ?payee .}" +
            "    ?rstate :stateName ?rstateClass . \n" +
            "    ?lstate :stateName ?lstateClass .} \n" +
            "    ?leftProp :datatype ?ldatatype .\n" +
            "    ?rightProp :datatype ?rdatatype .\n" +
            "}";

    public static String flowPropertiesQuery = "query=PREFIX :<http://cordaO.org/> SELECT ?flowName ?flowPropName ?datatype { \n" +
            "                ?flow a :Flow; \n" +
            "                    :flowName ?flowName .\n" +
            "                ?flow :properties/rdf:rest*/rdf:first ?flowProp . \n" +
            "                ?flowProp :flowPropertyName ?flowPropName; \n" +
            "                          :datatype ?datatype .   \n" +
            "            }";

    public static String transactionPropertiesQuery = "query=PREFIX :<http://cordaO.org/> SELECT ?flowName ?contractName ?otherParty ?inputState ?inputStateType ?commandName ?outputState ?outputStateType ?payee ?amountVar { \n" +
            "                ?flow a :Flow; \n" +
            "                    :flowName ?flowName ; \n" +
            "                    :otherParty ?otherPartyProp ;\n" +
            "                    :hasTransaction ?tx . \n" +
            "                \n" +
            "                ?tx a :Transaction ;\n" +
            "                :hasCommand ?comm .\n" +
            "                ?comm ^:hasCommand ?contract .\n" +
            "                ?comm :commandName ?commandName .\n" +
            "                ?contract :contractName ?contractName . \n" +
            "                ?otherPartyProp :flowPropertyName ?otherParty .\n" +
            "                OPTIONAL {\n" +
            "                    ?tx :hasOutputState ?outputState .\n" +
            "                    ?outputState a ?outputStateType .\n" +
            "                    OPTIONAL {\n" +
            "                        ?outputState a :Cash ;\n" +
            "                                    :payee ?payeeProp ;\n" +
            "                                     :amount ?amount .\n" +
            "                        ?payeeProp :propertyName ?payee .\n" +
            "                        ?amount :flowPropertyName ?amountVar .\n" +
            "                    }\n" +
            "                }\n" +
            "                OPTIONAL {\n" +
            "                    ?tx :hasInputState ?inputState .\n" +
            "                    ?inputState a ?inputStateType .\n" +
            "                }\n" +
            "}";

    public static String outputStateParamsQuery = "query=PREFIX :<http://cordaO.org/> SELECT DISTINCT ?flowName ?prop ?propName ?stateName{  \n" +
            "    ?flow a :Flow;  \n" +
            "        :flowName ?flowName ;  \n" +
            "        :hasTransaction ?tx .  \n" +
            "        ?tx :hasOutputState ?outputState .   \n" +
            "        ?outputState a :NewState ; \n" +
            "                        :stateClass ?state . \n" +
            "        ?state :stateName ?stateName . \n" +
            "        ?outputState :newProperties/rdf:rest*/rdf:first ?prop .  \n" +
            "    ?prop :flowPropertyName ?propName .  \n" +
            "} ORDER BY ?prop";

    public static String inputStateIDQuery = "query=PREFIX :<http://cordaO.org/> SELECT ?flowName ?stateName ?propName {\n" +
            "    ?flow a :Flow;\n" +
            "        :flowName ?flowName ;\n" +
            "        :hasTransaction ?tx .\n" +
            "    ?tx :hasCommand ?command . \n" +
            "        ?tx :hasInputState ?inputType . \n" +
            "        ?inputState a :RetrievedState ;\n" +
            "                    :stateClass ?state ;\n" +
            "                    :retrieveWith ?prop .\n" +
            "        ?prop :flowPropertyName ?propName .\n" +
            "        ?state :stateName ?stateName  .\n" +
            "}";

    public static String dbUrl = "http://localhost:5820/iou3/query";

    public static void main(String[] args) throws Exception {
//        getStateProperties();
//        getStateName();
//        getContractConditionsInitiator();
        getFlowProperties();
    }

    public static void getOutputStateParams( List<FlowModel> flows) throws IOException {
        JsonNode jsonNode = createConnection(dbUrl, outputStateParamsQuery);
        List<String> params = new ArrayList<>();
        Consumer<JsonNode> data = (JsonNode node) -> {
            String propName = getNodeParam(node, "propName", false);
            String flowName = getNodeParam(node, "flowName", false);
            String stateName = getNodeParam(node, "stateName", false);
            for (int i = 0; i < flows.size(); i++) {
                if (flows.get(i).flowName.equals(flowName)) {
                    if (flows.get(i).newStates.stream().filter(it -> it.stateName.contains(stateName)).collect(Collectors.toList()).size() == 0) {
                        flows.get(i).newStates.add(new NewState(stateName));
                    }
                    for (int j = 0; j < flows.get(i).newStates.size(); j++) {
                        if(flows.get(i).newStates.get(j).stateName.equals(stateName)) {
                            propName =  propName.contains("Id") ? String.format("new UniqueIdentifier(%s)",propName) : propName;
                            flows.get(i).newStates.get(j).params.add(propName);
                        }
                    }
                }
            }
        };
        jsonNode.get("results").get("bindings").forEach(data);
        }

    public static void getInputStateParams( List<FlowModel> flows) throws IOException {
        JsonNode jsonNode = createConnection(dbUrl, inputStateIDQuery);
        List<String> params = new ArrayList<>();
        Consumer<JsonNode> data = (JsonNode node) -> {
            String propName = getNodeParam(node, "propName", false);
            String flowName = getNodeParam(node, "flowName", false);
            String stateName = getNodeParam(node, "stateName", false);
            for (int i = 0; i < flows.size(); i++) {
                if (flows.get(i).flowName.equals(flowName)) {
                    flows.get(i).retrieveStates.add(new RetrieveState(propName,stateName));
                }
            }
        };
        jsonNode.get("results").get("bindings").forEach(data);
    }


    public static List<FlowModel> getFlowProperties() throws IOException {
        JsonNode jsonNode = createConnection(dbUrl,flowPropertiesQuery);
        List<FlowModel> flows = new ArrayList<FlowModel>();

        Consumer<JsonNode> data = (JsonNode node) -> {
            String flowName = getNodeParam(node, "flowName",false);
            String flowPropName = getNodeParam(node, "flowPropName",false);
            String dataType = getNodeParam(node, "datatype",false);
            if(flows.stream().filter(it -> it.flowName.contains(flowName)).collect(Collectors.toList()).size()==0) {
                flows.add(new FlowModel(flowName));
            }
            for (int i = 0; i < flows.size(); i++) {
                if(flows.get(i).flowName.equals(flowName)) {
                    flows.get(i).properties.put(flowPropName,dataType);
                }
            }
        };
        jsonNode.get("results").get("bindings").forEach(data);
        getTransactionProperties(flows);
        getOutputStateParams(flows);
        getInputStateParams(flows);
        return flows;
    }

    public static void getTransactionProperties(List<FlowModel> flows) throws IOException {
        JsonNode jsonNode = createConnection(dbUrl,transactionPropertiesQuery);
        Consumer<JsonNode> data = (JsonNode node) -> {
            String flowName = getNodeParam(node, "flowName",false);
            String commandName = getNodeParam(node, "commandName",false);
            String inputStateURI = getNodeParam(node, "inputState",true);
            String inputStateType = getNodeParam(node, "inputStateType",false);
            String outputStateURI = getNodeParam(node, "outputState",true);
            String outputStateType = getNodeParam(node, "outputStateType",false);
            String payee = getNodeParam(node, "payee",false);
            String amountVar = getNodeParam(node, "amountVar",false);
            String contractName = getNodeParam(node, "contractName",false);
            String otherParty = getNodeParam(node, "otherParty",false);

            for (int i = 0; i < flows.size(); i++) {
                if(flows.get(i).flowName.equals(flowName)) {
                    flows.get(i).otherParty = otherParty;
                    flows.get(i).transaction.append(String.format(".addCommand(new %s.Commands.%s(), requiredSigners)",contractName,commandName));
                    if(inputStateType.contains("RetrievedState")) {
                        flows.get(i).transaction.append(String.format(".addInputState(retrievedState)"));
                    }
                    flows.get(i).transaction.append(String.format(".setTimeWindow(getServiceHub().getClock().instant(), Duration.ofMinutes(5))"));
                    if(outputStateType.contains("NewState")) {
                        flows.get(i).transaction.append(String.format(".addOutputState(newState, %s.ID)",contractName));
                    }
                    if(!amountVar.isEmpty() && !payee.isEmpty()) {
                        flows.get(i).amountVar = amountVar;
                        flows.get(i).payee = payee;
                    }
                }
            }
        };
        jsonNode.get("results").get("bindings").forEach(data);
    }

    public static List<String> getCommands() throws IOException {
        JsonNode jsonNode = createConnection(dbUrl,commandsQuery);
        List<String> commands = new ArrayList<String>();

        Consumer<JsonNode> data = (JsonNode node) -> {
            commands.add(node.get("name").get("value").toString().replace("\"",""));
        };
        jsonNode.get("results").get("bindings").forEach(data);
        return commands;
    }

    public static StateAndContract getStateName() throws IOException {
            StateAndContract stateAndContract = new StateAndContract();
            JsonNode jsonNode = createConnection(dbUrl,stateNameQuery);
            Consumer<JsonNode> data = (JsonNode node) -> {
                stateAndContract.stateName =  node.get("stateName").get("value").toString().replace("\"","");
                stateAndContract.contractName =  node.get("contractName").get("value").toString().replace("\"","");
            };
            jsonNode.get("results").get("bindings").forEach(data);
            return stateAndContract;
        }

    public static Map<String, String> getStateProperties() throws IOException {
            Map<String, String> fieldsMap = new LinkedHashMap<>();

            JsonNode jsonNode = createConnection(dbUrl,statePropertiesQuery);

            Consumer<JsonNode> data = (JsonNode node) -> {
                fieldsMap.put(node.get("propertyName").get("value").toString().replace("\"",""), node.get("dataType").get("value").toString().replace("\"",""));
            };
            jsonNode.get("results").get("bindings").forEach(data);
            fieldsMap.put("linearId","UniqueIdentifier");
            return fieldsMap;
        }

    public static String getAppName() throws IOException {

        JsonNode jsonNode = createConnection(dbUrl,appNameQuery);
        List<String> appName = new ArrayList<>();
        Consumer<JsonNode> data = (JsonNode node) -> {
            appName.add(node.get("appName").get("value").toString().replace("\"",""));
        };
        jsonNode.get("results").get("bindings").forEach(data);
        return appName.get(0);
    }

        public static  List<CommandConstraints> getContractConditionsInitiator() throws Exception {
            List<CommandConstraints> commandsWithConstraints = new ArrayList<>();

            List<String> commands = getCommands();
            for(String command: commands) {
                commandsWithConstraints.add(new CommandConstraints(command));
            }
            getContractConditionsInteger(commandsWithConstraints);
            getContractConditions(commandsWithConstraints);
            return commandsWithConstraints;
        }


    public static String getNodeParam(JsonNode node, String param, Boolean removeNamespace) {
        if (!removeNamespace) {
            try {
                return node.get(param).get("value").toString().replace("\"", "");
            } catch (Exception e) {
                return "";
            }
        } else {
            try {
                return node.get(param).get("value").toString().replace("\"", "").replace("http://cordaO.org/", "");
            } catch (Exception e) {
                return "";
            }
        }
    }



    public static List<CommandConstraints> getContractConditions(List<CommandConstraints> commandsWithConstraints) throws Exception{

        JsonNode jsonNode = createConnection(dbUrl,contractConditionsQuery);
        TransactionProperties tx = new TransactionProperties();

        Consumer<JsonNode> data = (JsonNode node) -> {
            MethodCallExpr lstateProp, rstateProp;
            String lName = getNodeParam(node,"lName",false);
            String rName = getNodeParam(node,"rName",false);
            String rstateClass = getNodeParam(node,"rstateClass",false);
            String lstateClass = getNodeParam(node,"lstateClass",false);
            String binOperator = getNodeParam(node,"bin",true);
            String desc = getNodeParam(node,"desc",false);
            String commandName =getNodeParam(node,"commandName",false);
            String leftType = getNodeParam(node,"leftType",true);
            String payee = getNodeParam(node,"payee",false);
            String payeeState = getNodeParam(node,"payeeState",false);
            String rightType = getNodeParam(node,"rightType",true);
            String ltxType = "";
            String rtxType = "";
            ContractCondition contractCondition;
            BinaryExpr.Operator operator = getOperator(binOperator);

            ltxType = leftType.contains("Input") ? tx.TX_INPUT:tx.TX_OUTPUT;
            rtxType = rightType.contains("Input") ? tx.TX_INPUT:tx.TX_OUTPUT;

            try {
                lstateProp = (lName.equals("AcceptableCash")) ? tx.getAcceptableCashQuantity(): tx.getStateProperty(lName, lstateClass,ltxType);
                rstateProp = tx.getStateProperty(rName, rstateClass,rtxType);
            } catch (Exception e) {
                lstateProp = null;
                rstateProp = null;
                e.printStackTrace();
            }

            if(lstateProp!=null && rstateProp!=null){
                contractCondition = new ContractCondition(desc,lstateProp,operator,rstateProp);
                for (int i = 0; i < commandsWithConstraints.size(); i++) {
                    if(commandsWithConstraints.get(i).commandName.equals(commandName)) {
                        commandsWithConstraints.get(i).constraints.add(contractCondition);
                        if(lstateClass.contains("Cash")) {
                            commandsWithConstraints.get(i).variables.add(tx.singleStateType(rstateClass, tx.TX_OUTPUT));
                            commandsWithConstraints.get(i).variables.add(tx.getCashFromOutput());
                            commandsWithConstraints.get(i).variables.add(tx.acceptableCashFromPayee( payee, payeeState));
                        }
                        if(!lstateClass.contains("Cash")) {
                            commandsWithConstraints.get(i).variables.add(tx.singleStateType(lstateClass, ltxType));
                        }
                        if(!rstateClass.contains("Cash")) {
                            commandsWithConstraints.get(i).variables.add(tx.singleStateType(rstateClass, rtxType));
                        }
                    }
                }
            }
        };

        jsonNode.get("results").get("bindings").forEach(data);
        List<ExpressionStmt> listWithoutDuplicates;
        for (int i = 0; i < commandsWithConstraints.size(); i++) {
            listWithoutDuplicates = commandsWithConstraints.get(i).variables.stream().distinct().collect(Collectors.toList());
            commandsWithConstraints.get(i).variables = listWithoutDuplicates;
        }
        return commandsWithConstraints;
    }


        public static List<CommandConstraints> getContractConditionsInteger(List<CommandConstraints> commandsWithConstraints) throws Exception{

            JsonNode jsonNode = createConnection(dbUrl,contractConditionsIntegerQuery);
            TransactionProperties tx = new TransactionProperties();


            Consumer<JsonNode> data = (JsonNode node) -> {
                MethodCallExpr stateProp = new MethodCallExpr();;
                String binOperator = getNodeParam(node,"lName",true);
                String lName = getNodeParam(node,"lName",false);
                String left = getNodeParam(node,"left",true);
                String right = getNodeParam(node,"right",false);
                String leftType = getNodeParam(node,"leftType",false);
                String rightType = getNodeParam(node,"rightType",false);
                String desc = getNodeParam(node,"desc",false);
                String commandName = getNodeParam(node,"commandName",false);
                String lstateClass = getNodeParam(node,"lstateClass",false);
                String ldatatype = getNodeParam(node,"ldatatype",false);
                String txType = "";

                if(!lstateClass.isEmpty()) {
                    try {
                        txType = leftType.contains("Input") ? tx.TX_INPUT:tx.TX_OUTPUT;
                        if(ldatatype.contains("Amount<Currency>")) stateProp = tx.getStateProperty(lName, lstateClass,txType,true);
                        else stateProp = tx.getStateProperty(lName, lstateClass,txType);

                    } catch (Exception e) {
                        stateProp = new MethodCallExpr();
                        e.printStackTrace();
                    }
                } else if(left.contains("Size")) {
                    stateProp = tx.txInputOutputSize(lName.contains("Input") ? tx.TX_INPUT:tx.TX_OUTPUT);
                } else {
                    String[] listIO = lName.split(" ");
                    stateProp = tx.listStatesIO(listIO[1],listIO[0].contains("Input") ? tx.TX_INPUT:tx.TX_OUTPUT);
                }
                BinaryExpr.Operator operator = getOperator(binOperator);
                ContractCondition contractCondition = rightType.contains("integer") ? new ContractCondition(desc,stateProp,operator, new IntegerLiteralExpr(right)):new ContractCondition(desc,stateProp,operator, new StringLiteralExpr(right));
                for (int i = 0; i < commandsWithConstraints.size(); i++) {
                    if(commandsWithConstraints.get(i).commandName.equals(commandName)) {
                        commandsWithConstraints.get(i).constraints.add(contractCondition);
                        if(!lstateClass.isEmpty())  commandsWithConstraints.get(i).variables.add(tx.singleStateType(lstateClass, txType));
                        if((lName.contains("Input") || lName.contains("Output")) && !lName.contains("Size")){
                            String[] listIO = lName.split(" ");
                            commandsWithConstraints.get(i).variables.add(tx.getListInputOutputsOfState(listIO[1],listIO[0]));
                        }
                    }
                }
            };
            jsonNode.get("results").get("bindings").forEach(data);

            List<ExpressionStmt> listWithoutDuplicates;
            for (int i = 0; i < commandsWithConstraints.size(); i++) {
                listWithoutDuplicates = commandsWithConstraints.get(i).variables.stream().distinct().collect(Collectors.toList());
                commandsWithConstraints.get(i).variables = listWithoutDuplicates;
            }
            return commandsWithConstraints;
        }

        public static BinaryExpr.Operator getOperator(String operator) {
            switch(operator) {
                case "equals":
                    return BinaryExpr.Operator.EQUALS;
                case "greaterEquals":
                    return BinaryExpr.Operator.GREATER_EQUALS;
                case "greaterThan":
                    return BinaryExpr.Operator.GREATER;
                case "lessThan":
                    return BinaryExpr.Operator.LESS;
                case "lessEquals":
                    return BinaryExpr.Operator.LESS_EQUALS;
                case "notEquals":
                    return BinaryExpr.Operator.NOT_EQUALS;
                default:
                    return BinaryExpr.Operator.EQUALS;
            }
        }

    public static JsonNode createConnection(String url, String query) throws IOException {
        URL obj = new URL(url);
        HttpURLConnection con = (HttpURLConnection) obj.openConnection();
        con.setRequestMethod("GET");
        con.setRequestProperty("Accept", "application/sparql-results+json");
        con.setRequestProperty("User-Agent", USER_AGENT);

        con.setDoOutput(true);
        OutputStream os = con.getOutputStream();
        os.write(query.getBytes());
        os.flush();
        os.close();

        int responseCode = con.getResponseCode();

        if (responseCode == HttpURLConnection.HTTP_OK) { //success
            BufferedReader in = new BufferedReader(new InputStreamReader(
                    con.getInputStream()));
            String inputLine;
            StringBuffer response = new StringBuffer();

            while ((inputLine = in.readLine()) != null) {
                response.append(inputLine);
            }
            in.close();

            ObjectMapper mapper = new ObjectMapper();
            JsonNode jsonNode = mapper.readTree(response.toString());
            return jsonNode;
        } else {
            return null;
        }
    }

}



