package com.contracts;

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

public class IOUContract implements Contract {

    private final static String ID = "com.template.IOUContract";

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
        if (commandData instanceof Commands.Issue)
            verifyIssue(tx, setOfSigners);
        if (commandData instanceof Commands.Transfer)
            verifyTransfer(tx, setOfSigners);
    }

    private Set<PublicKey> keysFromParticipants(IOUState iouState) {
        return iouState.getParticipants().stream().map(AbstractParty::getOwningKey).collect(toSet());
    }

    private void verifyIssue(LedgerTransaction tx, Set<PublicKey> signers) {
        requireThat(empty req -> {
            TemplateState iouState = (TemplateState) tx.getOutputStates().get(0);
            req.using("No inputs to be consumed", tx.getInputStates().isEmpty());
        });
    }
}
