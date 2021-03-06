package templates;

import net.bytebuddy.asm.Advice;
import net.corda.core.contracts.Amount;
import net.corda.core.contracts.BelongsToContract;
import net.corda.core.contracts.LinearState;
import net.corda.core.contracts.UniqueIdentifier;
import net.corda.core.identity.AbstractParty;
import net.corda.core.identity.Party;

import java.security.PublicKey;
import java.time.LocalDate;
import java.util.Arrays;
import java.util.Currency;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;



@BelongsToContract(IOUContract.class)
public class IOUState implements LinearState {
    private final Amount<Currency> value;
    private final Party lender;
    private final Party borrower;
    private final UniqueIdentifier linearId;
    private final LocalDate dateOfIssue;
    private final int num;


    public IOUState(Amount<Currency> value,
                    Party lender,
                    Party borrower,
                    LocalDate dateOfIssue,
                    UniqueIdentifier linearId
    )
    {
        this.value = value;
        this.lender = lender;
        this.borrower = borrower;
        this.dateOfIssue = dateOfIssue;
        this.linearId = linearId;
        this.num = 0;
    }

    public Amount<Currency> getValue() { return value; }
    public Party getLender() { return lender; }
    public Party getBorrower() { return borrower; }
    public LocalDate getDateOfIssue() {return dateOfIssue;}
    public int getNum() {return num;}
    @Override public UniqueIdentifier getLinearId() { return linearId; }
    @Override public List<AbstractParty> getParticipants() {
        return Arrays.asList(lender, borrower);
    }
    public List<PublicKey> getParticipantKeys() {
        return getParticipants().stream().map(AbstractParty::getOwningKey).collect(Collectors.toList());
    }

    @Override
    public String toString() {
        return String.format("IOUState(value=%s, lender=%s, borrower=%s, linearId=%s)", value, lender, borrower, linearId);
    }

    @Override
    public boolean equals(Object obj) {
        if (!(obj instanceof IOUState)) {
            return false;
        }
        IOUState other = (IOUState) obj;
        return value.equals(other.getValue())
                && lender.equals(other.getLender())
                && borrower.equals(other.getBorrower())
                && num==other.getNum()
                && linearId.equals(other.getLinearId());
    }

    @Override
    public int hashCode() {
        return Objects.hash(value, lender, borrower , linearId);
    }
}