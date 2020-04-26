package templates;
import com.google.common.collect.Sets;
import net.corda.core.contracts.*;
import net.corda.core.identity.AbstractParty;
import net.corda.core.transactions.LedgerTransaction;
import net.corda.finance.contracts.asset.Cash;

import java.security.PublicKey;
import java.time.LocalDate;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import static java.util.stream.Collectors.toSet;

import static net.corda.core.contracts.ContractsDSL.requireSingleCommand;
import static net.corda.core.contracts.ContractsDSL.requireThat;
import static net.corda.core.contracts.Structures.withoutIssuer;
import static net.corda.finance.contracts.utils.StateSumming.sumCash;


public class IOUContract implements Contract {
    public static final String ID = "com.example.contract.IOUContract";

    public interface Commands extends CommandData {
        class Issue extends TypeOnlyCommandData implements Commands {
        }

        class Transfer extends TypeOnlyCommandData implements Commands {
        }

        class Settle extends TypeOnlyCommandData implements Commands {
        }
    }


    private Set<PublicKey> keysFromParticipants(IOUState obligation) {
        return obligation.getParticipants().stream().map(AbstractParty::getOwningKey).collect(toSet());
    }

    @Override
    public void verify(LedgerTransaction tx) {
        final CommandWithParties<Commands> command = requireSingleCommand(tx.getCommands(), Commands.class);
        final Commands commandData = command.getValue();
        final Set<PublicKey> setOfSigners = new HashSet<>(command.getSigners());
        if (commandData instanceof Commands.Issue) {
            verifyIssue(tx, setOfSigners);
        }
        else if (commandData instanceof Commands.Settle) {
            verifySettle(tx, setOfSigners);
        }
        else {
            throw new IllegalArgumentException("Unrecognised command.");
        }
    }

    private void verifyIssue(LedgerTransaction tx, Set<PublicKey> signers) {
        requireThat(req -> {
            IOUState obligation = (IOUState) tx.getOutputStates().get(0);
            req.using(String.format("Issue Date must be before 2019-10-10. But is %s",obligation.getDateOfIssue()), obligation.getDateOfIssue().isBefore(LocalDate.parse("2019-10-10")));
            req.using("No inputs should be consumed when issuing an obligation.", tx.getInputStates().size()==0);
            req.using("Only one obligation state should be created when issuing an obligation.",tx.getOutputStates().size() == 1);
            req.using("A newly issued obligation must have a positive amount.",obligation.getValue().getQuantity() > 0);
            req.using("A newly issued obligation must be less than $150.",obligation.getValue().getQuantity() < 150*100);
            req.using("The lender and borrower cannot be the same identity.", obligation.getBorrower().equals(obligation.getLender()));
            req.using("Both lender and borrower together only may sign obligation issue transaction.", signers.equals(keysFromParticipants(obligation)));
            return null;
        });
    }

    private void verifyTransfer(LedgerTransaction tx, Set<PublicKey> signers) {
        requireThat(req -> {
            IOUState inputObligation = (IOUState) tx.getInputStates().get(0);
            IOUState outputObligation = (IOUState) tx.getOutputStates().get(0);

            req.using("An obligation transfer transaction should only consume one input state.",
                    tx.getInputs().size() == 1);
            req.using("An obligation transfer transaction should only create one output state.",
                    tx.getOutputs().size() == 1);
            req.using("The lender property must change in a transfer.", inputObligation.getLender()!=(outputObligation.getLender()));
            req.using("The borrower, old lender and new lender only must sign an obligation transfer transaction",
                    signers.equals(Sets.union(keysFromParticipants(inputObligation), keysFromParticipants(outputObligation))));
            return null;
        });
    }

    private void verifySettle(LedgerTransaction tx, Set<PublicKey> signers) {
        requireThat(req -> {

            List<IOUState> obligationInputs = tx.inputsOfType(IOUState.class);
            List<IOUState> obligationOutputs = tx.outputsOfType(IOUState.class);
            IOUState inputObligation = (IOUState) tx.getInputStates().get(0);
            IOUState outputObligation = (IOUState) tx.getOutputStates().get(0);

            List<Cash.State> cash = tx.outputsOfType(Cash.State.class);
            List<Cash.State> acceptableCash = cash.stream()
                    .filter(it -> it.getOwner().equals(inputObligation.getLender())).collect(Collectors.toList());

            req.using("There must be one input obligation.", obligationInputs.size() == 1);
            req.using("There must be output cash.", cash.size() != 0);
            req.using("There must be output cash paid to the recipient.", !acceptableCash.isEmpty());
            req.using("The amount settled should be equal to amount in initial contract.",withoutIssuer(sumCash(acceptableCash)).getQuantity()==inputObligation.getValue().getQuantity());
            req.using("There must be no output obligation as it has been fully settled.", obligationOutputs.isEmpty());
            req.using("Both lender and borrower together only may sign obligation issue transaction.", signers.equals(keysFromParticipants(inputObligation)));
            return null;
        });
    }
}