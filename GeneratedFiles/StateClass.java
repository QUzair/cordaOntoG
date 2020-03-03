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

    public int getvalue() {
        return value;
    }

    public Party getlender() {
        return lender;
    }

    public Party getborrower() {
        return borrower;
    }

    @Override
    public List<AbstractParty> getParticipants() {
        return Arrays.asList(lender,borrower);
    }
}
