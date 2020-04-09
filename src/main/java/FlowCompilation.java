import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.NodeList;
import com.github.javaparser.ast.body.ClassOrInterfaceDeclaration;
import com.github.javaparser.ast.body.MethodDeclaration;
import com.github.javaparser.ast.body.Parameter;
import com.github.javaparser.ast.body.VariableDeclarator;
import com.github.javaparser.ast.expr.*;
import com.github.javaparser.ast.stmt.BlockStmt;
import com.github.javaparser.ast.stmt.ExpressionStmt;
import com.github.javaparser.ast.stmt.IfStmt;
import com.github.javaparser.ast.stmt.ReturnStmt;
import com.github.javaparser.ast.type.ClassOrInterfaceType;
import com.github.javaparser.ast.type.VoidType;

import java.io.*;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Scanner;

import static com.github.javaparser.ast.Modifier.Keyword.*;

public class FlowCompilation {

    public static void main(String[] args) throws Exception {

        /*
        Requires:
        - ContractName - used to declare ClassName
        - Array of commands of Contract.
        - ID of package to be stored in
        */

        //-------------FETCHING----------------------//

        //Creating an array of commands from triplestore
        String packageName = "com.template.IOUFlow";
        List<String> commands = Arrays.asList("Issue", "Transfer");
//        List<String> commands = JenaQuery.getCommands();
        String stateName = "IOUState";
        String contractName = "IOUContract";

        //-------------GENERATING----------------------//

        // Creating new Compilation Unit
        CompilationUnit compilationUnit = new CompilationUnit();
        generateStateImports(compilationUnit);
        compilationUnit.setPackageDeclaration("com.contracts");

        //Defining Contract Class
        ClassOrInterfaceDeclaration classDeclaration = generateContractClass(compilationUnit, "IOUContract");

        //Output Generated File
        System.out.println(compilationUnit.toString());
        createNewContractClassFile(compilationUnit.toString());
    }


    public static void createNewContractClassFile(String newFile) throws IOException {
        BufferedWriter writer = new BufferedWriter(new FileWriter("./GeneratedFiles/FlowClass.java"));
        writer.write(newFile);
        writer.close();
    }



    static ClassOrInterfaceDeclaration generateContractClass(CompilationUnit cu, String contractName) {
        return cu
                .addClass(contractName).setPublic(true)
                .addImplementedType("Contract");
    }

    static void generateStateImports(CompilationUnit cu) throws FileNotFoundException {

        Scanner scanner = new Scanner(new File("src/main/resources/flow_imports"));
        while (scanner.hasNextLine()) {
            String lib = scanner.nextLine();
            cu.addImport(lib);
        }
    }
}