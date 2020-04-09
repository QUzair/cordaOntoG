
import com.github.javaparser.StaticJavaParser;
import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.body.ClassOrInterfaceDeclaration;
import com.github.javaparser.ast.body.MethodDeclaration;
import com.github.javaparser.ast.visitor.VoidVisitor;
import com.github.javaparser.ast.visitor.VoidVisitorAdapter;
import com.github.javaparser.printer.DotPrinter;
import com.github.javaparser.printer.YamlPrinter;

import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.List;

public class VoidVisitorStarter {

    private static final String FILE_PATH = "src/main/java/templates/IOUState.java";
    private static final String AST_YAML_FILE = "src/main/resources/AST_Vis_Base_State.yaml";


    public static void main(String[] args) throws Exception {

        CompilationUnit cu = StaticJavaParser.parse(new FileInputStream(FILE_PATH));
        YamlPrinter printer = new YamlPrinter(true);
        //System.out.println(printer.output(cu));

        try (FileOutputStream fos = new FileOutputStream(AST_YAML_FILE)) {
            fos.write(printer.output(cu).getBytes());
        }

        DotPrinter dotPrinter = new DotPrinter(true);
        try (FileWriter fileWriter = new FileWriter("src/main/resources/ast_contract.dot");
             PrintWriter printWriter = new PrintWriter(fileWriter)) {
            printWriter.print(dotPrinter.output(cu));
        }

        VoidVisitor<?> classNameVisitor= new ClassNamePrinter();
        classNameVisitor.visit(cu,null);
    }

    private static class ClassNamePrinter extends VoidVisitorAdapter<Void> {

        @Override
        public void visit(ClassOrInterfaceDeclaration cd, Void arg) {
            super.visit(cd, arg);
            System.out.println("Class Name Printed: " + cd.getName());
        }
    }
}
