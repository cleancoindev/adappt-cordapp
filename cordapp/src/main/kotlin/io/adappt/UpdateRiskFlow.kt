/**
 *   Copyright 2019, Dapps Incorporated.
 *
 *    Licensed under the Apache License, Version 2.0 (the "License");
 *    you may not use this file except in compliance with the License.
 *    You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *    Unless required by applicable law or agreed to in writing, software
 *    distributed under the License is distributed on an "AS IS" BASIS,
 *    WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *    See the License for the specific language governing permissions and
 *    limitations under the License.
 */

package io.adappt


import co.paralleluniverse.fibers.Suspendable
import io.adappt.policy.PolicyHolderDetails
import io.adappt.policy.Risk
import net.corda.core.contracts.TransactionResolutionException
import net.corda.core.contracts.UniqueIdentifier
import net.corda.core.flows.FinalityFlow
import net.corda.core.flows.FlowLogic
import net.corda.core.flows.StartableByRPC
import net.corda.core.transactions.SignedTransaction
import net.corda.core.transactions.TransactionBuilder

/**
 * Flow to update one or more details on the Risk state.
 */


@StartableByRPC
class UpdateRiskFlow(private val accountId: UniqueIdentifier,
                     private val updatedDetails: PolicyHolderDetails) : FlowLogic<SignedTransaction>() {

    @Suspendable
    override fun call(): SignedTransaction {
        val riskStateAndRefs = serviceHub.accountExists(accountId)
        if (riskStateAndRefs.isEmpty())
            throw RiskFlowException("Unknown account id.")

        val stateAndRef = try {
            serviceHub.toStateAndRef<Risk.State>(riskStateAndRefs.first().ref)
        } catch (e: TransactionResolutionException) {
            throw RiskFlowException("Risk state could not be found.", e)
        }

        val notary = serviceHub.networkMapCache.notaryIdentities.first()
        val builder = TransactionBuilder(notary)
        val signer = Risk.generateUpdate(builder, stateAndRef, updatedDetails, notary)

        val tx = serviceHub.signInitialTransaction(builder, signer)
        val finalizedTx = subFlow(FinalityFlow(tx))

        return finalizedTx
    }
}