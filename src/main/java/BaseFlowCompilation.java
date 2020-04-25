import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.Modifier;
import com.github.javaparser.ast.body.*;
import com.github.javaparser.ast.expr.*;
import com.github.javaparser.ast.stmt.*;
import com.github.javaparser.ast.type.ClassOrInterfaceType;
import com.github.javaparser.ast.type.VoidType;

import java.io.*;
import java.util.Scanner;

public class BaseFlowCompilation {

    public static void main(String[] args) throws IOException {
        //-------------FETCHING----------------------//
        //Creating an array of commands from tripleStore
        String packageName = "com.template.IOUFlow";
        String appName = QueryDB.getAppName();
        String stateName = QueryDB.getStateName().stateName;

        //-------------GENERATING----------------------//

        // Creating new Compilation Unit
        CompilationUnit compilationUnit = new CompilationUnit();
        generateStateImports(compilationUnit);
        compilationUnit.setPackageDeclaration(packageName);

        //Define Base Abstract Class
        ClassOrInterfaceDeclaration classDeclaration = generateFlowClass(compilationUnit, appName + "BaseFlow");

        //getFirstNotary Method
        generateGetFirstNotaryMethod(classDeclaration);

        //getStateByLinearId
        generateGetStateByLinearId(classDeclaration, stateName);

        // class to sign transactions
        signTxNoChecking(classDeclaration);

        //Output Generated File
        System.out.println(compilationUnit.toString());
        createNewFlowClassFile(compilationUnit.toString());
    }

    public static void generateGetFirstNotaryMethod(ClassOrInterfaceDeclaration cd) {
        MethodDeclaration md = new MethodDeclaration()
                .setType(new ClassOrInterfaceType().setName("Party"))
                .setName("getFirstNotary")
                .addThrownException(new ClassOrInterfaceType().setName("FlowException"))
                .setBody(new BlockStmt()
                        .addStatement(new ExpressionStmt().setExpression(new VariableDeclarationExpr().addVariable(new VariableDeclarator()
                                .setInitializer(new MethodCallExpr().setName("getNotaryIdentities").setScope(new MethodCallExpr("getNetworkMapCache").setScope(new MethodCallExpr("getServiceHub"))))
                                .setName("notaries")
                                .setType(new ClassOrInterfaceType().setName("List").setTypeArguments(new ClassOrInterfaceType().setName("Party")))
                        )))
                        .addStatement(new IfStmt()
                                .setCondition(new MethodCallExpr("isEmpty").setScope(new NameExpr("notaries")))
                                .setThenStmt(new BlockStmt().addStatement(new ThrowStmt().setExpression(new ObjectCreationExpr().setType(new ClassOrInterfaceType().setName("FlowException")).addArgument(new StringLiteralExpr("No available Notary.")))))
                        )
                        .addStatement(new ReturnStmt().setExpression(new MethodCallExpr("get").setScope(new NameExpr("notaries")).addArgument(new IntegerLiteralExpr().setValue("0"))))
                );
        cd.addMember(md);
    }

    public static void generateGetStateByLinearId(ClassOrInterfaceDeclaration cd, String stateName) {
        MethodDeclaration md = new MethodDeclaration()
                .setType(new ClassOrInterfaceType().setName("StateAndRef").setTypeArguments(new ClassOrInterfaceType().setName(stateName)))
                .setName(TransactionProperties.camelCase("get",stateName)+"ByLinearId")
                .addParameter(new Parameter().setVarArgs(false).setName("linearId").setType(new ClassOrInterfaceType().setName("UniqueIdentifier")))
                .addThrownException(new ClassOrInterfaceType().setName("FlowException"))
                .setBody(new BlockStmt()
                        .addStatement(new VariableDeclarationExpr().addVariable(new VariableDeclarator()
                                .setInitializer(new ObjectCreationExpr()
                                        .setType(new ClassOrInterfaceType().setName("LinearStateQueryCriteria").setScope(new ClassOrInterfaceType().setName("QueryCriteria")))
                                        .addArgument(new NullLiteralExpr())
                                        .addArgument(new MethodCallExpr("of").setScope(new NameExpr("ImmutableList")).addArgument(new NameExpr("linearId")))
                                        .addArgument(new FieldAccessExpr().setName("UNCONSUMED").setScope(new FieldAccessExpr().setName("StateStatus").setScope(new NameExpr("Vault"))))
                                        .addArgument(new NullLiteralExpr())

                                )
                                .setName("queryCriteria")
                                .setType(new ClassOrInterfaceType().setName("QueryCriteria"))
                        ))
                        .addStatement(new ExpressionStmt().setExpression(new VariableDeclarationExpr().addVariable(new VariableDeclarator()
                                .setInitializer(new MethodCallExpr("getStates").setScope(new MethodCallExpr("queryBy")
                                        .setScope(new MethodCallExpr("getVaultService").setScope(new MethodCallExpr("getServiceHub")))
                                        .addArgument(new ClassExpr().setType(new ClassOrInterfaceType().setName(stateName)))
                                        .addArgument(new NameExpr("queryCriteria"))
                                ))
                                .setName(stateName.toLowerCase())
                                .setType(new ClassOrInterfaceType().setName("List").setTypeArguments(new ClassOrInterfaceType().setName("StateAndRef").setTypeArguments(new ClassOrInterfaceType().setName(stateName))))
                        )))
                        .addStatement(new IfStmt()
                                .setCondition(new BinaryExpr().setOperator(BinaryExpr.Operator.NOT_EQUALS).setLeft(new MethodCallExpr("size").setScope(new NameExpr(stateName.toLowerCase()))).setRight(new IntegerLiteralExpr().setValue("1")))
                                .setThenStmt(new BlockStmt().addStatement(new ThrowStmt().setExpression(new ObjectCreationExpr().setType(new ClassOrInterfaceType().setName("FlowException")).addArgument(new MethodCallExpr("format").setScope(new NameExpr("String")).addArgument(new StringLiteralExpr(stateName + " with id %s not found.")).addArgument(new NameExpr().setName("linearId"))))))
                        )
                        .addStatement(new ReturnStmt().setExpression(new MethodCallExpr("get").setScope(new NameExpr(stateName.toLowerCase())).addArgument(new IntegerLiteralExpr().setValue("0"))))


                );

        cd.addMember(md);
    }

    public static void signTxNoChecking(ClassOrInterfaceDeclaration cd) {
        ClassOrInterfaceDeclaration sign_cd = new ClassOrInterfaceDeclaration()
                .setInterface(false)
                .setName("SignTxFlowNoChecking")
                .addExtendedType(new ClassOrInterfaceType().setName("SignTransactionFlow"))
                .addModifier(Modifier.Keyword.STATIC)
                .addMember(new ConstructorDeclaration()
                        .setBody(new BlockStmt().addStatement(new ExplicitConstructorInvocationStmt().setThis(false).addArgument(new NameExpr("otherFlow")).addArgument(new NameExpr("progressTracker"))))
                        .setName("SignTxFlowNoChecking")
                        .addParameter(new Parameter().setVarArgs(false).setName("otherFlow").setType(new ClassOrInterfaceType().setName("FlowSession")))
                        .addParameter(new Parameter().setVarArgs(false).setName("progressTracker").setType(new ClassOrInterfaceType().setName("ProgressTracker")))
                )
                .addMember(new MethodDeclaration().setBody(new BlockStmt()).setType(new VoidType()).setName("checkTransaction").setModifiers(Modifier.Keyword.PROTECTED).addParameter(new Parameter().setVarArgs(false).setName("tx").setType(new ClassOrInterfaceType().setName("SignedTransaction"))).addAnnotation(new MarkerAnnotationExpr().setName("Override")));


        cd.addMember(sign_cd);
    }


    public static ClassOrInterfaceDeclaration generateFlowClass(CompilationUnit cu, String flowName) {
        return cu
                .addClass(flowName).setPublic(true)
                .setInterface(false)
                .setModifiers(Modifier.Keyword.ABSTRACT)
                .addExtendedType(new ClassOrInterfaceType().setName("FlowLogic").setTypeArguments(new ClassOrInterfaceType().setName("SignedTransaction")));
    }


    static void generateStateImports(CompilationUnit cu) throws FileNotFoundException {

        Scanner scanner = new Scanner(new File("src/main/resources/base_flow_imports"));
        while (scanner.hasNextLine()) {
            String lib = scanner.nextLine();
            cu.addImport(lib);
        }
    }

    public static void createNewFlowClassFile(String newFile) throws IOException {
        BufferedWriter writer = new BufferedWriter(new FileWriter("./GeneratedFiles/BaseFlow.java"));
        writer.write(newFile);
        writer.close();
    }
}
