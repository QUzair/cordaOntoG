# CordaOntoG
Auto generation Tool to build Corda Distributed Applications using Semantic Knowledge Bases.

# Set Up Database!

The Code Generator needs to point to a database containing the triple statements from which to generate the CorDapp classes.
We recommend using a database that has RDF* and SPARQL* enabled, such as Stardog: [Set Up Stardog](https://www.stardog.com/get-started/)

Sample Knowledge bases can be found at  [Sample Knowledge Bases](https://github.com/QUzair/cordaOntoG/tree/master/CorDapp%20Knowledge%20Bases)
  - IOU Contract
  - Clinical Trial Patient
  - Car Registration

# Generate Classes
To generate the classes we replace the database URL in `src/main/java/queryDB/QueryDB.java -> dbUrl` which the RDF* and SPARQL* database
Then we run the main class in `src/main/java/compilers/MainGenerator.java`.

# Run CorDapp
Once the files have been generated you should find them at the `` folder.
To simulate a Corda network locally and test the CorDapp, clone the following [Corda Network Template Repo](https://github.com/QUzair/cordapp-template-java).

## Place Generated Files
Continue to place state and contract classes in relevant folders. `cordapp-template-java/contracts/src/main/java/com/template/states` and `cordapp-template-java/contracts/src/main/java/com/template/contracts`.

Then place the Flow classes in `cordapp-template-java/workflows/src/main/java/com/template/flows`


## Running network
to run the network 
1. Clean build the project
    ```sh
    $ ./gradlew clean deployNodes
    ```
2. Deploy Nodes in a non-IDE terminal (its going ot open multiple tabs for each node in network)
    ```sh
    $ build/nodes/runnodes
    ```

If you get stuff you should be able to find more step by step solutions here [Running a CorDapp](https://docs.corda.net/docs/corda-os/4.4/hello-world-running.html). 

### Sample Commands for Example Flows
```sh
# Issue $1000 Cash to node
$ flow start net.corda.finance.flows.CashIssueFlow amount: "$1000", issuerBankPartyRef: "00", notary: "O=Notary,L=London,C=GB"

# Issue and IOU of $10 with PartyA as Borrower (Execute in PartyB terminal)
$ start IssueFlow$Initiator amount: $10, lender: "O=PartyB,L=New York,C=US", borrower: "O=PartyA,L=London,C=GB" , externalId: "1234" 


# Query Nodes Vault for IOUState
$ run vaultQuery contractStateType: com.template.states.IOUState
$ run vaultQuery contractStateType: com.template.states.<anyState defined in CorDapp>

# Settle IOU with linearId
$ flow start SettleFlow$Initiator linearId: "<get linearId from vault>", amount: "$10”, lender: "O=PartyB,L=New York,C=US"
```

