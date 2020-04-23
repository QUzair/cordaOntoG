import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.body.*;
import com.github.javaparser.ast.expr.*;
import com.github.javaparser.ast.stmt.BlockStmt;
import com.github.javaparser.ast.stmt.ExpressionStmt;
import com.github.javaparser.ast.stmt.IfStmt;
import com.github.javaparser.ast.stmt.ReturnStmt;
import com.github.javaparser.ast.type.ClassOrInterfaceType;
import com.github.javaparser.ast.type.PrimitiveType;


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

        //Creating a map of field -> name, value pairs from tripleStore
        Map<String, String> fieldsMap = QueryDB.getStateProperties();
        StateAndContract stateAndContract =  QueryDB.getStateName();
        String stateName = stateAndContract.stateName;
        String contractName = stateAndContract.contractName;

        //-------------GENERATING----------------------//

        // Creating new Compilation Unit
        CompilationUnit compilationUnit = new CompilationUnit();
        generateStateImports(compilationUnit);
        compilationUnit.setPackageDeclaration("com.contracts");

        // Defining State Name
        ClassOrInterfaceDeclaration classDeclaration = generateStateClass(compilationUnit, stateName, contractName);

        //Adding Members to Class
        //State Constructor
        generateStateFieldsAndConstructor(classDeclaration, fieldsMap);

        //Getters for fields
        generateFieldGetters(classDeclaration, fieldsMap);

        //Participants involved with State Changes
        generateStateParticipantsGetter(classDeclaration, fieldsMap);

        //GetParticipant Keys
        generateGetParticipantKeys(classDeclaration);

        //Equals
        generateEqualsFunction(classDeclaration, stateName, fieldsMap);

        //Hash Function
        generateHashCodeFunction(classDeclaration,fieldsMap);

        System.out.println(compilationUnit.toString());

        createNewStateClass(compilationUnit.toString());

    }

    public static void createNewStateClass(String newFile) throws IOException {
        BufferedWriter writer = new BufferedWriter(new FileWriter("./GeneratedFiles/StateClass.java"));
        writer.write(newFile);

        writer.close();
    }

    public static ClassOrInterfaceDeclaration generateStateClass(CompilationUnit cu, String stateName, String contractName) {
        return cu
                .addClass(stateName).setPublic(true)
                .addImplementedType("LinearState")
                .addAnnotation(new SingleMemberAnnotationExpr().setMemberValue(new ClassExpr(new ClassOrInterfaceType().setName(contractName))).setName("BelongsToContract"));
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
        String methodName = "get" + param.substring(0, 1).toUpperCase() + param.substring(1);
        MethodDeclaration md = new MethodDeclaration()
                .setBody(new BlockStmt().addStatement(new ReturnStmt().setExpression(new NameExpr(param))))
                .setType(type)
                .addModifier(PUBLIC)
                .setName(methodName);

        if(param=="linearId") md.addAnnotation(new MarkerAnnotationExpr().setName("Override"));
        cd.addMember(md);
    }

    public static void generateFieldGetters(ClassOrInterfaceDeclaration cd, Map<String, String> fields) {

        for (Map.Entry<String, String> field : fields.entrySet())
            generateStateFieldGetter(cd, field.getValue(), field.getKey());
    }

    public static void generateStateFieldsAndConstructor(ClassOrInterfaceDeclaration cd, Map<String, String> fields) {

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

    public static void generateStateParticipantsGetter(ClassOrInterfaceDeclaration cd, Map<String, String> fields) {

        ArrayList<String> participants = new ArrayList<String>();
        for (Map.Entry<String, String> field : fields.entrySet()) {
            String fieldType = field.getValue();
            if (fieldType.equals("Party")) participants.add(field.getKey());
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

    public static void generateGetParticipantKeys(ClassOrInterfaceDeclaration cd) {
        MethodDeclaration md = new MethodDeclaration()
                .setBody(new BlockStmt().addStatement(new ReturnStmt().setExpression(new MethodCallExpr("collect")
                        .setScope(new MethodCallExpr("map")
                                .setScope(new MethodCallExpr("stream").setScope(new MethodCallExpr("getParticipants")))
                                .addArgument(new MethodReferenceExpr().setIdentifier("getOwningKey").setScope(new TypeExpr(new ClassOrInterfaceType().setName("AbstractParty"))))
                        )
                        .addArgument(new MethodCallExpr().setName("toList").setScope(new NameExpr("Collectors")))
                )))
                .setType(new ClassOrInterfaceType().setName("List").setTypeArguments(new ClassOrInterfaceType().setName("PublicKey")))
                .setName("getParticipantKeys")
                .setModifiers(PUBLIC);

        cd.addMember(md);
    }

    public static MethodCallExpr methodCallExpr(String property) {
        return new MethodCallExpr()
                .setName("equals")
                .setScope(new NameExpr(property))
                .addArgument(new MethodCallExpr().setName(TransactionProperties.camelCase("get",property)).setScope(new NameExpr("other")));
    }


    public static void generateEqualsFunction(ClassOrInterfaceDeclaration cd, String stateName, Map<String, String> fieldsMap) {

        BlockStmt bStmt = new BlockStmt()
                .addStatement(new IfStmt()
                        .setCondition(new UnaryExpr().setOperator(UnaryExpr.Operator.LOGICAL_COMPLEMENT).setExpression(new EnclosedExpr().setInner(new InstanceOfExpr().setExpression("obj").setType(new ClassOrInterfaceType().setName(stateName)))))
                        .setThenStmt(new BlockStmt().addStatement(new ReturnStmt().setExpression(new BooleanLiteralExpr().setValue(false))))
                )
                .addStatement(new ExpressionStmt(new VariableDeclarationExpr().addVariable(new VariableDeclarator()
                        .setInitializer(new CastExpr().setExpression(new NameExpr("obj")).setType(new ClassOrInterfaceType().setName(stateName)))
                        .setName("other")
                        .setType(stateName)
                )));

        Object[] keys = fieldsMap.keySet().toArray();
        BinaryExpr expr_curr = new BinaryExpr().setOperator(BinaryExpr.Operator.AND);
        BinaryExpr expr_next = new BinaryExpr().setOperator(BinaryExpr.Operator.AND);
        bStmt.addStatement(expr_curr);
        String fieldName;

        for (int i=0;i<fieldsMap.size()-2;i++) {
            fieldName = keys[i].toString();
            expr_next = new BinaryExpr().setOperator(BinaryExpr.Operator.AND);
            expr_curr.setRight(methodCallExpr(fieldName));
            expr_curr.setLeft(expr_next);
            expr_curr = expr_next;
        }
        expr_next.setRight(methodCallExpr(keys[fieldsMap.size()-2].toString()));
        expr_next.setLeft(methodCallExpr(keys[fieldsMap.size()-1].toString()));


        MethodDeclaration md = new MethodDeclaration()
                .setBody(bStmt)
                .setType(new PrimitiveType().setType(PrimitiveType.Primitive.BOOLEAN))
                .setName("equals")
                .addModifier(PUBLIC)
                .addParameter(new Parameter().setVarArgs(false).setName("obj").setType(new ClassOrInterfaceType().setName("Object")))
                .addAnnotation("Override");

        cd.addMember(md);

    }


    public static void generateHashCodeFunction(ClassOrInterfaceDeclaration cd, Map<String, String> fieldsMap) {
        MethodCallExpr hashMethod  = new MethodCallExpr("hash").setScope(new NameExpr("Objects"));
        Object[] keys = fieldsMap.keySet().toArray();
        for(Object key: keys) {
            hashMethod.addArgument(new NameExpr(key.toString()));
        }
        MethodDeclaration md = new MethodDeclaration()
                .setType(new PrimitiveType(PrimitiveType.Primitive.INT))
                .setName("hashCode")
                .addModifier(PUBLIC)
                .addAnnotation(new MarkerAnnotationExpr("Override"))
                .setBody(new BlockStmt().addStatement(new ReturnStmt().setExpression(hashMethod)));

        cd.addMember(md);
    }


}
