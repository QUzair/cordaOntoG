import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.body.ClassOrInterfaceDeclaration;
import com.github.javaparser.ast.body.ConstructorDeclaration;
import com.github.javaparser.ast.body.MethodDeclaration;
import com.github.javaparser.ast.expr.AssignExpr;
import com.github.javaparser.ast.expr.FieldAccessExpr;
import com.github.javaparser.ast.expr.NameExpr;
import com.github.javaparser.ast.expr.ThisExpr;
import com.github.javaparser.ast.stmt.BlockStmt;
import com.github.javaparser.ast.stmt.ExpressionStmt;
import com.github.javaparser.ast.stmt.ReturnStmt;


import java.io.*;
import java.util.*;

import static com.github.javaparser.ast.Modifier.Keyword.*;
import static com.github.javaparser.ast.Modifier.Keyword.PUBLIC;

public class StateCompilation {

    public static void main(String[] args) throws Exception {

        /*
        Requires:
        - StateName - used to declare ClassName
        - Key,Value pairs of fields/properties of State. (Key:fieldName, value: fieldType)
        */

        //-------------FETCHING----------------------//

        //Creating a map of field -> name, value pairs from triplestore
        Map<String, String> fieldsMap = new HashMap<>();
        fieldsMap.put("value", "int");
        fieldsMap.put("lender", "Party");
        fieldsMap.put("borrower", "Party");

        //-------------GENERATING----------------------//

        // Creating new Compilation Unit
        CompilationUnit compilationUnit = new CompilationUnit();
        generateStateImports(compilationUnit);
        compilationUnit.setPackageDeclaration("com.contracts");

        // Defining State Name
        ClassOrInterfaceDeclaration classDeclaration = generateStateClass(compilationUnit, "IOUState");

        //State Constructor
        generateStateFieldsAndConstructor(classDeclaration, fieldsMap);

        //Getters for fields
        generateStateFieldGetter(classDeclaration, "int", "value");
        generateStateFieldGetter(classDeclaration, "Party", "lender");
        generateStateFieldGetter(classDeclaration, "Party", "borrower");

        //Participants involved with State Changes - Pass Participants as list of Party fields existing in State
        generateStateParticipantsGetter(classDeclaration, fieldsMap);

        System.out.println(compilationUnit.toString());

        createNewStateClass(compilationUnit.toString());

    }

    public static void createNewStateClass(String newFile) throws IOException {
        BufferedWriter writer = new BufferedWriter(new FileWriter("./GeneratedFiles/StateClass.java"));
        writer.write(newFile);

        writer.close();
    }

    public static ClassOrInterfaceDeclaration generateStateClass(CompilationUnit cu, String stateName) {
        return cu
                .addClass(stateName).setPublic(true)
                .addImplementedType("ContractState");
    }

    public static void generateStateImports(CompilationUnit cu) throws FileNotFoundException {

        Scanner scanner = new Scanner(new File("src/main/resources/state_imports"));
        while (scanner.hasNextLine()) {
            String lib = scanner.nextLine();
            cu.addImport(lib);
        }
    }

    public static void generateStateField(ClassOrInterfaceDeclaration cd, String type, String param) {

        cd.addField(type, param, PRIVATE, FINAL);
    }

    public static void generateStateFieldGetter(ClassOrInterfaceDeclaration cd, String type, String param) {

        cd.addMethod("get" + param, PUBLIC)
                .setType(type)
                .setBody(new BlockStmt().addStatement(new ReturnStmt().setExpression(new NameExpr(param))));
    }

    public static void generateFieldGetters(ClassOrInterfaceDeclaration cd,Map<String,String> fields) {

        for (Map.Entry<String, String> field : fields.entrySet()) generateStateFieldGetter(cd, field.getValue(), field.getKey());
    }

    public static void generateStateFieldsAndConstructor(ClassOrInterfaceDeclaration cd,Map<String,String> fields) {

        ConstructorDeclaration consDec = cd.addConstructor(PUBLIC);
        BlockStmt st = new BlockStmt();

        for (Map.Entry<String, String> field : fields.entrySet()) {
            String fieldName = field.getKey();
            String fieldType = field.getValue();

            consDec.addParameter(fieldType, fieldName);
            st.addStatement(new ExpressionStmt(new AssignExpr(
                    new FieldAccessExpr(new ThisExpr(), fieldName),
                    new NameExpr(fieldName),
                    AssignExpr.Operator.ASSIGN)));

            generateStateField(cd, fieldType, fieldName);
        }

        consDec.setBody(st);
    }

    public static void generateStateParticipantsGetter(ClassOrInterfaceDeclaration cd, Map<String,String> fields) {

        ArrayList<String> participants = new ArrayList<String>();
        for (Map.Entry<String, String> field : fields.entrySet()) {
            String fieldType = field.getValue();
            if(fieldType.equals("Party")) participants.add(field.getKey());
        }
        String participantsList = String.join(",", participants);

        MethodDeclaration methodDeclaration = cd.addMethod("getParticipants", PUBLIC);
        methodDeclaration
                .setType("List<AbstractParty>")
                .addMarkerAnnotation("Override");
        BlockStmt blockStmt = new BlockStmt();
        methodDeclaration.setBody(blockStmt);

        ReturnStmt returnStmt = new ReturnStmt();
        NameExpr returnNameExpr = new NameExpr();
        returnNameExpr.setName("Arrays.asList(" + participantsList + ")");
        returnStmt.setExpression(returnNameExpr);
        blockStmt.addStatement(returnStmt);
    }
}
