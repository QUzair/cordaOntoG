import com.github.javaparser.ast.body.Parameter;
import com.github.javaparser.ast.body.VariableDeclarator;
import com.github.javaparser.ast.expr.*;
import com.github.javaparser.ast.stmt.ExpressionStmt;
import com.github.javaparser.ast.type.ClassOrInterfaceType;
import com.github.javaparser.ast.type.UnknownType;

public class TransactionProperties {

    public static String TX_INPUT = "input";
    public static String TX_OUTPUT = "output";

    static String camelCase(String first, String second) {
        second = second.toLowerCase();
        first = first.toLowerCase();
        return  first + second.substring(0, 1).toUpperCase() + second.substring(1);
    }

    static String camelCase(String first, String second, String third) {
        second = second.toLowerCase();
        first = first.toLowerCase();
        third = third.toLowerCase();
        return  first + second.substring(0, 1).toUpperCase() + second.substring(1) + third.substring(0, 1).toUpperCase() + third.substring(1);
    }

    // IOUState obligation = (IOUState) tx.getOutputStates().get(0);
    static ExpressionStmt singleStateType(String stateName, String stateType) {
        return new ExpressionStmt().setExpression(new VariableDeclarationExpr().addVariable(
                new VariableDeclarator()
                        .setName(camelCase(stateName,stateType))
                        .setType(stateName)
                        .setInitializer(
                                new CastExpr()
                                        .setExpression(
                                                new MethodCallExpr()
                                                        .setName("get")
                                                        .setScope(new MethodCallExpr("get"+stateType+"States").setScope(new NameExpr("tx")))
                                                        .addArgument(new IntegerLiteralExpr("0"))
                                        )
                                        .setType(new ClassOrInterfaceType().setName(stateName))
                        )
        ));
    }

    public static ExpressionStmt getCashFromOtherParty(String otherParty) {
        return new ExpressionStmt().setExpression(new VariableDeclarationExpr().addVariable(new VariableDeclarator()));
    }

    // tx.get<Input/Output>States().size()
    public static MethodCallExpr txInputOutputSize(String stateType) {
        return new MethodCallExpr("size")
                .setScope(
                        new MethodCallExpr("get"+stateType+"States")
                                .setScope(new NameExpr("tx")
                                )
                );
    }

    public static MethodCallExpr getStateProperty(String property,String stateName,String stateType) {
        return new MethodCallExpr(camelCase("get",property)).setScope(new NameExpr(camelCase(stateName,stateType)));
    }

    public static MethodCallExpr getStateProperty(String property,String stateName,String stateType,Boolean amount) throws Exception {
        if(amount) return new MethodCallExpr("getQuantity").setScope(new MethodCallExpr(camelCase("get",property)).setScope(new NameExpr(camelCase(stateName,stateType))));
        else throw new Exception();
    }

    // List<Cash.State> cash = tx.outputsOfType(Cash.State.class);
    public static ExpressionStmt getCashFromOutput() {
        return new ExpressionStmt().setExpression(new VariableDeclarationExpr().addVariable(new VariableDeclarator()
                .setInitializer(new MethodCallExpr("outputsOfType").setScope(new NameExpr("tx")).addArgument(new ClassExpr(new ClassOrInterfaceType().setName("State").setScope(new ClassOrInterfaceType().setName("Cash")))))
                .setName("cash")
                .setType(new ClassOrInterfaceType().setName("List").setTypeArguments(new ClassOrInterfaceType().setName("State").setScope(new ClassOrInterfaceType().setName("Cash"))))
        ));
    }

    public static ExpressionStmt acceptableCashFromPayee(String payee, String stateName) {
        return new ExpressionStmt()
                .setExpression(new VariableDeclarationExpr()
                        .addVariable(new VariableDeclarator().setInitializer(new MethodCallExpr().setName("collect").setScope(new MethodCallExpr("filter")
                                        .setScope(new MethodCallExpr("stream").setScope(new NameExpr("cash")))
                                        .addArgument(new LambdaExpr()
                                                .setEnclosingParameters(false)
                                                .setBody(new ExpressionStmt()
                                                        .setExpression(new MethodCallExpr("equals")
                                                                .setScope(new MethodCallExpr("getOwner").setScope(new NameExpr("it")))
                                                                .addArgument(new MethodCallExpr(camelCase("get", payee)).setScope(new NameExpr(camelCase(stateName, TX_OUTPUT))))
                                                        )
                                                )
                                                .addParameter(new Parameter().setVarArgs(false).setName("it").setType(new UnknownType()))
                                        )
                                )
                                        .addArgument(new MethodCallExpr("toList").setScope(new NameExpr("Collectors")))

                                )
                                        .setName("acceptableCash")
                                        .setType(new ClassOrInterfaceType().setName("List").setTypeArguments(new ClassOrInterfaceType().setName("State").setScope(new ClassOrInterfaceType().setName("Cash"))))
                        ));
    }

    // Sum cash being sent to us in Amount<Currency>
    public static ExpressionStmt getSumOfCashBeingSent() {
        return new ExpressionStmt().setExpression(new VariableDeclarationExpr().addVariable(new VariableDeclarator()
                .setInitializer(new MethodCallExpr("withoutIssuer").addArgument(new MethodCallExpr("sumCash").addArgument(new NameExpr("acceptableCash"))))
                .setName("sumAcceptableCash")
                .setType(new ClassOrInterfaceType().setName("Amount").setTypeArguments(new ClassOrInterfaceType().setName("Currency")))
        ));
    }

}

