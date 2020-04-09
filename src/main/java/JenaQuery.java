
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.*;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

import static com.google.common.net.HttpHeaders.USER_AGENT;

public class JenaQuery {

    public static String stateNameQuery = "query=SELECT ?stateName WHERE {\n" +
            "    ?stateprop a <http://cordaO.org/State> ;\n" +
            "                <http://cordaO.org/stateName> ?stateName ;\n" +
            "}";

    public static String statePropertiesQuery = "query=PREFIX :<http://cordaO.org/> SELECT DISTINCT ?stateName ?propertyName ?dataType WHERE {\n" +
            "    ?state a :State ;\n" +
            "                :stateName ?stateName ;\n" +
            "                :hasProperty ?props .\n" +
            "\n" +
            "    ?props :propertyName ?propertyName ;\n" +
            "            :datatype ?dataType .\n" +
            "}";

    public static String commandsQuery = "query=SELECT ?name WHERE {\n" +
            "    ?command a :Command ;\n" +
            "        :commandName ?name .\n" +
            "}";


    public static String dbUrl = "http://localhost:5820/corda/query";

    public static void main(String[] args) throws IOException {
        getStateProperties();
        getStateName();
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


    public static String getStateName() throws IOException {

            JsonNode jsonNode = createConnection(dbUrl,stateNameQuery);
            final String[] stateName = new String[1];

            Consumer<JsonNode> data = (JsonNode node) -> {
                stateName[0] =  node.get("stateName").get("value").toString().replace("\"","");
            };
            jsonNode.get("results").get("bindings").forEach(data);

            return stateName[0];

        }

    public static Map<String, String> getStateProperties() throws IOException {

            // Return Map Result
            Map<String, String> fieldsMap = new HashMap<>();
            fieldsMap.put("linearId","UniqueIdentifier");

            JsonNode jsonNode = createConnection(dbUrl,statePropertiesQuery);

            Consumer<JsonNode> data = (JsonNode node) -> {
                fieldsMap.put(node.get("propertyName").get("value").toString().replace("\"",""), node.get("dataType").get("value").toString().replace("\"",""));
            };
            jsonNode.get("results").get("bindings").forEach(data);
            return fieldsMap;
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



