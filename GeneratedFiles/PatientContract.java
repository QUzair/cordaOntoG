package com.template.contracts;

import com.template.states.Patient;
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

public class PatientContract implements Contract {

    public final static String ID = "com.template.contracts.PatientContract";

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

    private Set<PublicKey> keysFromParticipants(Patient patient) {
        return patient.getParticipants().stream().map(AbstractParty::getOwningKey).collect(toSet());
    }

    private void verifyRegister(LedgerTransaction tx, Set<PublicKey> signers) {
        requireThat(req -> {
            Patient patientOutput = (Patient) tx.getOutputStates().get(0);
            req.using("Patient Age should be greater than or equal to 6.", patientOutput.getAge() >= 6);
            req.using("SNQ must have been conducted after February 1st 2010.", patientOutput.getVisitDate().isAfter(LocalDate.parse("2010-02-01")));
            req.using("Only one clinical trial patient state should be created during registration.", tx.getOutputStates().size() == 1);
            req.using("No inputs should be consumed when registering a new clinical trial patient.", tx.getInputStates().size() == 0);
            req.using("SNQ Score must be greater than or equals to 1.", patientOutput.getSnqscore() >= 1);
            req.using("Gender Must be Female.", patientOutput.getGender().equals("Female"));
            req.using("SNQ must have been conducted before February 15th 2010.", patientOutput.getVisitDate().isBefore(LocalDate.parse("2010-02-15")));
            req.using("Should be patients first Visit.", patientOutput.getVisit() == 1);
            return null;
        });
    }
}
