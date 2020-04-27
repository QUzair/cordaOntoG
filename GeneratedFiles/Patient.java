package com.template.states;

import com.template.contracts.PatientContract;
import net.corda.core.contracts.Amount;
import net.corda.core.contracts.BelongsToContract;
import net.corda.core.contracts.LinearState;
import net.corda.core.contracts.UniqueIdentifier;
import net.corda.core.identity.AbstractParty;
import net.corda.core.identity.Party;
import java.security.PublicKey;
import java.util.Arrays;
import java.util.Currency;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;
import java.time.LocalDate;

@BelongsToContract(PatientContract.class)
public class Patient implements LinearState {

    public Patient(String gender, int age, int snqscore, int visit, LocalDate visitDate, Party regulator, Party investigator, UniqueIdentifier linearId) {
        this.gender = gender;
        this.age = age;
        this.snqscore = snqscore;
        this.visit = visit;
        this.visitDate = visitDate;
        this.regulator = regulator;
        this.investigator = investigator;
        this.linearId = linearId;
    }

    private final String gender;

    private final int age;

    private final int snqscore;

    private final int visit;

    private final LocalDate visitDate;

    private final Party regulator;

    private final Party investigator;

    private final UniqueIdentifier linearId;

    public String getGender() {
        return gender;
    }

    public int getAge() {
        return age;
    }

    public int getSnqscore() {
        return snqscore;
    }

    public int getVisit() {
        return visit;
    }

    public LocalDate getVisitDate() {
        return visitDate;
    }

    public Party getRegulator() {
        return regulator;
    }

    public Party getInvestigator() {
        return investigator;
    }

    @Override
    public UniqueIdentifier getLinearId() {
        return linearId;
    }

    @Override
    public List<AbstractParty> getParticipants() {
        return Arrays.asList(regulator,investigator);
    }

    public List<PublicKey> getParticipantKeys() {
        return getParticipants().stream().map(AbstractParty::getOwningKey).collect(Collectors.toList());
    }

    @Override()
    public boolean equals(Object obj) {
        if (!(obj instanceof Patient)) {
            return false;
        }
        Patient other = (Patient) obj;
        return linearId.equals(other.getLinearId()) && investigator.equals(other.getInvestigator()) && regulator.equals(other.getRegulator()) && visitDate.equals(other.getVisitDate()) && visit == other.getVisit() && snqscore == other.getSnqscore() && age == other.getAge() && gender.equals(other.getGender());
    }

    @Override
    public int hashCode() {
        return Objects.hash(gender, age, snqscore, visit, visitDate, regulator, investigator, linearId);
    }
}
