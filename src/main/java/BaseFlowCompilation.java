import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.Modifier;
import com.github.javaparser.ast.body.ClassOrInterfaceDeclaration;
import com.github.javaparser.ast.body.MethodDeclaration;
import com.github.javaparser.ast.body.VariableDeclarator;
import com.github.javaparser.ast.expr.*;
import com.github.javaparser.ast.stmt.*;
import com.github.javaparser.ast.type.ClassOrInterfaceType;

import java.io.*;
import java.util.Scanner;

public class BaseFlowCompilation {

    public static void main(String[] args) throws IOException {
        //-------------FETCHING----------------------//
        //Creating an array of commands from tripleStore
        String packageName = "com.template.IOUFlow";
        String appName = "IOU";


        //-------------GENERATING----------------------//

        // Creating new Compilation Unit
        CompilationUnit compilationUnit = new CompilationUnit();
        generateStateImports(compilationUnit);
        compilationUnit.setPackageDeclaration("com.flows");

        //Define Base Abstract Class
        ClassOrInterfaceDeclaration classDeclaration = generateFlowClass(compilationUnit, appName+"BaseFlow");

        //getFirstNotary Method
        generateGetFirstNotaryMethod(classDeclaration);




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

    static ClassOrInterfaceDeclaration generateFlowClass(CompilationUnit cu, String flowName) {
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
        BufferedWriter writer = new BufferedWriter(new FileWriter("./GeneratedFiles/ContractClass.java"));
        writer.write(newFile);
        writer.close();
    }
}
