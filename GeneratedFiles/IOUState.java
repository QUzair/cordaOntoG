package com.template.states;

import com.template.contracts.IOUContract;
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

@BelongsToContract(IOUContract.class)
public class IOUState implements LinearState {

    public IOUState(Amount<Currency> value, Party borrower, Party lender, UniqueIdentifier linearId) {
        this.value = value;
        this.borrower = borrower;
        this.lender = lender;
        this.linearId = linearId;
    }

    private final Amount<Currency> value;

    private final Party borrower;

    private final Party lender;

    private final UniqueIdentifier linearId;

    public Amount<Currency> getValue() {
        return value;
    }

    public Party getBorrower() {
        return borrower;
    }

    public Party getLender() {
        return lender;
    }

    @Override
    public UniqueIdentifier getLinearId() {
        return linearId;
    }

    @Override
    public List<AbstractParty> getParticipants() {
        return Arrays.asList(borrower,lender);
    }

    public List<PublicKey> getParticipantKeys() {
        return getParticipants().stream().map(AbstractParty::getOwningKey).collect(Collectors.toList());
    }

    @Override()
    public boolean equals(Object obj) {
        if (!(obj instanceof IOUState)) {
            return false;
        }
        IOUState other = (IOUState) obj;
        return linearId.equals(other.getLinearId()) && lender.equals(other.getLender()) && borrower.equals(other.getBorrower()) && value.equals(other.getValue());
    }

    @Override
    public int hashCode() {
        return Objects.hash(value, borrower, lender, linearId);
    }
}
