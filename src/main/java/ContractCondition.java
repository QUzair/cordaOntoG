import com.github.javaparser.ast.expr.BinaryExpr;
import com.github.javaparser.ast.expr.IntegerLiteralExpr;
import com.github.javaparser.ast.expr.MethodCallExpr;

public class ContractCondition {
    BinaryExpr.Operator operator;
    MethodCallExpr left;
    MethodCallExpr right;
    IntegerLiteralExpr rightInt;
    String description;

    public ContractCondition(String description,MethodCallExpr left,  BinaryExpr.Operator operator, MethodCallExpr right) {
        this.operator = operator;
        this.left = left;
        this.right = right;
        this.description = description;
    }

    public ContractCondition(String description,MethodCallExpr left,  BinaryExpr.Operator operator, IntegerLiteralExpr right) {
        this.operator = operator;
        this.left = left;
        this.rightInt = right;
        this.description = description;
    }

    public BinaryExpr.Operator getOperator() {
        return operator;
    }

    public MethodCallExpr getLeft() {
        return left;
    }

    public MethodCallExpr getRight() {
        return right;
    }

    public String getDescription() {
        return description;
    }
}
