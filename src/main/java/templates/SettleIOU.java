package templates;

import co.paralleluniverse.fibers.Suspendable;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import net.corda.core.contracts.Amount;
import net.corda.core.contracts.StateAndRef;
import net.corda.core.contracts.UniqueIdentifier;
import net.corda.core.flows.*;
import net.corda.core.identity.Party;
import net.corda.core.transactions.SignedTransaction;
import net.corda.core.transactions.TransactionBuilder;
import net.corda.core.utilities.ProgressTracker;
import net.corda.core.utilities.ProgressTracker.Step;

import net.corda.finance.contracts.asset.PartyAndAmount;
import net.corda.finance.workflows.asset.CashUtils;

import java.security.PublicKey;
import java.util.Currency;
import java.util.List;

import static net.corda.finance.workflows.GetBalances.getCashBalance;

public class SettleIOU {

    @InitiatingFlow
    @StartableByRPC
    public static class Initiator extends IOUBaseFlow {
        private final UniqueIdentifier linearId;
        private final Amount<Currency> amount;
        private final Party lender;

        private final Step INITIALISING = new Step("Performing initial steps.");
        private final Step BUILDING = new Step("Building and verifying transaction.");
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

        private final ProgressTracker progressTracker = new ProgressTracker(
                INITIALISING, BUILDING, SIGNING, COLLECTING, FINALISING
        );

        public Initiator(UniqueIdentifier linearId, Amount<Currency> amount, Party lender) {
            this.linearId = linearId;
            this.amount = amount;
            this.lender = lender;
        }

        @Override
        public ProgressTracker getProgressTracker() {
            return progressTracker;
        }

        @Suspendable
        @Override
        public SignedTransaction call() throws FlowException {

            // Stage 1. Retrieve obligation specified by linearId from the vault.
            progressTracker.setCurrentStep(INITIALISING);
            final StateAndRef<IOUState> obligationToSettle = getObligationByLinearId(linearId);
            final IOUState inputObligation = obligationToSettle.getState().getData();
            final FlowSession session = initiateFlow(lender);

            if (!inputObligation.getBorrower().equals(getOurIdentity())) {
                throw new FlowException(String.format("Settle Obligation flow must be initiated by the borrower. %s %s",inputObligation.getBorrower().toString(),getOurIdentity().toString()));
            }

            // Stage 4. Check we have enough cash to settle the requested amount.
            final Amount<Currency> cashBalance = getCashBalance(getServiceHub(), amount.getToken());
            if (cashBalance.getQuantity() <= 0L) {
                throw new FlowException(String.format("Borrower has no %s to settle.", amount.getToken()));
            } else if (cashBalance.getQuantity() < amount.getQuantity()) {
                throw new FlowException(String.format(
                        "Borrower has only %s but needs %s to settle.", cashBalance, amount));
            }


            // Step 2. Building.
            progressTracker.setCurrentStep(BUILDING);
            final List<PublicKey> requiredSigners = inputObligation.getParticipantKeys();

            final TransactionBuilder builder = new TransactionBuilder(getFirstNotary())
                    .addInputState(obligationToSettle)
                    .addCommand(new IOUContract.Commands.Settle(), requiredSigners);


            // Stage 7. Get some cash from the vault and add a spend to our transaction builder.
            final List<PublicKey> cashSigningKeys = CashUtils.generateSpend(
                    getServiceHub(),
                    builder,
                    ImmutableList.of(new PartyAndAmount<>(inputObligation.getLender(), amount)),
                    getOurIdentityAndCert(),
                    ImmutableSet.of()).getSecond();


            // Stage 9. Verify and sign the transaction.
            progressTracker.setCurrentStep(SIGNING);
            builder.verify(getServiceHub());
            final List<PublicKey> signingKeys = new ImmutableList.Builder<PublicKey>()
                    .addAll(cashSigningKeys)
                    .add(getOurIdentity().getOwningKey())
                    .build();
            final SignedTransaction ptx = getServiceHub().signInitialTransaction(builder, signingKeys);

            // Stage 10. Get CounterParty signature.
            progressTracker.setCurrentStep(COLLECTING);
            final ImmutableSet<FlowSession> sessions = ImmutableSet.of(session);
            final SignedTransaction stx = subFlow(new CollectSignaturesFlow(
                    ptx,
                    sessions,
                    signingKeys,
                    COLLECTING.childProgressTracker()));

            // Stage 11. Finalize the transaction.
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
            SignedTransaction stx = subFlow(new IOUBaseFlow.SignTxFlowNoChecking(otherFlow, SignTransactionFlow.Companion.tracker()));
            return subFlow(new ReceiveFinalityFlow(otherFlow, stx.getId()));
        }
    }
}