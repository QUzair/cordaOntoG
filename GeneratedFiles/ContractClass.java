package com.contracts;

import com.google.common.collect.Sets;
import net.corda.core.contracts.*;
import net.corda.core.identity.AbstractParty;
import net.corda.core.transactions.LedgerTransaction;
import net.corda.finance.contracts.asset.Cash;
import java.security.PublicKey;
import java.util.Currency;
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

    private final static String ID = "com.template.IOUContract";

    public interface Commands extends CommandData {

        class Issue extends TypeOnlyCommandData implements Commands {
        }

        class Settle extends TypeOnlyCommandData implements Commands {
        }
    }

    @Override
    public void verify(LedgerTransaction tx) {
        final CommandWithParties<Commands> command = requireSingleCommand(tx.getCommands(), Commands.class);
        final Commands commandData = command.getValue();
        final Set<PublicKey> setOfSigners = new HashSet<>(command.getSigners());
        if (commandData instanceof Commands.Issue)
            verifyIssue(tx, setOfSigners);
        else if (commandData instanceof Commands.Settle)
            verifySettle(tx, setOfSigners);
        else {
            throw new IllegalArgumentException("Unrecognised Command");
        }
    }

    private Set<PublicKey> keysFromParticipants(IOUState ioustate) {
        return ioustate.getParticipants().stream().map(AbstractParty::getOwningKey).collect(toSet());
    }

    private void verifyIssue(LedgerTransaction tx, Set<PublicKey> signers) {
        requireThat(empty req -> {
            IOUState ioustateOutput = (IOUState) tx.getoutputStates().get(0);
            req.using("A newly issued obligation must have a positive amount.", ioustateOutput.getValue().getQuantity() > 0);
            req.using("A newly issued obligation must be less than $150.", ioustateOutput.getValue().getQuantity() < 15000);
            req.using("No inputs should be consumed when issuing an obligation.", tx.getInputStates().size().equals(0));
            req.using("Only one obligation state should be created when issuing an obligation.", tx.getOutputStates().size().equals(1));
            req.using("The lender and borrower cannot be the same identity.", !ioustateOutput.getBorrower().equals(ioustateOutput.getLender()));
            return null;
        });
    }

    private void verifySettle(LedgerTransaction tx, Set<PublicKey> signers) {
        requireThat(empty req -> {
            List<IOUState> ioustateOutputs = tx.OutputsOfType(IOUState.class);
            List<Cash.State> cash = tx.outputsOfType(Cash.State.class);
            List<Cash.State> acceptableCash = cash.stream().filter(it -> it.getOwner().equals(ioustateOutput.getLender())).collect(Collectors.toList());
            req.using("There must be one input obligation.", tx.getInputStates().size().equals(1));
            req.using("There must be no output obligation as it has been fully settled.", ioustateOutputs.size().equals(0));
            req.using("The amount settled should be equal to amount in initial contract.", withoutIssuer(sumCash(acceptableCash)).getQuantity().equals(ioustateInput.getValue().getQuantity()));
            return null;
        });
    }
}
