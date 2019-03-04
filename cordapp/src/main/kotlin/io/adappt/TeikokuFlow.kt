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
import io.adappt.policy.Teikoku
import io.adappt.policy.TeikokuContract
import it.oraclize.cordapi.OraclizeUtils
import it.oraclize.cordapi.entities.Answer
import it.oraclize.cordapi.entities.ProofType
import it.oraclize.cordapi.flows.OraclizeQueryAwaitFlow
import it.oraclize.cordapi.flows.OraclizeSignFlow
import net.corda.core.contracts.*
import net.corda.core.flows.*
import net.corda.core.identity.AbstractParty
import net.corda.core.identity.Party
import net.corda.core.transactions.LedgerTransaction
import net.corda.core.transactions.SignedTransaction
import net.corda.core.transactions.TransactionBuilder
import net.corda.core.utilities.ProgressTracker
import java.util.function.Predicate
import javax.xml.transform.TransformerConfigurationException


@StartableByRPC
@InitiatingFlow
class TeikokuFlow : FlowLogic<SignedTransaction>() {

    companion object {
        object QUERYING_ORACLIZE : ProgressTracker.Step("Querying Oraclize")
        object VERIFYING_PROOF : ProgressTracker.Step("Verifying the proof")
        object CREATING_TRANSACTION : ProgressTracker.Step("Creating the transaction")
        object GATHERING_SIGNATURES : ProgressTracker.Step("Gathering signatures")
        object FINALIZING_TRANSACTION : ProgressTracker.Step("Finalizing transaction")

        fun tracker() = ProgressTracker(QUERYING_ORACLIZE, VERIFYING_PROOF,
                CREATING_TRANSACTION, GATHERING_SIGNATURES, FINALIZING_TRANSACTION)
    }

    override val progressTracker = tracker()

    @Suspendable
    override fun call() : SignedTransaction{
        progressTracker.currentStep = QUERYING_ORACLIZE
        val query = "json(https://www.tdb.co.jp/english/services/db_service/28.html).result.0.last"
        val answer = subFlow(OraclizeQueryAwaitFlow("URL", query, ProofType.TLSNOTARY, 0))

        progressTracker.currentStep = VERIFYING_PROOF
        val pvt = OraclizeUtils.ProofVerificationTool()

        if (!pvt.verifyProof(answer.proof!!))
            throw FlowException("The proof is not valid")

        progressTracker.currentStep = CREATING_TRANSACTION
        val notary = serviceHub.networkMapCache.notaryIdentities.first()
        val oracle = OraclizeUtils.getPartyNode(serviceHub)

        val builder = TransactionBuilder(notary)
        builder.addCommand(Command(answer, listOf(oracle.owningKey, ourIdentity.owningKey)))
        //builder.addOutputState(Teikoku( ourIdentity), TeikokuContract.TEIKOKU_CONTRACT_ID)

        builder.verify(serviceHub)

        progressTracker.currentStep = GATHERING_SIGNATURES
        val onceSigned = serviceHub.signInitialTransaction(builder)

        val filtering = OraclizeUtils()::filtering
        val ftx = builder.toWireTransaction(serviceHub)
                .buildFilteredTransaction(Predicate { filtering(oracle.owningKey, it) })

        val oracleSignature = subFlow(OraclizeSignFlow(ftx))

        val fullSigned = onceSigned + oracleSignature

        progressTracker.currentStep = FINALIZING_TRANSACTION

        return subFlow(FinalityFlow(fullSigned))
    }
}