import com.github.javaparser.ast.stmt.ExpressionStmt;

import java.util.ArrayList;
import java.util.List;

public class CommandConstraints {
    public String commandName;
    public List<ContractCondition> constraints;
    public List<ExpressionStmt> variables;

    public CommandConstraints(String commandName) {
        this.commandName = commandName;
        this.constraints = new ArrayList<>();
        this.variables = new ArrayList<>();
    }

    public CommandConstraints(String commandName, List<ContractCondition> constraints) {
        this.commandName = commandName;
        this.constraints = constraints;
        this.variables = new ArrayList<>();
    }

    public CommandConstraints(String commandName, List<ContractCondition> constraints, List<ExpressionStmt> variables) {
        this.commandName = commandName;
        this.constraints = new ArrayList<>();
        this.variables =variables;
    }
}
