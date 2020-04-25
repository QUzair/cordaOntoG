package com.template.flows;

import co.paralleluniverse.fibers.Suspendable;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import net.corda.core.contracts.Amount;
import net.corda.core.contracts.UniqueIdentifier;
import net.corda.core.flows.*;
import net.corda.core.identity.Party;
import net.corda.core.transactions.SignedTransaction;
import net.corda.core.transactions.TransactionBuilder;
import net.corda.core.utilities.ProgressTracker;
import net.corda.core.utilities.ProgressTracker.Step;
import java.security.PublicKey;
import java.time.Duration;
import java.util.Currency;
import java.util.List;

public class SettleFlow {

    @InitiatingFlow
    @StartableByRPC
    public static class Initiator extends IOUBaseFlow {

        public Initiator(UniqueIdentifier linearId, Amount<Currency> amount, Party lender) {
            this.linearId = linearId;
            this.amount = amount;
            this.lender = lender;
        }

        private final UniqueIdentifier linearId;

        private final Amount<Currency> amount;

        private final Party lender;

        private final Step INITIALISING = new Step("Performing Initial Steps.");

        private final Step BUILDING = new Step("Building Transaction.");

        private final Step SIGNING = new Step("Signing transaction.");

        private final Step COLLECTING = new Step("Collecting counterparty signature.") {

            @Override
            public ProgressTracker childProgressTracker() {
                return CollectSignaturesFlow.Companion.tracker();
            }
        };

        private final Step FINALISING = new Step("Finalising transaction.") {

            @Override
            public ProgressTracker childProgressTracker() {
                return FinalityFlow.Companion.tracker();
            }
        };

        private final ProgressTracker progressTracker = new ProgressTracker(INITIALISING, BUILDING, SIGNING, COLLECTING, FINALISING);

        @Override
        public ProgressTracker getProgressTracker() {
            return progressTracker;
        }

        @Suspendable
        @Override
        public SignedTransaction call() throws FlowException {
            progressTracker.setCurrentStep(INITIALISING);
            final StateAndRef<IOUState> retrievedState = getIOUStateByLinearId(linearId);
            final IOUState newState = retrievedState.getState().getData();
            final List<PublicKey> requiredSigners = newState.getParticipantKeys();
            final FlowSession otherFlow = initiateFlow(lender);
            final PublicKey ourSigningKey = getOurIdentity().getOwningKey();
            progressTracker.setCurrentStep(BUILDING);
            final TransactionBuilder utx = new TransactionBuilder(getFirstNotary()).addCommand(new IOUContract.Commands.Settle(), requiredSigners).addInputState(retrievedState).setTimeWindow(getServiceHub().getClock().instant(), Duration.ofMinutes(5));
            final Amount<Currency> cashBalance = getCashBalance(getServiceHub(), amount.getToken());
            if (cashBalance.getQuantity() <= 0L) {
                throw new FlowException(String.format("Borrower has no %s to settle.", amount.getToken()));
            } else if (cashBalance.getQuantity() < amount.getQuantity()) {
                throw new FlowException(String.format("Borrower has only %s but needs %s to settle.", cashBalance, amount));
            }
            final List<PublicKey> cashSigningKeys = CashUtils.generateSpend(getServiceHub(), utx, ImmutableList.of(new PartyAndAmount<>(newState.getLender(), amount)), getOurIdentityAndCert(), ImmutableSet.of()).getSecond();
            progressTracker.setCurrentStep(SIGNING);
            utx.verify(getServiceHub());
            final List<PublicKey> signingKeys = new ImmutableList.Builder<PublicKey>().add(ourSigningKey).addAll(cashSigningKeys).build();
            final SignedTransaction ptx = getServiceHub().signInitialTransaction(utx, signingKeys);
            progressTracker.setCurrentStep(COLLECTING);
            final ImmutableSet<FlowSession> sessions = ImmutableSet.of(otherFlow);
            final SignedTransaction stx = subFlow(new CollectSignaturesFlow(ptx, sessions, signingKeys, COLLECTING.childProgressTracker()));
            progressTracker.setCurrentStep(FINALISING);
            return subFlow(new FinalityFlow(stx, sessions, FINALISING.childProgressTracker()));
        }
    }

    @InitiatedBy(Initiator.class)
    public static class Responder extends FlowLogic<SignedTransaction> {

        private final FlowSession otherFlow;

        public Responder(FlowSession otherFlow) {
            this.otherFlow = otherFlow;
        }

        @Suspendable
        @Override
        public SignedTransaction call() throws FlowException {
            final SignedTransaction stx = subFlow(new IOUBaseFlow.SignTxFlowNoChecking(otherFlow, SignTransactionFlow.Companion.tracker()));
            return subFlow(new ReceiveFinalityFlow(otherFlow, stx.getId()));
        }
    }
}
