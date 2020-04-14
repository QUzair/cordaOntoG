import com.github.javaparser.StaticJavaParser;
import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.body.*;
import com.github.javaparser.ast.expr.*;
import com.github.javaparser.ast.stmt.*;
import com.github.javaparser.ast.type.ClassOrInterfaceType;


import java.io.*;
import java.util.*;
import java.util.stream.Collectors;

import static com.github.javaparser.ast.Modifier.Keyword.*;

public class FlowCompilation {
    public static void main(String[] args) throws Exception {

        /*
        Requires:
        */

        //-------------FETCHING----------------------//

        //Creating an array of commands from triplestore
        String packageName = "com.template.IssueFlow";
        List<String> commands = Arrays.asList("Issue", "Transfer");
        String commandName = "Issue";
        String stateName = "IOUState";
        String contractName = "IOUContract";
        String appName = "IOU";
        String flowName = "IssueIOU";
        String otherParty = "lender";
        Map<String, String> fieldsMap = new HashMap<String, String>();
        fieldsMap.put("amount", "Amount<Currency>");
        fieldsMap.put("lender", "Party");
        fieldsMap.put("externalId", "String");
        List<String> paramsList = Arrays.asList("amount", "lender", "getOurIdentity()");

        //-------------GENERATING----------------------//

        // Generating Flow class
        CompilationUnit compilationUnit = new CompilationUnit();
        compilationUnit.setPackageDeclaration("com." + flowName);

        // Import Libraries
        generateStateImports(compilationUnit);

        // Defining Main Flow Class
        ClassOrInterfaceDeclaration classDeclaration = generateFlowClass(compilationUnit, flowName);

        //Initiator Class
        ClassOrInterfaceDeclaration initiatorClass = generateInitiatorClass(classDeclaration, appName);
        classDeclaration.addMember(initiatorClass);

        // Add Constructor and Fields
        generateFlowFieldsAndConstructor(initiatorClass, fieldsMap);

        generateSteps(initiatorClass);
        generateProgressTracker(initiatorClass);
        generateProgressTrackerGetter(initiatorClass);


        //Generate Call Function with Steps
        generateMainFlowCall(initiatorClass, stateName, contractName, commandName, otherParty, paramsList);

        //Add Responder Class
        generateResponderFlow(classDeclaration, appName);

        // Output Generated File
        System.out.println(compilationUnit.toString());
        createNewFlowClassFile(compilationUnit.toString(), flowName);
    }


    public static void generateMainFlowCall(ClassOrInterfaceDeclaration cd, String stateName, String contractName, String commandName, String otherParty, List<String> paramsList) {
        Boolean settlingCash = false;
        String amountVar = "amount";
        BlockStmt blockStmt = new BlockStmt();
        cd.addMember(generateFlowCallFunction().setBody(blockStmt));

        generateInitialisingStatements(blockStmt, stateName, otherParty, paramsList);
        generateBuildingStatements(blockStmt, contractName, commandName);
        if (settlingCash) {
            generateCheckEnoughCashStatements(blockStmt, amountVar);
            generateCashSpendStatements(blockStmt, otherParty, amountVar);
        }
        generateSigningStatements(blockStmt);
        generateCollectionStatements(blockStmt);
        generateFinalisingStatements(blockStmt);
    }

    public static void generateCashSpendStatements(BlockStmt blockStmt, String payee, String amountVar) {
        VariableDeclarationExpr cashKeys = StaticJavaParser.parseVariableDeclarationExpr(String.format("final List<PublicKey> cashSigningKeys = CashUtils.generateSpend(\n" +
                "                    getServiceHub(),\n" +
                "                    builder,\n" +
                "                    ImmutableList.of(new PartyAndAmount<>(newState.%s(), %s)),\n" +
                "                    getOurIdentityAndCert(),\n" +
                "                    ImmutableSet.of()).getSecond()", TransactionProperties.camelCase("get", payee), amountVar));

        blockStmt.addStatement(new ExpressionStmt().setExpression(cashKeys));
    }

    public static ExpressionStmt generateSetCurrentStep(String stepName) {
        return new ExpressionStmt().setExpression(new MethodCallExpr().setName("setCurrentStep").setScope(new NameExpr("progressTracker")).addArgument(new NameExpr(stepName)));
    }

    public static void generateInitialisingStatements(BlockStmt blockStmt, String stateName, String otherParty, List<String> stateParams) {

        Boolean retrieveState = false;
        String params = stateParams.stream().collect(Collectors.joining(","));
        VariableDeclarationExpr newState;
        VariableDeclarationExpr retrievedState = new VariableDeclarationExpr();
        VariableDeclarationExpr requiredSigners = StaticJavaParser.parseVariableDeclarationExpr(String.format("final List<PublicKey> requiredSigners = newState.getParticipantKeys()"));
        VariableDeclarationExpr otherFlow = StaticJavaParser.parseVariableDeclarationExpr(String.format("final FlowSession otherFlow = initiateFlow(%s)", otherParty));
        VariableDeclarationExpr ourSigningKey = StaticJavaParser.parseVariableDeclarationExpr(String.format("final PublicKey ourSigningKey = getOurIdentity().getOwningKey()"));

        if (retrieveState) {
            newState = StaticJavaParser.parseVariableDeclarationExpr(String.format("final %s newState = retrievedState.getState().getData()", stateName));
            retrievedState = StaticJavaParser.parseVariableDeclarationExpr(String.format("final StateAndRef<%s> retrievedState = %s(linearId)", stateName, TransactionProperties.camelCase("get", stateName) + "ByLinearId"));
        } else {
            newState = StaticJavaParser.parseVariableDeclarationExpr(String.format("final %s newState = new %s(%s,new UniqueIdentifier(externalId))", stateName, stateName, params));
        }

        blockStmt.addStatement(generateSetCurrentStep("INITIALISING"));
        if (retrieveState) blockStmt.addStatement(new ExpressionStmt().setExpression(retrievedState));
        blockStmt.addStatement(new ExpressionStmt().setExpression(newState));
        blockStmt.addStatement(new ExpressionStmt().setExpression(requiredSigners));
        blockStmt.addStatement(new ExpressionStmt().setExpression(otherFlow));
        blockStmt.addStatement(new ExpressionStmt().setExpression(ourSigningKey));
    }

    public static void generateBuildingStatements(BlockStmt blockStmt, String contractName, String commandName) {

        String addCommand = String.format(".addCommand(new %s.Commands.%s(), requiredSigners)",contractName,commandName);
        String addOutput = String.format(".addOutputState(newState, %s.ID)",contractName);
        String addTimeWindow = String.format( ".setTimeWindow(getServiceHub().getClock().instant(), Duration.ofMinutes(5))");

        StringBuilder transactionComponents = new StringBuilder();
        transactionComponents.append(addCommand);
        transactionComponents.append(addOutput);
        transactionComponents.append(addTimeWindow);

        VariableDeclarationExpr tx = StaticJavaParser.parseVariableDeclarationExpr("final TransactionBuilder utx = new TransactionBuilder(getFirstNotary())" +
                transactionComponents
               );

        blockStmt.addStatement(generateSetCurrentStep("BUILDING"));
        blockStmt.addStatement(new ExpressionStmt().setExpression(tx));
    }

    public static void generateSigningStatements(BlockStmt blockStmt) {
        MethodCallExpr verifyTx = StaticJavaParser.parseExpression(String.format("utx.verify(getServiceHub())"));
        VariableDeclarationExpr ourSignTx = StaticJavaParser.parseVariableDeclarationExpr(String.format("final SignedTransaction ptx = getServiceHub().signInitialTransaction(utx, ourSigningKey)"));
        blockStmt.addStatement(generateSetCurrentStep("SIGNING"));
        blockStmt.addStatement(new ExpressionStmt().setExpression(verifyTx));
        blockStmt.addStatement(new ExpressionStmt().setExpression(ourSignTx));
    }

    public static void generateCollectionStatements(BlockStmt blockStmt) {
        VariableDeclarationExpr sessions = StaticJavaParser.parseVariableDeclarationExpr(String.format("final ImmutableSet<FlowSession> sessions = ImmutableSet.of(otherFlow)"));
        VariableDeclarationExpr signedTransaction = StaticJavaParser.parseVariableDeclarationExpr(String.format("final SignedTransaction stx = subFlow(new CollectSignaturesFlow(\n" +
                "                    ptx,\n" +
                "                    sessions,\n" +
                "                    ImmutableList.of(ourSigningKey),\n" +
                "                    COLLECTING.childProgressTracker())\n" +
                "            )"));

        blockStmt.addStatement(generateSetCurrentStep("COLLECTING"));
        blockStmt.addStatement(new ExpressionStmt().setExpression(sessions));
        blockStmt.addStatement(new ExpressionStmt().setExpression(signedTransaction));
    }

    public static void generateFinalisingStatements(BlockStmt blockStmt) {
        MethodCallExpr returnSubFlow = StaticJavaParser.parseExpression(String.format("subFlow(new FinalityFlow(stx, sessions, FINALISING.childProgressTracker()))"));
        blockStmt.addStatement(generateSetCurrentStep("FINALISING"));
        blockStmt.addStatement(new ReturnStmt().setExpression(returnSubFlow));
    }

    public static ClassOrInterfaceDeclaration generateInitiatorClass(ClassOrInterfaceDeclaration cd, String appName) {
        return new ClassOrInterfaceDeclaration()
                .setInterface(false)
                .setName("Initiator")
                .addExtendedType(new ClassOrInterfaceType().setName(appName + "BaseFlow"))
                .setModifiers(PUBLIC, STATIC)
                .addAnnotation(new MarkerAnnotationExpr().setName("InitiatingFlow"))
                .addAnnotation(new MarkerAnnotationExpr("StartableByRPC"));
    }

    public static void generateFlowFieldMember(ClassOrInterfaceDeclaration cd, String type, String param) {
        cd.addField(type, param, PRIVATE, FINAL);
    }

    public static void generateCheckEnoughCashStatements(BlockStmt blockStmt, String amountVar) {
        String cashHandler = "if (cashBalance.getQuantity() <= 0L) {\n" +
                "                throw new FlowException(String.format(\"Borrower has no %s to settle.\", amount.getToken()));\n" +
                "            } else if (cashBalance.getQuantity() < amount.getQuantity()) {\n" +
                "                throw new FlowException(String.format(\n" +
                "                        \"Borrower has only %s but needs %s to settle.\", cashBalance, amount));\n" +
                "            }";

        VariableDeclarationExpr cashBalance = StaticJavaParser.parseVariableDeclarationExpr(String.format("final Amount<Currency> cashBalance = getCashBalance(getServiceHub(), %s.getToken())", amountVar));
        Statement cashCheck = StaticJavaParser.parseStatement(cashHandler.replace("amount", amountVar));

        blockStmt.addStatement(new ExpressionStmt().setExpression(cashBalance));
        blockStmt.addStatement(cashCheck);
    }


    public static void generateFlowFieldsAndConstructor(ClassOrInterfaceDeclaration initiator, Map<String, String> fieldsMap) {
        BlockStmt st = new BlockStmt();
        ConstructorDeclaration consDec = initiator.addConstructor(PUBLIC);

        for (Map.Entry<String, String> field : fieldsMap.entrySet()) {
            String fieldName = field.getKey();
            String fieldType = field.getValue();

            generateFlowFieldMember(initiator, fieldType, fieldName);

            consDec.addParameter(fieldType, fieldName);
            st.addStatement(new ExpressionStmt(new AssignExpr(
                    new FieldAccessExpr(new ThisExpr(), fieldName),
                    new NameExpr(fieldName),
                    AssignExpr.Operator.ASSIGN)));
        }
        consDec.setBody(st);
    }

    public static void generateProgressTrackerGetter(ClassOrInterfaceDeclaration cd) {
        MethodDeclaration md = new MethodDeclaration()
                .setBody(new BlockStmt().addStatement(new ReturnStmt().setExpression(new NameExpr().setName("progressTracker"))))
                .setType(new ClassOrInterfaceType().setName("ProgressTracker"))
                .setName("getProgressTracker")
                .addModifier(PUBLIC)
                .addAnnotation(new MarkerAnnotationExpr("Override"));

        cd.addMember(md);
    }

    public static void generateProgressTracker(ClassOrInterfaceDeclaration cd) {
        FieldDeclaration fd = new FieldDeclaration()
                .setModifiers(PRIVATE, FINAL)
                .addVariable(new VariableDeclarator()
                        .setInitializer(new ObjectCreationExpr()
                                .setType(new ClassOrInterfaceType().setName("ProgressTracker"))
                                .addArgument(new NameExpr("INITIALISING"))
                                .addArgument(new NameExpr("BUILDING"))
                                .addArgument(new NameExpr("SIGNING"))
                                .addArgument(new NameExpr("COLLECTING"))
                                .addArgument(new NameExpr("FINALISING")))
                        .setName("progressTracker")
                        .setType(new ClassOrInterfaceType().setName("ProgressTracker"))
                );
    }

    public static void generateSteps(ClassOrInterfaceDeclaration cd) {
        cd.addMember(generateStep("INITIALISING", "Performing Initial Steps."));
        cd.addMember(generateStep("BUILDING", "Building Transaction."));
        cd.addMember(generateStep("SIGNING", "Signing transaction."));
        cd.addMember(generateCollectingStep("COLLECTING"));
        cd.addMember(generateFinalisingStep("FINALISING"));
    }

    public static FieldDeclaration generateCollectingStep(String stepName) {

        ObjectCreationExpr oce = StaticJavaParser.parseExpression("new Step(\"Collecting counterparty signature.\") {\n" +
                "            @Override public ProgressTracker childProgressTracker() {\n" +
                "                return CollectSignaturesFlow.Companion.tracker();\n" +
                "            }\n" +
                "        }");

        return new FieldDeclaration().setModifiers(PRIVATE, FINAL).addVariable(new VariableDeclarator()
                .setInitializer(oce)
                .setName(stepName)
                .setType(new ClassOrInterfaceType().setName("Step"))
        );
    }

    public static FieldDeclaration generateFinalisingStep(String stepName) {

        ObjectCreationExpr oce = StaticJavaParser.parseExpression("new Step(\"Finalising transaction.\") {\n" +
                "            @Override\n" +
                "            public ProgressTracker childProgressTracker() {\n" +
                "                return FinalityFlow.Companion.tracker();\n" +
                "            }\n" +
                "        }");

        return new FieldDeclaration().setModifiers(PRIVATE, FINAL).addVariable(new VariableDeclarator()
                .setInitializer(oce)
                .setName(stepName)
                .setType(new ClassOrInterfaceType().setName("Step"))
        );
    }


    public static FieldDeclaration generateStep(String stepName, String description) {
        return new FieldDeclaration().setModifiers(PRIVATE, FINAL).addVariable(new VariableDeclarator()
                .setInitializer(new ObjectCreationExpr()
                        .setType(new ClassOrInterfaceType().setName("Step"))
                        .addArgument(new StringLiteralExpr(description)))
                .setName(stepName)
                .setType(new ClassOrInterfaceType().setName("Step"))
        );
    }

    public static void generateResponderFlow(ClassOrInterfaceDeclaration mainFlow, String mainFlowName) {

        MethodCallExpr subFlow = StaticJavaParser.parseExpression(String.format("subFlow(new %sBaseFlow.SignTxFlowNoChecking(otherFlow, SignTransactionFlow.Companion.tracker()))", mainFlowName));
        MethodCallExpr returnStmt = StaticJavaParser.parseExpression("subFlow(new ReceiveFinalityFlow(otherFlow, stx.getId()))");

        ClassOrInterfaceDeclaration responder = new ClassOrInterfaceDeclaration().setInterface(false).setName("Responder")
                .addExtendedType(new ClassOrInterfaceType().setName("FlowLogic").setTypeArguments(new ClassOrInterfaceType().setName("SignedTransaction")))
                .setModifiers(PUBLIC, STATIC)
                .addAnnotation(new SingleMemberAnnotationExpr().setMemberValue(new ClassExpr().setType(new ClassOrInterfaceType().setName("Initiator"))).setName("InitiatedBy"))
                .addMember(new FieldDeclaration().setModifiers(PRIVATE, FINAL).addVariable(new VariableDeclarator().setName("otherFlow").setType(new ClassOrInterfaceType().setName("FlowSession"))))
                .addMember(new ConstructorDeclaration()
                        .setBody(new BlockStmt().addStatement(new ExpressionStmt().setExpression(new AssignExpr().setOperator(AssignExpr.Operator.ASSIGN).setTarget(new FieldAccessExpr().setName("otherFlow")).setValue(new NameExpr("otherFlow")))))
                        .setName("Responder")
                        .setModifiers(PUBLIC)
                        .addParameter(new Parameter().setVarArgs(false).setName("otherFlow").setType(new ClassOrInterfaceType().setName("FlowSession")))
                )
                .addMember(generateFlowCallFunction()
                        .setBody(new BlockStmt()
                                .addStatement(new ExpressionStmt().setExpression(new VariableDeclarationExpr().setModifiers(FINAL).addVariable(new VariableDeclarator().setInitializer(subFlow).setName("stx").setType(new ClassOrInterfaceType().setName("SignedTransaction")))))
                                .addStatement(new ReturnStmt().setExpression(returnStmt))));

        mainFlow.addMember(responder);
    }

    public static MethodDeclaration generateFlowCallFunction() {
        return new MethodDeclaration()
                .setType(new ClassOrInterfaceType().setName("SignedTransaction"))
                .setName("call")
                .setModifiers(PUBLIC)
                .addThrownException(new ClassOrInterfaceType().setName("FlowException"))
                .addAnnotation(new MarkerAnnotationExpr("Suspendable"))
                .addAnnotation(new MarkerAnnotationExpr("Override"));
    }


    static ClassOrInterfaceDeclaration generateFlowClass(CompilationUnit cu, String flowName) {
        ClassOrInterfaceDeclaration cd = new ClassOrInterfaceDeclaration().setInterface(false).setName(flowName).addModifier(PUBLIC);
        cu.addType(cd);
        return cd;
    }

    public static void createNewFlowClassFile(String newFile, String flowName) throws IOException {
        BufferedWriter writer = new BufferedWriter(new FileWriter("./GeneratedFiles/" + flowName + ".java"));
        writer.write(newFile);
        writer.close();
    }

    static void generateStateImports(CompilationUnit cu) throws FileNotFoundException {

        Scanner scanner = new Scanner(new File("src/main/resources/flow_imports"));
        while (scanner.hasNextLine()) {
            String lib = scanner.nextLine();
            cu.addImport(lib);
        }
    }
}