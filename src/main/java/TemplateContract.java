
import net.corda.core.contracts.CommandData;
import net.corda.core.contracts.CommandWithParties;
import net.corda.core.contracts.Contract;
import net.corda.core.contracts.TypeOnlyCommandData;
import net.corda.core.identity.AbstractParty;
import net.corda.core.transactions.LedgerTransaction;

import java.security.PublicKey;
import java.util.HashSet;
import java.util.Set;

import static java.util.stream.Collectors.toSet;
import static net.corda.core.contracts.ContractsDSL.requireSingleCommand;
import static net.corda.core.contracts.ContractsDSL.requireThat;


public class TemplateContract implements Contract {
    public static final String ID = "com.template.IOUContract";

    // Our Create command.
    public interface Commands extends CommandData {
        class Issue extends TypeOnlyCommandData implements Commands {
        }

        class Transfer extends TypeOnlyCommandData implements Commands {
        }
    }

    @Override
    public void verify(LedgerTransaction tx) {

        final CommandWithParties<Commands> command = requireSingleCommand(tx.getCommands(), Commands.class);
        final Commands commandData = command.getValue();
        final Set<PublicKey> setOfSigners = new HashSet<>(command.getSigners());

        if (commandData instanceof Commands.Issue) {
            verifyIssue(tx, setOfSigners);
        } else if (commandData instanceof Commands.Transfer) {
            verifyTransfer(tx, setOfSigners);
        }
    }

    private Set<PublicKey> keysFromParticipants(TemplateState iouState) {
        return iouState
                .getParticipants().stream()
                .map(AbstractParty::getOwningKey)
                .collect(toSet());
    }


    private void verifyIssue(LedgerTransaction tx, Set<PublicKey> signers) {

        requireThat(req -> {
            TemplateState iouState = (TemplateState) tx.getOutputStates().get(0);
            req.using("No inputs should be consumed when issuing an obligation.", tx.getInputStates().isEmpty());
            req.using("Only one obligation state should be created when issuing an obligation.", tx.getOutputStates().size() == 1);
            req.using("A newly issued obligation must have a positive amount.", iouState.getValue() > 0);
            req.using("The lender and borrower cannot be the same identity.", !iouState.getBorrower().equals(iouState.getLender()));
            req.using("Both lender and borrower together only may sign obligation issue transaction.", signers.equals(keysFromParticipants(iouState)));
            return null;
        });
    }

    private void verifyTransfer(LedgerTransaction tx, Set<PublicKey> signers) {

    }
}