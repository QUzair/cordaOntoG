package com.contracts;

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

@BelongsToContract(IOUContract.class)
public class IOUState implements LinearState {

    public IOUState(Party lender, Party borrower, int value, UniqueIdentifier linearId) {
        this.lender = lender;
        this.borrower = borrower;
        this.value = value;
        this.linearId = linearId;
    }

    private final Party lender;

    private final Party borrower;

    private final int value;

    private final UniqueIdentifier linearId;

    public Party getLender() {
        return lender;
    }

    public Party getBorrower() {
        return borrower;
    }

    public int getValue() {
        return value;
    }

    public UniqueIdentifier getLinearId() {
        return linearId;
    }

    @Override
    public List<AbstractParty> getParticipants() {
        return Arrays.asList(lender,borrower);
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
        linearId.equals(other.getLinearid()) && value.equals(other.getValue()) && borrower.equals(other.getBorrower()) && lender.equals(other.getLender());
    }

    @Override
    public int hashCode() {
        return Objects.hash(lender, borrower, value, linearId);
    }
}
