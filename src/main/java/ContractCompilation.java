import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.NodeList;
import com.github.javaparser.ast.body.*;
import com.github.javaparser.ast.expr.*;
import com.github.javaparser.ast.stmt.BlockStmt;
import com.github.javaparser.ast.stmt.ExpressionStmt;
import com.github.javaparser.ast.stmt.IfStmt;
import com.github.javaparser.ast.stmt.ReturnStmt;
import com.github.javaparser.ast.type.ClassOrInterfaceType;
import com.github.javaparser.ast.type.Type;
import com.github.javaparser.ast.type.VoidType;

import java.io.*;
import java.util.*;

import static com.github.javaparser.ast.Modifier.Keyword.*;

public class ContractCompilation {

    public static void main(String[] args) throws Exception {

        /*
        Requires:
        - ContractName - used to declare ClassName
        - Array of commands of Contract.
        - ID of package to be stored in
        */

        //-------------FETCHING----------------------//

        //Creating an array of commands from triplestore
        String packageName = "com.template.IOUContract";
        List<String> commands =  Arrays.asList("Issue", "Transfer");
//        List<String> commands = JenaQuery.getCommands();
        String stateName = "IOUState";
        String contractName = "IOU";

        //-------------GENERATING----------------------//

        // Creating new Compilation Unit
        CompilationUnit compilationUnit = new CompilationUnit();
        generateStateImports(compilationUnit);
        compilationUnit.setPackageDeclaration("com.contracts");

        //Defining Contract Class
        ClassOrInterfaceDeclaration classDeclaration = generateContractClass(compilationUnit, "IOUContract");

        //Defining ID
        generateContractIdField(classDeclaration, packageName);

        //Declaring Commands Interface
        generateCommandsInterface(classDeclaration, commands);

        //Main Verify Method
        generateMainVerifyMethod(classDeclaration,commands);

        //Method for retrieving Participants Keys
        generateKeyFromParticipantsMethod(classDeclaration,stateName,contractName);

        //Generating verify methods for the different commands
        generateVerifyCommands(classDeclaration,"Issue");


        //Output Generated File
        System.out.println(compilationUnit.toString());
        createNewContractClassFile(compilationUnit.toString());
    }

    public static ExpressionStmt generateConstraintStatements() {
        ExpressionStmt expressionStmt = new ExpressionStmt().setExpression(
                new MethodCallExpr("using")
                        .setScope(new NameExpr("req"))
                        .addArgument(new StringLiteralExpr("No inputs to be consumed"))
                        .addArgument(
                                new MethodCallExpr("isEmpty")
                                    .setScope(new MethodCallExpr("getInputStates").setScope(new NameExpr("tx")))
                        )
        );
        return  expressionStmt;
    }

    public static void generateVerifyCommands(ClassOrInterfaceDeclaration cd, String command) {
        String commandFn = "verify" + command.substring(0, 1).toUpperCase() + command.substring(1);
        BlockStmt blockStmt = new BlockStmt();
        BlockStmt lambdaBlockStmt = new BlockStmt();
        MethodDeclaration md = new MethodDeclaration();
        MethodCallExpr methodCallExpr = new MethodCallExpr();
        LambdaExpr lambdaExpr = new LambdaExpr();
        ExpressionStmt expressionStmt = new ExpressionStmt();

         expressionStmt = new ExpressionStmt().setExpression(new VariableDeclarationExpr().addVariable(
                new VariableDeclarator()
                        .setName("iouState")
                        .setType("TemplateState")
                        .setInitializer(
                            new CastExpr()
                                    .setExpression(
                                        new MethodCallExpr()
                                                .setName("get")
                                                .setScope(new MethodCallExpr("getOutputStates").setScope(new NameExpr("tx")))
                                                .addArgument(new IntegerLiteralExpr("0"))
                                    )
                                    .setType(new ClassOrInterfaceType().setName("TemplateState"))
                        )
        ));

        lambdaBlockStmt
                .addStatement(expressionStmt)
                .addStatement(generateConstraintStatements());

        lambdaExpr
                .setEnclosingParameters(false)
                .addParameter(new Parameter().setName("req").setVarArgs(false))
                .setBody(lambdaBlockStmt);


        methodCallExpr
                .setName("requireThat")
                .addArgument(lambdaExpr);

        blockStmt.addStatement(new ExpressionStmt().setExpression(methodCallExpr));

        md
                .setName(commandFn)
                .setModifiers(PRIVATE)
                .setType(new VoidType())
                .addParameter(new Parameter().setName("tx").setType(new ClassOrInterfaceType().setName("LedgerTransaction")).setVarArgs(false))
                .addParameter(new Parameter().setName("signers").setType(new ClassOrInterfaceType().setName("Set").setTypeArguments(new ClassOrInterfaceType().setName("PublicKey"))).setVarArgs(false))
                .setBody(blockStmt);

        cd.addMember(md);
    }

    public static void createNewContractClassFile(String newFile) throws IOException {
        BufferedWriter writer = new BufferedWriter(new FileWriter("./GeneratedFiles/ContractClass.java"));
        writer.write(newFile);
        writer.close();
    }

    public static void generateKeyFromParticipantsMethod(ClassOrInterfaceDeclaration cd, String stateName, String contractName) {

        String variableName = contractName.toLowerCase()+"State";
        BlockStmt blockStmt = new BlockStmt();
        MethodDeclaration md = new MethodDeclaration();
        MethodCallExpr methodCallExpr = new MethodCallExpr();
        md
                .setName("keysFromParticipants")
                .setModifiers(PRIVATE)
                .setType(new ClassOrInterfaceType().setName("Set").setTypeArguments(new ClassOrInterfaceType().setName("PublicKey")))
                .addParameter(new Parameter().setName(variableName).setType(new ClassOrInterfaceType().setName(stateName)).setVarArgs(false));

        methodCallExpr
                .setName("collect")
                .addArgument(new MethodCallExpr("toSet"))
                .setScope(
                        new MethodCallExpr("map")
                                .addArgument(new MethodReferenceExpr().setIdentifier("getOwningKey").setScope(new TypeExpr(new ClassOrInterfaceType("AbstractParty"))))
                                .setScope(
                                        new MethodCallExpr("stream")
                                                .setScope(
                                                        new MethodCallExpr("getParticipants")
                                                                .setScope(new NameExpr(variableName))
                                                )
                                )
                );

        md.setBody(blockStmt.addStatement(new ReturnStmt().setExpression(methodCallExpr)));
        cd.addMember(md);
    }


    static void generateMainVerifyMethod(ClassOrInterfaceDeclaration cd,List<String> commands) {
        MethodDeclaration md = new MethodDeclaration();
        md
                .setName("verify")
                .setModifiers(PUBLIC)
                .setType(new VoidType())
                .addMarkerAnnotation("Override")
                .addParameter(new Parameter().setName("tx").setType(new ClassOrInterfaceType().setName("LedgerTransaction")).setVarArgs(false));

        BlockStmt blockStmt = new BlockStmt();
        md.setBody(blockStmt);

        generateExpressions(blockStmt,"command","CommandWithParties",true,"Commands","requireSingleCommand(tx.getCommands(), Commands.class)");
        generateExpressions(blockStmt,"commandData","Commands",false,null,"command.getValue()");
        generateExpressions(blockStmt,"setOfSigners","Set",true,"PublicKey","new HashSet<>(command.getSigners())");

        for(String command:commands) {
            generateIfStatementsForCommandsVerify(blockStmt,command);
        }

        cd.addMember(md);
    }

    static void generateExpressions( BlockStmt blockStmt, String variableName, String classType, Boolean classInterface, String classInterfaceName, String initializer) {
        ExpressionStmt expressionStmt = new ExpressionStmt();
        VariableDeclarationExpr variableDeclarationExpr = new VariableDeclarationExpr().setModifiers(FINAL);
        VariableDeclarator variableDeclarator = new VariableDeclarator();
        variableDeclarator
                .setName(variableName)
                .setInitializer(initializer);

        if(classInterface) variableDeclarator.setType(new ClassOrInterfaceType().setName(classType).setTypeArguments(new ClassOrInterfaceType(classInterfaceName)));
        else variableDeclarator.setType(new ClassOrInterfaceType().setName(classType));

        NodeList<VariableDeclarator> variableDeclarators = new NodeList<>();
        variableDeclarators.add(variableDeclarator);
        variableDeclarationExpr.setVariables(variableDeclarators);
        expressionStmt.setExpression(variableDeclarationExpr);
        blockStmt.addStatement(expressionStmt);
    }

    static void generateIfStatementsForCommandsVerify( BlockStmt blockStmt, String command) {
        String methodName = "verify" + command.substring(0, 1).toUpperCase() + command.substring(1);
        IfStmt ifStmt = new IfStmt();
        ExpressionStmt expressionStmt = new ExpressionStmt();
        MethodCallExpr methodCallExpr = new MethodCallExpr();
        InstanceOfExpr instanceOfExpr = new InstanceOfExpr();

        methodCallExpr
                .setName(methodName)
                .addArgument(new NameExpr("tx"))
                .addArgument(new NameExpr("setOfSigners"));

        instanceOfExpr
                .setExpression("commandData")
                .setType(new ClassOrInterfaceType(command).setScope(new ClassOrInterfaceType("Commands")));

        ifStmt
                .setCondition(instanceOfExpr)
                .setThenStmt(expressionStmt.setExpression(methodCallExpr));


        blockStmt.addStatement(ifStmt);
    }

    static void generateCommandsInterface(ClassOrInterfaceDeclaration cd, List<String> commands) {
        ClassOrInterfaceDeclaration commCd = new ClassOrInterfaceDeclaration();
        commCd
                .setModifiers(PUBLIC)
                .setInterface(true)
                .setName("Commands")
                .addExtendedType("CommandData");

        //Adding all Commands
        for(String command: commands) {
            generateCommands(commCd,command);
        }

        cd.addMember(commCd);

    }

    static void generateCommands(ClassOrInterfaceDeclaration commandsInterface, String CommandType) {
        ClassOrInterfaceDeclaration comType = new ClassOrInterfaceDeclaration();
        comType
                .setName(CommandType)
                .addExtendedType("TypeOnlyCommandData")
                .addImplementedType("Commands");

        commandsInterface.addMember(comType);

    }

    static void generateContractIdField(ClassOrInterfaceDeclaration cd,String packageName) {

        cd.addField("String","ID",PRIVATE,FINAL,STATIC).getVariable(0).setInitializer(new StringLiteralExpr(packageName));;
    }


    static ClassOrInterfaceDeclaration generateContractClass(CompilationUnit cu, String contractName) {
        return cu
                .addClass(contractName).setPublic(true)
                .addImplementedType("Contract");
    }

    static void generateStateImports(CompilationUnit cu) throws FileNotFoundException {

        Scanner scanner = new Scanner(new File("src/main/resources/contract_imports"));
        while (scanner.hasNextLine()) {
            String lib = scanner.nextLine();
            cu.addImport(lib);
        }
    }
}
