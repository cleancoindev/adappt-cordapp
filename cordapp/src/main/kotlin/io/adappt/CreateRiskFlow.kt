package io.adappt

import co.paralleluniverse.fibers.Suspendable
import io.adappt.policy.PolicyHolderDetails
import io.adappt.policy.Risk
import net.corda.core.flows.CollectSignatureFlow
import net.corda.core.flows.FinalityFlow
import net.corda.core.flows.FlowLogic
import net.corda.core.flows.FlowSession
import net.corda.core.flows.InitiatedBy
import net.corda.core.flows.InitiatingFlow
import net.corda.core.flows.SignTransactionFlow
import net.corda.core.flows.StartableByRPC
import net.corda.core.identity.Party
import net.corda.core.transactions.SignedTransaction
import net.corda.core.transactions.TransactionBuilder

/**
 * Flow to create a Risk state. Given an owner, provider and the policy details, the broker flow
 * creates a which it signs as well as obtains the signature of the wellness provider.
 */


object CreateRiskFlow {

    @InitiatingFlow
    @StartableByRPC
    class BrokerFlow(private val agent: Party,
                     private val provider: Party,
                     private val details: PolicyHolderDetails) : FlowLogic<SignedTransaction>() {

        @Suspendable
        override fun call(): SignedTransaction {
            val notary = serviceHub.networkMapCache.notaryIdentities.first()
            val builder = TransactionBuilder(notary)
            val signer = Risk.generateCreate(builder, agent, provider, details, notary)

            val tx = serviceHub.signInitialTransaction(builder, signer)
            val providerSession = initiateFlow(provider)
            val providerSignature = subFlow(CollectSignatureFlow(tx, providerSession, provider.owningKey))
            val signedTx = tx + providerSignature

            val finalizedTx = subFlow(FinalityFlow(signedTx))

            return finalizedTx
        }

    }

    @InitiatedBy(BrokerFlow::class)
    class ProviderFlow(val brokerSession: FlowSession) : FlowLogic<SignedTransaction>() {

        @Suspendable
        override fun call(): SignedTransaction {
            val signTransactionFlow = object : SignTransactionFlow(brokerSession) {
                override fun checkTransaction(stx: SignedTransaction) {
                    val states: Iterable<Risk.State> = stx.tx.outputs.map { it.data as Risk.State }
                    states.forEach { state ->
                        state.participants.forEach { anon ->
                            require(serviceHub.identityService.wellKnownPartyFromAnonymous(anon) != null) {
                                "Transaction state $state involves unknown participant $anon"
                            }
                        }
                        require(state.provider in serviceHub.myInfo.legalIdentities) {
                            "Incorrect provider ${state.provider} on transaction state $state"
                        }
                    }
                }
            }

            val txId = subFlow(signTransactionFlow).id

            return waitForLedgerCommit(txId)
        }

    }
}