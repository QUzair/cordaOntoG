package com.template.contracts;

import com.template.states.Rental;
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
import java.time.LocalDate;

public class RentalContract implements Contract {

    public final static String ID = "com.template.contracts.RentalContract";

    public interface Commands extends CommandData {

        class Register extends TypeOnlyCommandData implements Commands {
        }
    }

    @Override
    public void verify(LedgerTransaction tx) {
        final CommandWithParties<Commands> command = requireSingleCommand(tx.getCommands(), Commands.class);
        final Commands commandData = command.getValue();
        final Set<PublicKey> setOfSigners = new HashSet<>(command.getSigners());
        if (commandData instanceof Commands.Register)
            verifyRegister(tx, setOfSigners);
        else {
            throw new IllegalArgumentException("Unrecognised Command");
        }
    }

    private Set<PublicKey> keysFromParticipants(Rental rental) {
        return rental.getParticipants().stream().map(AbstractParty::getOwningKey).collect(toSet());
    }

    private void verifyRegister(LedgerTransaction tx, Set<PublicKey> signers) {
        requireThat(req -> {
            Rental rentalOutput = (Rental) tx.getOutputStates().get(0);
            req.using("No inputs should be consumed when renting a new rental.", tx.getInputStates().size() == 0);
            req.using("Record Status must be clean.", rentalOutput.getRecordStatus().equals("clean"));
            req.using("Only one rental state should be created when renting.", tx.getOutputStates().size() == 1);
            req.using("Card Company should be Visa.", rentalOutput.getCardCompany().equals("Visa"));
            req.using("Age of rentee must be greater than 25.", rentalOutput.getAge() > 25);
            req.using("License Status should be valid.", rentalOutput.getLicenseStatus().equals("valid"));
            return null;
        });
    }
}
