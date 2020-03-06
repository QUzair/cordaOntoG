package com.contracts;

import net.corda.core.contracts.ContractState;
import net.corda.core.identity.AbstractParty;
import java.util.Arrays;
import java.util.List;
import net.corda.core.identity.Party;

public class IOUState implements ContractState {

    public IOUState(Party lender, Party borrower, int value) {
        this.lender = lender;
        this.borrower = borrower;
        this.value = value;
    }

    private final Party lender;

    private final Party borrower;

    private final int value;

    public Party getLender() {
        return lender;
    }

    public Party getBorrower() {
        return borrower;
    }

    public int getValue() {
        return value;
    }

    @Override
    public List<AbstractParty> getParticipants() {
        return Arrays.asList(lender,borrower);
    }
}
