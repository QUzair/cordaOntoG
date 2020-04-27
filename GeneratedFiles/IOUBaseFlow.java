package com.template.flows;

import com.template.states.IOUState;
import com.google.common.collect.ImmutableList;
import net.corda.core.contracts.StateAndRef;
import net.corda.core.contracts.UniqueIdentifier;
import net.corda.core.flows.FlowException;
import net.corda.core.flows.FlowLogic;
import net.corda.core.flows.FlowSession;
import net.corda.core.flows.SignTransactionFlow;
import net.corda.core.identity.Party;
import net.corda.core.node.services.Vault;
import net.corda.core.node.services.vault.QueryCriteria;
import net.corda.core.transactions.SignedTransaction;
import net.corda.core.utilities.ProgressTracker;
import java.util.List;
import java.time.LocalDate;

abstract class IOUBaseFlow extends FlowLogic<SignedTransaction> {

    Party getFirstNotary() throws FlowException {
        List<Party> notaries = getServiceHub().getNetworkMapCache().getNotaryIdentities();
        if (notaries.isEmpty()) {
            throw new FlowException("No available Notary.");
        }
        return notaries.get(0);
    }

    StateAndRef<IOUState> getIOUStateByLinearId(UniqueIdentifier linearId) throws FlowException {
        QueryCriteria queryCriteria = new QueryCriteria.LinearStateQueryCriteria(null, ImmutableList.of(linearId), Vault.StateStatus.UNCONSUMED, null);
        List<StateAndRef<IOUState>> ioustate = getServiceHub().getVaultService().queryBy(IOUState.class, queryCriteria).getStates();
        if (ioustate.size() != 1) {
            throw new FlowException(String.format("IOUState with id %s not found.", linearId));
        }
        return ioustate.get(0);
    }

    static class SignTxFlowNoChecking extends SignTransactionFlow {

        SignTxFlowNoChecking(FlowSession otherFlow, ProgressTracker progressTracker) {
            super(otherFlow, progressTracker);
        }

        @Override
        protected void checkTransaction(SignedTransaction tx) {
        }
    }
}
