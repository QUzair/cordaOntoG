package com.template.flows;

import com.template.states.Rental;
import com.template.contract.RentalContract;
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
import java.time.LocalDate;

public class Register {

    @InitiatingFlow
    @StartableByRPC
    public static class Initiator extends CarRentalBaseFlow {

        public Initiator(int age, String externalId, String recordStatus, Party rentee, String name, String cardCompany, Party rentalCompany, String licenseStatus, int cardNum) {
            this.age = age;
            this.externalId = externalId;
            this.recordStatus = recordStatus;
            this.rentee = rentee;
            this.name = name;
            this.cardCompany = cardCompany;
            this.rentalCompany = rentalCompany;
            this.licenseStatus = licenseStatus;
            this.cardNum = cardNum;
        }

        private final int age;

        private final String externalId;

        private final String recordStatus;

        private final Party rentee;

        private final String name;

        private final String cardCompany;

        private final Party rentalCompany;

        private final String licenseStatus;

        private final int cardNum;

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
            final Rental newState = new Rental(name, age, cardNum, cardCompany, licenseStatus, recordStatus, rentalCompany, rentee, new UniqueIdentifier(externalId));
            final List<PublicKey> requiredSigners = newState.getParticipantKeys();
            final FlowSession otherFlow = initiateFlow(rentalCompany);
            final PublicKey ourSigningKey = getOurIdentity().getOwningKey();
            progressTracker.setCurrentStep(BUILDING);
            final TransactionBuilder utx = new TransactionBuilder(getFirstNotary()).addCommand(new RentalContract.Commands.Register(), requiredSigners).setTimeWindow(getServiceHub().getClock().instant(), Duration.ofMinutes(5)).addOutputState(newState, RentalContract.ID);
            progressTracker.setCurrentStep(SIGNING);
            utx.verify(getServiceHub());
            final List<PublicKey> signingKeys = new ImmutableList.Builder<PublicKey>().add(ourSigningKey).build();
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
            final SignedTransaction stx = subFlow(new CarRentalBaseFlow.SignTxFlowNoChecking(otherFlow, SignTransactionFlow.Companion.tracker()));
            return subFlow(new ReceiveFinalityFlow(otherFlow, stx.getId()));
        }
    }
}
