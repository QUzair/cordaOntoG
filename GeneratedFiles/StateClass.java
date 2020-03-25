package com.contracts;

import net.corda.core.contracts.ContractState;
import net.corda.core.identity.AbstractParty;
import java.util.Arrays;
import java.util.List;
import net.corda.core.identity.Party;

public class IOUState implements ContractState {

    public IOUState(Party lender, int value, Party hooga) {
        this.lender = lender;
        this.value = value;
        this.hooga = hooga;
    }

    private final Party lender;

    private final int value;

    private final Party hooga;

    public Party getLender() {
        return lender;
    }

    public int getValue() {
        return value;
    }

    public Party getHooga() {
        return hooga;
    }

    @Override
    public List<AbstractParty> getParticipants() {
        return Arrays.asList(lender,hooga);
    }
}
