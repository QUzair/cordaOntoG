package compilers;

import com.github.javaparser.StaticJavaParser;
import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.NodeList;
import com.github.javaparser.ast.body.*;
import com.github.javaparser.ast.expr.*;
import com.github.javaparser.ast.stmt.*;
import com.github.javaparser.ast.type.ClassOrInterfaceType;
import com.github.javaparser.ast.type.UnknownType;
import com.github.javaparser.ast.type.VoidType;
import models.CommandConstraints;
import models.ContractCondition;
import models.StateAndContract;
import queryDB.QueryDB;
import transactionUtility.TransactionProperties;

import java.io.*;
import java.util.*;

import static com.github.javaparser.ast.Modifier.Keyword.*;

public class ContractCompilation {

    public static void main(Map<String, String> args) throws Exception {


        /*
        Requires:
        - ContractName - used to declare ClassName
        - Array of commands of Contract.
        - ID of package to be stored in

        - stateName "IOUState"
        - input/output "output"
        - property "value"
        */

        //-------------FETCHING----------------------//
        //Creating an array of commands from tripleStore
        String packageName = args.get("contractPackage");
        TransactionProperties tx = new TransactionProperties();
        List<CommandConstraints> commandConstraints = QueryDB.getContractConditionsInitiator();
        StateAndContract stateContract = QueryDB.getStateName();
        String stateName = stateContract.stateName;
        String contractName = stateContract.contractName;
        List<String> commands = QueryDB.getCommands();

        //-------------GENERATING----------------------//

        // Creating new Compilation Unit
        CompilationUnit compilationUnit = new CompilationUnit();
        compilationUnit.addImport(args.get("statePackage")+"."+stateName);
        generateStateImports(compilationUnit);
        compilationUnit.setPackageDeclaration(packageName);

        //Defining Contract Class
        ClassOrInterfaceDeclaration classDeclaration = generateContractClass(compilationUnit, contractName);

        //Defining ID
        generateContractIdField(classDeclaration, packageName,contractName);

        //Declaring Commands Interface
        generateCommandsInterface(classDeclaration, commands);

        //Main Verify Method
        generateMainVerifyMethod(classDeclaration, commands);

        //Method for retrieving Participants Keys
        generateKeyFromParticipantsMethod(classDeclaration, stateName);

        //Generating verify methods for the different commands
        for(CommandConstraints verify: commandConstraints) {
            generateVerifyCommands(classDeclaration, verify.commandName, verify.constraints, verify.variables);
        }

        //Output Generated File
        System.out.println(compilationUnit.toString());
        createNewContractClassFile(compilationUnit.toString(),contractName);
    }

    public static ExpressionStmt generateConstraintStatements(ContractCondition constraint) {
        UnaryExpr unaryExpr;
        BinaryExpr binaryExpr;
        ExpressionStmt expressionStmt = new ExpressionStmt();

        MethodCallExpr reqMethod = new MethodCallExpr("using")
                .setScope(new NameExpr("req"))
                .addArgument(new StringLiteralExpr(constraint.description));
        if(constraint.right!=null && constraint.right.toString().contains("Date")) {
            String dateOperand =  (constraint.operator == BinaryExpr.Operator.LESS) ? "isBefore":"isAfter";
            MethodCallExpr methodCallExpr = StaticJavaParser.parseExpression(String.format("%s.%s(%s)",constraint.left, dateOperand,((constraint.right!=null)?constraint.right:(constraint.rightInt!=null)?constraint.rightInt:constraint.rightStr)));
            reqMethod.addArgument(methodCallExpr);
            return expressionStmt.setExpression(reqMethod);
        }
        else if(constraint.operator.equals(BinaryExpr.Operator.NOT_EQUALS)) {
            MethodCallExpr methodCallExpr = StaticJavaParser.parseExpression(String.format("%s.equals(%s)",((constraint.right!=null)?constraint.right:(constraint.rightInt!=null)?constraint.rightInt:constraint.rightStr),constraint.left));
             unaryExpr = new UnaryExpr()
                    .setOperator(UnaryExpr.Operator.LOGICAL_COMPLEMENT)
                    .setExpression(methodCallExpr);
            reqMethod.addArgument(unaryExpr);
             return expressionStmt.setExpression(reqMethod);
        } else if(constraint.operator.equals(BinaryExpr.Operator.EQUALS) && constraint.rightInt==null) {
            MethodCallExpr methodCallExpr = StaticJavaParser.parseExpression(String.format("%s.equals(%s)",constraint.left,((constraint.right!=null)?constraint.right:(constraint.rightInt!=null)?constraint.rightInt:constraint.rightStr)));
            reqMethod.addArgument(methodCallExpr);
            return expressionStmt.setExpression(reqMethod);
        } else {
             binaryExpr = new BinaryExpr()
                    .setOperator(constraint.operator)
                    .setLeft(constraint.left)
                    .setRight(constraint.right != null ? constraint.right :(constraint.rightInt!=null ?constraint.rightInt:constraint.rightStr));
            reqMethod.addArgument(binaryExpr);
             return expressionStmt.setExpression(reqMethod);
        }
    }


    public static void generateVerifyCommands(ClassOrInterfaceDeclaration cd, String command, List<ContractCondition> descriptions, List<ExpressionStmt> baseVariables) {
        String commandFn = "verify" + command.substring(0, 1).toUpperCase() + command.substring(1);
        BlockStmt blockStmt = new BlockStmt();
        BlockStmt lambdaBlockStmt = new BlockStmt();
        MethodDeclaration md = new MethodDeclaration();
        MethodCallExpr methodCallExpr = new MethodCallExpr();
        LambdaExpr lambdaExpr = new LambdaExpr();

        for (ExpressionStmt var : baseVariables) {
            lambdaBlockStmt
                    .addStatement(var);
        }

        for (ContractCondition description : descriptions) {
            lambdaBlockStmt.addStatement(generateConstraintStatements(description));
        }
        lambdaBlockStmt.addStatement(new ReturnStmt().setExpression(new NullLiteralExpr()));


        lambdaExpr
                .setEnclosingParameters(false)
                .addParameter(new Parameter().setName("req").setVarArgs(false).setType(new UnknownType()))
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

    public static void createNewContractClassFile(String newFile, String contractName) throws IOException {
        BufferedWriter writer = new BufferedWriter(new FileWriter(String.format("./GeneratedFiles/%s.java",contractName)));
        writer.write(newFile);
        writer.close();
    }

    public static void generateKeyFromParticipantsMethod(ClassOrInterfaceDeclaration cd, String stateName) {

        String variableName = stateName.toLowerCase();
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

    static void generateMainVerifyMethod(ClassOrInterfaceDeclaration cd, List<String> commands) {
        MethodDeclaration md = new MethodDeclaration();
        md
                .setName("verify")
                .setModifiers(PUBLIC)
                .setType(new VoidType())
                .addMarkerAnnotation("Override")
                .addParameter(new Parameter().setName("tx").setType(new ClassOrInterfaceType().setName("LedgerTransaction")).setVarArgs(false));

        BlockStmt blockStmt = new BlockStmt();
        md.setBody(blockStmt);

        generateExpressions(blockStmt, "command", "CommandWithParties", true, "Commands", "requireSingleCommand(tx.getCommands(), Commands.class)");
        generateExpressions(blockStmt, "commandData", "Commands", false, null, "command.getValue()");
        generateExpressions(blockStmt, "setOfSigners", "Set", true, "PublicKey", "new HashSet<>(command.getSigners())");


        BlockStmt noCommandFound = illegalArgumentExceptionStmt("Unrecognised Command");
        IfStmt nextStmt;
        IfStmt topIfStmt = generateVerifyIfStatement(commands.get(0));
        IfStmt prevIfStmt = topIfStmt;

        for (int c = 1; c < commands.size(); c++) {
            nextStmt = generateVerifyIfStatement(commands.get(c));
            prevIfStmt.setElseStmt(nextStmt);
            prevIfStmt = nextStmt;
        }
        prevIfStmt.setElseStmt(noCommandFound);
        blockStmt.addStatement(topIfStmt);

        cd.addMember(md);
    }


    static BlockStmt illegalArgumentExceptionStmt(String message) {
        return new BlockStmt().addStatement(new ThrowStmt().setExpression(new ObjectCreationExpr().setType(new ClassOrInterfaceType().setName("IllegalArgumentException")).addArgument(new StringLiteralExpr(message))));
    }

    static void generateExpressions(BlockStmt blockStmt, String variableName, String classType, Boolean classInterface, String classInterfaceName, String initializer) {
        ExpressionStmt expressionStmt = new ExpressionStmt();
        VariableDeclarationExpr variableDeclarationExpr = new VariableDeclarationExpr().setModifiers(FINAL);
        VariableDeclarator variableDeclarator = new VariableDeclarator();
        variableDeclarator
                .setName(variableName)
                .setInitializer(initializer);

        if (classInterface)
            variableDeclarator.setType(new ClassOrInterfaceType().setName(classType).setTypeArguments(new ClassOrInterfaceType(classInterfaceName)));
        else variableDeclarator.setType(new ClassOrInterfaceType().setName(classType));

        NodeList<VariableDeclarator> variableDeclarators = new NodeList<>();
        variableDeclarators.add(variableDeclarator);
        variableDeclarationExpr.setVariables(variableDeclarators);
        expressionStmt.setExpression(variableDeclarationExpr);
        blockStmt.addStatement(expressionStmt);
    }

    static IfStmt generateVerifyIfStatement(String command) {
        String methodName = TransactionProperties.camelCase("verify", command);
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
                .setType(new ClassOrInterfaceType().setName(command).setScope(new ClassOrInterfaceType().setName("Commands")));

        ifStmt
                .setCondition(instanceOfExpr)
                .setThenStmt(expressionStmt.setExpression(methodCallExpr));

        return ifStmt;
    }

    static void generateCommandsInterface(ClassOrInterfaceDeclaration cd, List<String> commands) {
        ClassOrInterfaceDeclaration commCd = new ClassOrInterfaceDeclaration();
        commCd
                .setModifiers(PUBLIC)
                .setInterface(true)
                .setName("Commands")
                .addExtendedType("CommandData");

        //Adding all Commands
        for (String command : commands) {
            generateCommands(commCd, command);
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

    static void generateContractIdField(ClassOrInterfaceDeclaration cd, String packageName, String contractName) {
        cd.addField("String", "ID", PUBLIC, FINAL, STATIC).getVariable(0).setInitializer(new StringLiteralExpr(packageName+"."+contractName));
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
