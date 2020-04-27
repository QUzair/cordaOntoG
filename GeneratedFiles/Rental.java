package com.template.states;

import com.template.contracts.RentalContract;
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

@BelongsToContract(RentalContract.class)
public class Rental implements LinearState {

    public Rental(String name, int age, int cardNum, String cardCompany, String licenseStatus, String recordStatus, Party rentalCompany, Party rentee, UniqueIdentifier linearId) {
        this.name = name;
        this.age = age;
        this.cardNum = cardNum;
        this.cardCompany = cardCompany;
        this.licenseStatus = licenseStatus;
        this.recordStatus = recordStatus;
        this.rentalCompany = rentalCompany;
        this.rentee = rentee;
        this.linearId = linearId;
    }

    private final String name;

    private final int age;

    private final int cardNum;

    private final String cardCompany;

    private final String licenseStatus;

    private final String recordStatus;

    private final Party rentalCompany;

    private final Party rentee;

    private final UniqueIdentifier linearId;

    public String getName() {
        return name;
    }

    public int getAge() {
        return age;
    }

    public int getCardNum() {
        return cardNum;
    }

    public String getCardCompany() {
        return cardCompany;
    }

    public String getLicenseStatus() {
        return licenseStatus;
    }

    public String getRecordStatus() {
        return recordStatus;
    }

    public Party getRentalCompany() {
        return rentalCompany;
    }

    public Party getRentee() {
        return rentee;
    }

    @Override
    public UniqueIdentifier getLinearId() {
        return linearId;
    }

    @Override
    public List<AbstractParty> getParticipants() {
        return Arrays.asList(rentalCompany,rentee);
    }

    public List<PublicKey> getParticipantKeys() {
        return getParticipants().stream().map(AbstractParty::getOwningKey).collect(Collectors.toList());
    }

    @Override()
    public boolean equals(Object obj) {
        if (!(obj instanceof Rental)) {
            return false;
        }
        Rental other = (Rental) obj;
        return linearId.equals(other.getLinearId()) && rentee.equals(other.getRentee()) && rentalCompany.equals(other.getRentalCompany()) && recordStatus.equals(other.getRecordStatus()) && licenseStatus.equals(other.getLicenseStatus()) && cardCompany.equals(other.getCardCompany()) && cardNum == other.getCardNum() && age == other.getAge() && name.equals(other.getName());
    }

    @Override
    public int hashCode() {
        return Objects.hash(name, age, cardNum, cardCompany, licenseStatus, recordStatus, rentalCompany, rentee, linearId);
    }
}
