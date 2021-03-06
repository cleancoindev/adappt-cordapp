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

package io.adappt.agreement

import net.corda.core.contracts.*
import net.corda.core.contracts.Requirements.using
import net.corda.core.identity.AbstractParty
import net.corda.core.identity.Party
import net.corda.core.serialization.CordaSerializable
import net.corda.core.transactions.LedgerTransaction
import java.lang.IllegalArgumentException

// *****************
// * Agreement State *
// *****************


data class Agreement(val agreementNumber: String,
                     val agreementName: String,
                     val agreementStatus: AgreementStatus,
                     val agreementType: AgreementType,
                     val totalAgreementValue: Int,
                     val party: Party,
                     val counterparty: Party,
        //val agreementStartDate: String,
        //val agreementEndDate: String,
        //val agreementLineItem: AgreementLineItem,
        //val attachmentId: SecureHash.SHA256,
        //val active: Boolean,
        //val createdAt: String,
        //val lastUpdated: String,
                     override val linearId: UniqueIdentifier = UniqueIdentifier()) : LinearState, ContractState {

    override val participants: List<AbstractParty> get() = listOf(party, counterparty)

    constructor(
            agreementNumber: String,
            agreementName: String,
            agreementStatus: AgreementStatus,
            agreementType: AgreementType,
            totalAgreementValue: Int,
            party: Party,
            counterparty: Party,
            //agreementStartDate: String,
            //agreementEndDate: String,
            //agreementLineItem: AgreementLineItem,
            //attachmentId: SecureHash.SHA256,
            //active: Boolean,
            //createdAt: String,
            //lastUpdated: String,
            linearId: String
    ) :  // this(agreementNumber, agreementName, agreementStatus, agreementType, totalAgreementValue, party, counterparty, agreementStartDate, agreementEndDate, agreementLineItem, attachmentId,  active, createdAt, lastUpdated, UniqueIdentifier(linearId))
            this(agreementNumber, agreementName, agreementStatus, agreementType, totalAgreementValue, party, counterparty, UniqueIdentifier(linearId))
    /**
    override fun generatedMappedObject(schema: MappedSchema): PersistentState {
    return when (schema) {
    is AgreementSchemaV1 -> AgreementSchemaV1.PersistentAgreement(
    agreementNumber = agreementNumber,
    agreementName = agreementName,
    agreementStatus = agreementStatus.toString(),
    agreementType = agreementType.toString(),
    totalAgreementValue = totalAgreementValue.toString(),
    party = party.name.toString(),
    counterparty = counterparty.name.toString(),
    agreementStartDate = agreementStartDate.toString(),
    agreementEndDate = agreementEndDate.toString(),
    active = active.toString(),
    createdAt = createdAt.toString(),
    lastUpdated = lastUpdated.toString(),
    linearId = linearId.id.toString(),
    externalID = linearId.id.toString()
    )
    else -> throw IllegalArgumentException("Unrecognized schema $schema")
    }
    }

    override fun supportedSchemas(): Iterable<MappedSchema> = listOf(AgreementSchemaV1)
    }

     */
}

@CordaSerializable
enum class AgreementStatus {
    REQUEST, APPROVAL_REQUIRED, APPROVED, IN_REVIEW, ACTIVATED, INEFFECT, REJECTED, RENEWED, TERMINATED, AMENDED, SUPERSEDED, EXPIRED
}




@CordaSerializable
enum class AgreementType {
    NDA, MSA, SLA, SOW
}



// **********************
// * Agreement Contract *
// **********************

class AgreementContract : Contract {
    // This is used to identify our contract when building a transaction
    companion object {
        val AGREEMENT_CONTRACT_ID = AgreementContract::class.java.canonicalName
    }

    // Used to indicate the transaction's intent.
    interface Commands : CommandData {

        class CreateAgreement : Commands
        class ActivateAgreement : Commands
        class ReviewAgreement : Commands
        class RenewAgreement : Commands
        class TerminateAgreement : Commands
        class ExpireAgreement : Commands
        class AmendAgreement : Commands


    }


    // A transaction is considered valid if the verify() function of the contract of each of the transaction's input
    // and output states does not throw an exception.
    override fun verify(tx: LedgerTransaction) {
        val agreementInputs = tx.inputsOfType<Agreement>()
        val agreementOutputs = tx.outputsOfType<Agreement>()
        val agreementCommand = tx.commandsOfType<AgreementContract.Commands>().single()

        when(agreementCommand.value) {
            is Commands.CreateAgreement -> requireThat {
                "no inputs should be consumed" using (agreementInputs.isEmpty())
                // TODO we might allow several jobs to be proposed at once later
                "one output should be produced" using (agreementOutputs.size == 1)

                val agreementOutput = agreementOutputs.single()
                "the party should be different to the counterparty" using (agreementOutput.party != agreementOutput.counterparty)
                "the status should be set as request" using (agreementOutput.agreementStatus == AgreementStatus.REQUEST)

                "the party and counterparty are required signers" using
                        (agreementCommand.signers.containsAll(listOf(agreementOutput.party.owningKey, agreementOutput.counterparty.owningKey)))
            }

            is Commands.ReviewAgreement -> requireThat {
                "one input should be consumed" using (agreementInputs.size == 1)
                "one output should bbe produced" using (agreementOutputs.size == 1)

                val agreementInput = agreementInputs.single()
                val agreementOutput = agreementOutputs.single()
                "the status should be set to request" using (agreementOutput.agreementStatus == AgreementStatus.REQUEST)
                "the previous status should not be STARTED" using (agreementInput.agreementStatus != AgreementStatus.IN_REVIEW)
                "only the job status should change" using (agreementInput.copy(agreementStatus = agreementOutput.agreementStatus) == agreementOutput)

            }

            is Commands.ActivateAgreement -> requireThat {
                "one input should be produced" using (agreementInputs.size == 1)
                "one output should be produced" using (agreementOutputs.size == 1)

                val agreementInput = agreementInputs.single()
                val agreementOutput = agreementOutputs.single()

                "the input status must be set as Request" using (agreementInput.agreementStatus == AgreementStatus.REQUEST)
                "the output status should be set as Activated" using (agreementOutput.agreementStatus == AgreementStatus.INEFFECT)
                "the agreement in unchanged apart from the status field" using (agreementInput.copy(agreementStatus = agreementOutput.agreementStatus) == agreementOutput)
            }

            is Commands.TerminateAgreement -> requireThat {
                "one input should be produced" using (agreementInputs.size == 1)
                "one output should be produced" using (agreementOutputs.size == 1)

                val agreementInput = agreementInputs.single()
                val agreementOutput = agreementOutputs.single()

                "the Agreement status must be set as ACTIVATED" using (agreementInput.agreementStatus == AgreementStatus.INEFFECT)
                "the output status should be set as terminated" using (agreementOutput.agreementStatus ==AgreementStatus.TERMINATED)
                "only the status must change" using (agreementInput.copy(agreementStatus = agreementOutput.agreementStatus) == agreementOutput)


            }

            is Commands.RenewAgreement -> requireThat {
                "one input should be produced" using (agreementInputs.size == 1)
                "one output should be produced" using (agreementOutputs.size == 1)

                val agreementInput = agreementInputs.single()
                val agreementOutput = agreementOutputs.single()

                "the input status must be set as in effect" using (agreementInput.agreementStatus == AgreementStatus.INEFFECT)
                "the output status should be set as renewed" using (agreementOutput.agreementStatus ==AgreementStatus.RENEWED)
                "only the status must change" using (agreementInput.copy(agreementStatus = agreementOutput.agreementStatus) == agreementOutput)


            }

            is Commands.ExpireAgreement -> requireThat {

                "one input should be produced" using (agreementInputs.size == 1)
                "one output should be produced" using (agreementOutputs.size == 1)

                val agreementInput = agreementInputs.single()
                val agreementOutput = agreementOutputs.single()

                "the input status must be set as in effect" using (agreementInput.agreementStatus == AgreementStatus.INEFFECT)
                "the output status should be set as terminated" using (agreementOutput.agreementStatus ==AgreementStatus.EXPIRED)
                "only the status must change" using (agreementInput.copy(agreementStatus = agreementOutput.agreementStatus) == agreementOutput)

            }


            is Commands.AmendAgreement -> requireThat {

                "one input should be produced" using (agreementInputs.size == 1)
                "one output should be produced" using (agreementOutputs.size == 1)

                val agreementInput = agreementInputs.single()
                val agreementOutput = agreementOutputs.single()

                "the input status must be set as in effect" using (agreementInput.agreementStatus == AgreementStatus.INEFFECT)
                "the output status should be set as terminated" using (agreementOutput.agreementStatus ==AgreementStatus.AMENDED)
                "only the status must change" using (agreementInput.copy(agreementStatus = agreementOutput.agreementStatus) == agreementOutput)

            }

            else -> throw IllegalArgumentException("Unrecognised command.")
        }
    }

}