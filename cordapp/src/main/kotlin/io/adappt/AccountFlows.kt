package io.adappt


import io.adappt.account.Account
import co.paralleluniverse.fibers.Suspendable
import io.adappt.account.AccountContract
import io.adappt.account.AccountContract.Companion.ACCOUNT_CONTRACT_ID
import net.corda.core.contracts.Command
import net.corda.core.contracts.Requirements.using
import net.corda.core.contracts.requireThat
import net.corda.core.flows.*
import net.corda.core.identity.Party
import net.corda.core.transactions.SignedTransaction
import net.corda.core.transactions.TransactionBuilder
import net.corda.core.utilities.ProgressTracker

// *********
// * Create Account Flow *
// *********


object CreateAccountFlow {
    @Suspendable
    @InitiatingFlow
    @StartableByRPC
    class Initiator(val accountId: String,
                    val accountName: String,
                    val accountType: String,
                    val industry: String,
                    val phone: String,
                    val owner: Party) : FlowLogic<SignedTransaction>() {

        companion object {
            object GENERATING_TRANSACTION : ProgressTracker.Step("Generating transaction based on new Trade.")
            object VERIFYING_TRANSACTION : ProgressTracker.Step("Verifying contract constraints.")
            object SIGNING_TRANSACTION : ProgressTracker.Step("Signing transaction with our private key.")
            object GATHERING_SIGS : ProgressTracker.Step("Gathering the counterparty's signature.") {
                override fun childProgressTracker() = CollectSignaturesFlow.tracker()
            }

            object FINALISING_TRANSACTION : ProgressTracker.Step("Obtaining notary signature and recording transaction.") {
                override fun childProgressTracker() = FinalityFlow.tracker()
            }

            fun tracker() = ProgressTracker(
                    GENERATING_TRANSACTION,
                    VERIFYING_TRANSACTION,
                    SIGNING_TRANSACTION,
                    GATHERING_SIGS,
                    FINALISING_TRANSACTION
            )
        }

        override val progressTracker = tracker()

        @Suspendable
        override fun call(): SignedTransaction {
            // Obtain a reference to the notary we want to use.
            val notary = serviceHub.networkMapCache.notaryIdentities[0]

            // Stage 1.
            progressTracker.currentStep = GENERATING_TRANSACTION


            // Generate an unsigned transaction.
            val me = ourIdentityAndCert.party
            // Generate an unsigned transaction.
            val accountState = Account(accountId, accountName, accountType, industry, phone, serviceHub.myInfo.legalIdentities.first())
            val txCommand = Command(AccountContract.Commands.CreateAccount(), accountState.participants.map { it.owningKey })
            val txBuilder = TransactionBuilder(notary = notary)
                    .addOutputState(accountState, ACCOUNT_CONTRACT_ID)
                    .addCommand(txCommand)

            // Stage 2.
            progressTracker.currentStep = VERIFYING_TRANSACTION

            // Verify that the transaction is valid.
            txBuilder.verify(serviceHub)

            // Stage 3.
            progressTracker.currentStep = SIGNING_TRANSACTION

            val signedTx = serviceHub.signInitialTransaction(txBuilder)

            // Notarise and record the transaction in both parties' vaults.
            subFlow(FinalityFlow(signedTx))


            val partSignedTx = serviceHub.signInitialTransaction(txBuilder)

            progressTracker.currentStep = GATHERING_SIGS
            val otherPartyFlow = initiateFlow(owner)
            val fullySignedTx = subFlow(CollectSignaturesFlow(partSignedTx, setOf(otherPartyFlow), GATHERING_SIGS.childProgressTracker()))
            return subFlow(FinalityFlow(fullySignedTx, FINALISING_TRANSACTION.childProgressTracker()))
        }
    }


    @InitiatedBy(Initiator::class)
    class AccountAcceptor(val otherPartyFlow: FlowSession) : FlowLogic<SignedTransaction>() {
        @Suspendable
        override fun call(): SignedTransaction {
            val signTransactionFlow = object : SignTransactionFlow(otherPartyFlow) {
                override fun checkTransaction(stx: SignedTransaction) = requireThat {
                    val output = stx.tx.outputs.single().data
                    "This must be an Account transaction." using (output is Account)
                    val account = output as Account
                }
            }

            return subFlow(signTransactionFlow)
        }
    }
}