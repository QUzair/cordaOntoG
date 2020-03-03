import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.body.ClassOrInterfaceDeclaration;

import java.io.File;
import java.io.FileNotFoundException;
import java.util.Scanner;

import static com.github.javaparser.ast.Modifier.Keyword.*;

public class ContractCompilation {

    public static void main(String[] args) throws Exception {
        // Creating new Compilation Unit
        CompilationUnit compilationUnit = new CompilationUnit();
        generateStateImports(compilationUnit);
        compilationUnit.setPackageDeclaration("com.contracts");

        ClassOrInterfaceDeclaration classDeclaration = generateContractClass(compilationUnit, "IOUContract");

        generateContractIdField(classDeclaration);
        System.out.println(compilationUnit.toString());
    }

    static void generateContractIdField(ClassOrInterfaceDeclaration cd) {
        cd.addField("String","ID",PRIVATE,FINAL,STATIC);

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
