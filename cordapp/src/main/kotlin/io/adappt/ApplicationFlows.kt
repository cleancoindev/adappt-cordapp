package io.adappt

import co.paralleluniverse.fibers.Suspendable
import io.adappt.application.Application
import io.adappt.application.ApplicationStatus
import io.adappt.policy.ApplicationContract
import net.corda.core.contracts.Command
import net.corda.core.contracts.Requirements.using
import net.corda.core.contracts.UniqueIdentifier
import net.corda.core.contracts.requireThat
import net.corda.core.flows.*
import net.corda.core.identity.Party
import net.corda.core.node.services.queryBy
import net.corda.core.node.services.vault.QueryCriteria
import net.corda.core.transactions.SignedTransaction
import net.corda.core.transactions.TransactionBuilder
import net.corda.core.utilities.ProgressTracker



// *********
// * Create Application Flow *
// *********



object CreateApplicationFlow {
    @StartableByRPC
    @InitiatingFlow
    @Suspendable
    class Initiator(val applicationId: String,
                    val applicationName: String,
                    val industry: String,
                    val applicationStatus: ApplicationStatus,
                    val otherParty: Party) : FlowLogic<SignedTransaction>() {

        companion object {
            object GENERATING_TRANSACTION : ProgressTracker.Step("Generating transaction based on new Agreement.")
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

        /**
         * The flow logic is encapsulated within the call() method.
         */
        @Suspendable
        override fun call(): SignedTransaction {
            // Obtain a reference to the notary we want to use.
            val notary = serviceHub.networkMapCache.notaryIdentities[0]
            progressTracker.currentStep = GENERATING_TRANSACTION

            val applicationState = Application(applicationId, applicationName, industry, applicationStatus, serviceHub.myInfo.legalIdentities.first(), otherParty)
            val txCommand = Command(ApplicationContract.Commands.CreateApplication(), applicationState.participants.map { it.owningKey })
            progressTracker.currentStep = VERIFYING_TRANSACTION
            val txBuilder = TransactionBuilder(notary)
                    .addOutputState(applicationState, APPLICATION_CONTRACT_ID)
                    .addCommand(txCommand)

            txBuilder.verify(serviceHub)
            // Sign the transaction.
            progressTracker.currentStep = SIGNING_TRANSACTION
            val partSignedTx = serviceHub.signInitialTransaction(txBuilder)


            progressTracker.currentStep = GATHERING_SIGS
            val otherPartyFlow = initiateFlow(otherParty)
            val fullySignedTx = subFlow(CollectSignaturesFlow(partSignedTx, setOf(otherPartyFlow), GATHERING_SIGS.childProgressTracker()))
            return subFlow(FinalityFlow(fullySignedTx, FINALISING_TRANSACTION.childProgressTracker()))
        }
    }

    @InitiatedBy(Initiator::class)
    class Acceptor(val otherPartyFlow: FlowSession) : FlowLogic<SignedTransaction>() {
        @Suspendable
        override fun call(): SignedTransaction {
            val signTransactionFlow = object : SignTransactionFlow(otherPartyFlow) {
                override fun checkTransaction(stx: SignedTransaction) = requireThat {
                    val output = stx.tx.outputs.single().data
                    "This must be an Application transaction." using (output is Application)
                    val application = output as Application
                    "I won't accept Application with Approved." using (application.applicationStatus != ApplicationStatus.APPROVED)
                }
            }

            return subFlow(signTransactionFlow)
        }
    }
}






// *********
// * Approve Application Flow *
// *********

@InitiatingFlow
@StartableByRPC
class ApproveApplicationFlow(val applicationId: String) : FlowLogic<SignedTransaction>() {

    override val progressTracker = ProgressTracker()

    @Suspendable
    override fun call(): SignedTransaction {

        val applicationStateAndRef = serviceHub.vaultService.queryBy<Application>().states.find {
            it.state.data.applicationId == applicationId
        } ?: throw IllegalArgumentException("No agreement with ID $applicationId found.")


        val application = applicationStateAndRef.state.data
        val applicationStatus = ApplicationStatus.APPROVED


        // Creating the output.
        val approvedApplication = Application(
                application.applicationId,
                application.applicationName,
                application.industry,
                applicationStatus,
                application.agent,
                application.provider,
                application.linearId)

        // Building the transaction.
        val notary = applicationStateAndRef.state.notary
        val txBuilder = TransactionBuilder(notary)
        txBuilder.addInputState(applicationStateAndRef)
        txBuilder.addOutputState(approvedApplication, ApplicationContract.APPLICATION_CONTRACT_ID)
        txBuilder.addCommand(ApplicationContract.Commands.ApproveApplication(), ourIdentity.owningKey)
        txBuilder.verify(serviceHub)

        val stx = serviceHub.signInitialTransaction(txBuilder)
        return subFlow(FinalityFlow(stx))
    }
}




// *********
// * Reject Application Flow *
// *********


@InitiatingFlow
@StartableByRPC
class RejectApplicationFlow(val applicationId: String) : FlowLogic<SignedTransaction>() {

    override val progressTracker = ProgressTracker()

    @Suspendable
    override fun call(): SignedTransaction {

        val applicationStateAndRef = serviceHub.vaultService.queryBy<Application>().states.find {
            it.state.data.applicationId == applicationId
        } ?: throw IllegalArgumentException("No agreement with ID $applicationId found.")


        val application = applicationStateAndRef.state.data
        val applicationStatus = ApplicationStatus.REJECTED

        // Creating the output.
        val rejectedApplication = Application(
                application.applicationId,
                application.applicationName,
                application.industry,
                applicationStatus,
                application.agent,
                application.provider,
                application.linearId)

        // Building the transaction.
        val notary = applicationStateAndRef.state.notary
        val txBuilder = TransactionBuilder(notary)
        txBuilder.addInputState(applicationStateAndRef)
        txBuilder.addOutputState(rejectedApplication, ApplicationContract.APPLICATION_CONTRACT_ID)
        txBuilder.addCommand(ApplicationContract.Commands.RejectApplication(), ourIdentity.owningKey)
        txBuilder.verify(serviceHub)

        val stx = serviceHub.signInitialTransaction(txBuilder)
        return subFlow(FinalityFlow(stx))
    }
}


const val APPLICATION_CONTRACT_ID = "io.adappt.ApplicationContract"