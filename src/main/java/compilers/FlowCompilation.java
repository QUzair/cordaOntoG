package compilers;

import com.github.javaparser.StaticJavaParser;
import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.body.*;
import com.github.javaparser.ast.expr.*;
import com.github.javaparser.ast.stmt.*;
import com.github.javaparser.ast.type.ClassOrInterfaceType;
import models.FlowModel;
import models.NewState;
import models.RetrieveState;
import models.StateAndContract;
import queryDB.QueryDB;
import transactionUtility.TransactionProperties;

import java.io.*;
import java.util.*;
import java.util.stream.Collectors;

import static com.github.javaparser.ast.Modifier.Keyword.*;

public class FlowCompilation {
    public static void main(Map<String, String> args) throws Exception {

        List<FlowModel> flowProperties = QueryDB.getFlowProperties();
        for(FlowModel flow: flowProperties) {
            createNewFlowClass(flow,args);
        }
    }

    static void createNewFlowClass(FlowModel flow,Map<String, String> args) throws IOException {

        //-------------FETCHING----------------------//

        String packageName = args.get("flowPackage");
        String appName = QueryDB.getAppName();
        String flowName = flow.flowName;
        Map<String, String> fieldsMap = flow.properties;
        StringBuilder transactionComponents = flow.transaction;
        StateAndContract stateContract = QueryDB.getStateName();
        String stateName = stateContract.stateName;
        String contractName = stateContract.contractName;
        //-------------GENERATING----------------------//

        // Generating Flow class
        CompilationUnit compilationUnit = new CompilationUnit();
        compilationUnit.setPackageDeclaration(packageName);

        // Import Libraries
        compilationUnit.addImport(args.get("statePackage")+"."+stateName);
        compilationUnit.addImport(args.get("contractPackage")+"."+contractName);
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
        generateMainFlowCall(initiatorClass, flow, transactionComponents);

        //Add Responder Class
        generateResponderFlow(classDeclaration, appName);

        // Output Generated File
        System.out.println(compilationUnit.toString());
        createNewFlowClassFile(compilationUnit.toString(), flowName);
    }

    public static void generateMainFlowCall(ClassOrInterfaceDeclaration cd, FlowModel flow, StringBuilder transactionComponents) {
        BlockStmt blockStmt = new BlockStmt();
        cd.addMember(generateFlowCallFunction().setBody(blockStmt));

        generateInitialisingStatements(blockStmt, flow.otherParty, flow);
        generateBuildingStatements(blockStmt, transactionComponents);
        if (!flow.amountVar.isEmpty()) {
            generateCheckEnoughCashStatements(blockStmt, flow.amountVar);
            generateCashSpendStatements(blockStmt, flow.otherParty, flow.amountVar);
        }
        generateSigningStatements(blockStmt,flow);
        generateCollectionStatements(blockStmt);
        generateFinalisingStatements(blockStmt);
    }

    public static VariableDeclarationExpr generateSigningKeys(FlowModel flow) {
        StringBuilder keys = new StringBuilder();
        keys.append(".add(ourSigningKey)");
        if(!flow.payee.isEmpty()) keys.append(".addAll(cashSigningKeys)");
        VariableDeclarationExpr signingKeys = StaticJavaParser.parseVariableDeclarationExpr(String.format("final List<PublicKey> signingKeys = new ImmutableList.Builder<PublicKey>()\n" +
                keys +
                ".build()"));
       return signingKeys;
    }

    public static void generateCashSpendStatements(BlockStmt blockStmt, String payee, String amountVar) {
        VariableDeclarationExpr cashKeys = StaticJavaParser.parseVariableDeclarationExpr(String.format("final List<PublicKey> cashSigningKeys = CashUtils.generateSpend(\n" +
                "                    getServiceHub(),\n" +
                "                    utx,\n" +
                "                    ImmutableList.of(new PartyAndAmount<>(newState.%s(), %s)),\n" +
                "                    getOurIdentityAndCert(),\n" +
                "                    ImmutableSet.of()).getSecond()", TransactionProperties.camelCase("get", payee), amountVar));

        blockStmt.addStatement(new ExpressionStmt().setExpression(cashKeys));
    }

    public static ExpressionStmt generateSetCurrentStep(String stepName) {
        return new ExpressionStmt().setExpression(new MethodCallExpr().setName("setCurrentStep").setScope(new NameExpr("progressTracker")).addArgument(new NameExpr(stepName)));
    }

    public static void generateInitialisingStatements(BlockStmt blockStmt, String otherParty,FlowModel flow) {
        List<VariableDeclarationExpr> allNewStates = new ArrayList<>();
        for(NewState newState: flow.newStates) {
            String params = newState.params.stream().collect(Collectors.joining(","));
            VariableDeclarationExpr newVar = StaticJavaParser.parseVariableDeclarationExpr(String.format("final %s newState = new %s(%s)", newState.stateName, newState.stateName, params));
            allNewStates.add(newVar);
        }

        for(RetrieveState retrieveState: flow.retrieveStates) {
            VariableDeclarationExpr retrievedStateRef = StaticJavaParser.parseVariableDeclarationExpr(String.format("final StateAndRef<%s> retrievedState = %s(%s)", retrieveState.stateName, TransactionProperties.camelCase("get", retrieveState.stateName) + "ByLinearId",retrieveState.propertyName));
            VariableDeclarationExpr retrievedState  = StaticJavaParser.parseVariableDeclarationExpr(String.format("final %s newState = retrievedState.getState().getData()", retrieveState.stateName));
            allNewStates.add(retrievedStateRef);
            allNewStates.add(retrievedState);
        }

        VariableDeclarationExpr requiredSigners = StaticJavaParser.parseVariableDeclarationExpr(String.format("final List<PublicKey> requiredSigners = newState.getParticipantKeys()"));
        VariableDeclarationExpr otherFlow = StaticJavaParser.parseVariableDeclarationExpr(String.format("final FlowSession otherFlow = initiateFlow(%s)", otherParty));
        VariableDeclarationExpr ourSigningKey = StaticJavaParser.parseVariableDeclarationExpr(String.format("final PublicKey ourSigningKey = getOurIdentity().getOwningKey()"));

        blockStmt.addStatement(generateSetCurrentStep("INITIALISING"));
        for(VariableDeclarationExpr initNewState:allNewStates) {
            blockStmt.addStatement(new ExpressionStmt().setExpression(initNewState));
        }

        blockStmt.addStatement(new ExpressionStmt().setExpression(requiredSigners));
        blockStmt.addStatement(new ExpressionStmt().setExpression(otherFlow));
        blockStmt.addStatement(new ExpressionStmt().setExpression(ourSigningKey));
    }

    public static void generateBuildingStatements(BlockStmt blockStmt, StringBuilder transactionComponents) {

        VariableDeclarationExpr tx = StaticJavaParser.parseVariableDeclarationExpr("final TransactionBuilder utx = new TransactionBuilder(getFirstNotary())" +
                transactionComponents
               );
        blockStmt.addStatement(generateSetCurrentStep("BUILDING"));
        blockStmt.addStatement(new ExpressionStmt().setExpression(tx));
    }

    public static void generateSigningStatements(BlockStmt blockStmt, FlowModel flow) {
        MethodCallExpr verifyTx = StaticJavaParser.parseExpression(String.format("utx.verify(getServiceHub())"));
        VariableDeclarationExpr ourSignTx = StaticJavaParser.parseVariableDeclarationExpr(String.format("final SignedTransaction ptx = getServiceHub().signInitialTransaction(utx, signingKeys)"));
        blockStmt.addStatement(generateSetCurrentStep("SIGNING"));
        blockStmt.addStatement(new ExpressionStmt().setExpression(verifyTx));
        blockStmt.addStatement(new ExpressionStmt().setExpression(generateSigningKeys(flow)));
        blockStmt.addStatement(new ExpressionStmt().setExpression(ourSignTx));
    }

    public static void generateCollectionStatements(BlockStmt blockStmt) {
        VariableDeclarationExpr sessions = StaticJavaParser.parseVariableDeclarationExpr(String.format("final ImmutableSet<FlowSession> sessions = ImmutableSet.of(otherFlow)"));
        VariableDeclarationExpr signedTransaction = StaticJavaParser.parseVariableDeclarationExpr(String.format("final SignedTransaction stx = subFlow(new CollectSignaturesFlow(\n" +
                "                    ptx,\n" +
                "                    sessions,\n" +
                "                    signingKeys,\n" +
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

        cd.addMember(fd);
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