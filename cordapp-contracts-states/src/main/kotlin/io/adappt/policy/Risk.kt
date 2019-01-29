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

package io.adappt.policy


import io.adappt.application.Application
import io.adappt.application.ApplicationStatus
import net.corda.core.contracts.*
import net.corda.core.identity.AbstractParty
import net.corda.core.identity.Party
import net.corda.core.schemas.MappedSchema
import net.corda.core.schemas.PersistentState
import net.corda.core.schemas.QueryableState
import net.corda.core.transactions.LedgerTransaction
import net.corda.core.transactions.TransactionBuilder
import java.lang.IllegalArgumentException
import java.security.PublicKey

val DEFAULT_RISK_SCORE = 0

/**
 * Contract to define the risk state, commands and verifications.
 */

class Risk : Contract {
    companion object {
        const val RISK_ID: ContractClassName = "io.adappt.policy.Risk"


        @JvmStatic
        fun generateCreate(tx: TransactionBuilder, agent: AbstractParty, provider: Party,
                           details: PolicyHolderDetails, notary: Party): PublicKey {
            check(tx.inputStates().isEmpty())
            check(tx.outputStates().isEmpty())

            tx.addOutputState(TransactionState(State(agent, provider, UniqueIdentifier(), details),
                    RISK_ID, notary))
            tx.addCommand(Commands.Create(), agent.owningKey, provider.owningKey)

            return agent.owningKey

        }

        @JvmStatic
        fun generateUpdate(tx: TransactionBuilder, stateAndRef: StateAndRef<State>,
                           updatedDetails: PolicyHolderDetails, notary: Party): PublicKey {
            check(tx.inputStates().isEmpty())
            check(tx.outputStates().isEmpty())

            tx.addInputState(stateAndRef)
            val updatedRisk = stateAndRef.state.data.copy(details = updatedDetails)

            tx.addOutputState(TransactionState(updatedRisk, RISK_ID, notary))
            tx.addCommand(Commands.Update(), updatedRisk.owner.owningKey)

            return updatedRisk.owner.owningKey

        }

        fun generateRiskScore(tx: TransactionBuilder, stateAndRef: StateAndRef<State>,
                              score: Int, notary: Party): PublicKey {
            check(tx.inputStates().isEmpty())
            check(tx.outputStates().isEmpty())

            tx.addInputState(stateAndRef)
            val scoredRisk = stateAndRef.state.data.copy(score = score)

            tx.addOutputState(TransactionState(scoredRisk, RISK_ID, notary))
            tx.addCommand(Commands.Score(), scoredRisk.owner.owningKey, scoredRisk.provider.owningKey)

            return scoredRisk.owner.owningKey

        }
    }

    override fun verify(tx: LedgerTransaction) {
        val groups = tx.groupStates { it: State -> it.accountId }

        val command = tx.commands.requireSingleCommand<Commands>()
        for ((inputs, outputs, _) in groups) {
            when (command.value) {
                is Commands.Create -> verifyCreateCommand(tx, inputs, outputs)
                is Commands.Update -> verifyUpdateCommand(tx, inputs, outputs)
                is Commands.Score -> verifyScoreCommand(tx, inputs, outputs)
            }
        }
    }


    private fun verifyCreateCommand(tx: LedgerTransaction, inputs: List<State>, outputs: List<State>) {
        val createCommand = tx.commands.requireSingleCommand<Commands.Create>()
        requireThat {
            "there are no input states" using (inputs.count() == 0)
            "there is a single output state" using (outputs.count() == 1)
            "agent and provider are signers on the command" using
                    (createCommand.signers.containsAll(
                            listOf(outputs.first().owner.owningKey, outputs.first().provider.owningKey)
                    ))
        }

        verifyRiskDetails(outputs.first().details)
    }

    private fun verifyUpdateCommand(tx: LedgerTransaction, inputs: List<State>, outputs: List<State>) {
        val updateCommand = tx.commands.requireSingleCommand<Commands.Update>()
        requireThat {
            "there is a single input state" using (inputs.count() == 1)
            "there is a single output state" using (outputs.count() == 1)
            "user agent is a command signer" using (outputs.first().owner.owningKey in updateCommand.signers)
        }

        val inputDetails = inputs.first().details
        val outputDetails = outputs.first().details

        requireThat {
            "details have been update" using (inputDetails != outputDetails)
        }

        verifyRiskDetails(outputDetails)

    }


    private fun verifyScoreCommand(tx: LedgerTransaction, inputs: List<State>, outputs: List<State>) {
        val scoreCommand = tx.commands.requireSingleCommand<Commands.Score>()
        requireThat {
            "there is a single input state" using (inputs.count() == 1)
            "there is a single output state" using (outputs.count() == 1)
            "broker and provider are signers on the command" using
                    (scoreCommand.signers.containsAll(
                            listOf(outputs.first().owner.owningKey, outputs.first().provider.owningKey)
                    ))
        }

        val output = outputs.first()
        requireThat {
            "valid risk score has been computed" using ((output.score > 0) && (output.score < 100))
        }
    }

    private fun verifyRiskDetails(details: PolicyHolderDetails) {
        requireThat {
            "company is within corporate range" using (details.basics.age in 0..100)
            "financials is within expected range" using (details.financials.stock in 0..10000)
            "medical is within expected range" using (details.medical.exercise in 0..100)
        }

        if (details.financials.teikoku > 0) {
            requireThat {
                "teikoko is in normal range" using (details.financials.teikoku in 0..100)
            }
        }
    }


    data class State(override val owner: AbstractParty,
                     val provider: Party,
                     val accountId: UniqueIdentifier,
                     val details: PolicyHolderDetails,
                     val score: Int = DEFAULT_RISK_SCORE) : OwnableState, QueryableState {


        override val participants = listOf(owner, provider)

        override fun withNewOwner(newOwner: AbstractParty): CommandAndState =
                CommandAndState(Commands.Update(), copy(owner = newOwner))

        override fun generateMappedObject(schema: MappedSchema): PersistentState {
            return when (schema) {
                is RiskSchemaV1 -> RiskSchemaV1.PersistentRiskState(
                        agent = this.owner,
                        provider = this.provider,
                        accountId = this.accountId.toString(),
                        credit = this.details.financials.credit,
                        teikoku = this.details.financials.teikoku,
                        title = this.details.basics.title,
                        age = this.details.basics.age,
                        sex = this.details.basics.sex.toString(),
                        height = this.details.basics.height,
                        weight = this.details.basics.weight,
                        bloodPressure = this.details.medical.bloodPressure,
                        urine = this.details.medical.urine.toString(),
                        blood = this.details.medical.blood.toString(),
                        maximumAge = this.details.basics.maximumAge,
                        smoker = this.details.medical.smoker.toString(),
                        drinker = this.details.medical.drinker.toString(),
                        minimumLives = this.details.basics.minimumLives,
                        revenue = this.details.financials.revenue,
                        salary = this.details.financials.salary,
                        bonus = this.details.financials.bonus,
                        bonds = this.details.financials.bonds,
                        options = this.details.financials.options,
                        profit = this.details.financials.profit,
                        debt = this.details.financials.debt,
                        cash = this.details.financials.cash,
                        exercise = this.details.exercise.toString(),
                        sleep = this.details.sleep.toString(),
                        score = this.score
                )
                else -> throw IllegalArgumentException("Unrecognized schema $schema")
            }
        }

        override fun supportedSchemas(): Iterable<MappedSchema> = listOf(RiskSchemaV1)

    }

    interface Commands : CommandData {

        class Create : Commands

        class Update : Commands

        class Score : Commands
    }

}


class ApplicationContract : Contract {
    // This is used to identify our contract when building a transaction
    companion object {
        val APPLICATION_CONTRACT_ID = "io.adappt.policy.ApplicationContract"
    }

    // Used to indicate the transaction's intent.
    interface Commands : CommandData {

        class CreateApplication: Commands
        class ReviewApplication : Commands
        class ApproveApplication: Commands
        class RejectApplication: Commands


    }


    // A transaction is considered valid if the verify() function of the contract of each of the transaction's input
    // and output states does not throw an exception.
    override fun verify(tx: LedgerTransaction) {
        val applicationInputs = tx.inputsOfType<Application>()
        val applicationOutputs = tx.outputsOfType<Application>()
        val applicationCommand = tx.commandsOfType<Commands>().single()

        when(applicationCommand.value) {
            is Commands.CreateApplication -> requireThat {
                "no inputs should be consumed" using (applicationInputs.isEmpty())
                // TODO we might allow several jobs to be proposed at once later
                "one output should be produced" using (applicationOutputs.size == 1)

                val applicationOutput = applicationOutputs.single()
                "the party should be different to the counterparty" using (applicationOutput.agent != applicationOutput.provider)
                "the status should be set as requested" using (applicationOutput.applicationStatus == ApplicationStatus.REQUESTED)

                "the party and counterparty are required signers" using
                        (applicationCommand.signers.containsAll(listOf(applicationOutput.agent.owningKey, applicationOutput.provider.owningKey)))
            }


            is Commands.ReviewApplication -> requireThat {
                "one input should be produced" using (applicationInputs.size == 1)
                "one output should be produced" using (applicationOutputs.size == 1)

                val applicationInput = applicationInputs.single()
                val applicationOutput = applicationOutputs.single()

                "the input status must be set as started" using (applicationInputs.single().applicationStatus == ApplicationStatus.REQUESTED)
                "the output status should be set as ineffect" using (applicationOutputs.single().applicationStatus == ApplicationStatus.INREVIEW)
                "only the status must change" using (applicationInput.copy(applicationStatus = ApplicationStatus.INREVIEW) == applicationOutput)
                "the update must be signed by the contractor of the " using (applicationOutputs.single().agent == applicationInputs.single().provider)
                "the contractor should be signer" using (applicationCommand.signers.contains(applicationOutputs.single().provider.owningKey))

            }



            is Commands.ApproveApplication -> requireThat {
                "one input should be produced" using (applicationInputs.size == 1)
                "one output should be produced" using (applicationOutputs.size == 1)

                val applicationInput = applicationInputs.single()
                val applicationOutput = applicationOutputs.single()

                "the input status must be set as started" using (applicationInputs.single().applicationStatus == ApplicationStatus.INREVIEW)
                "the output status should be set as approved" using (applicationOutputs.single().applicationStatus == ApplicationStatus.APPROVED)
                "only the status must change" using (applicationInput.copy(applicationStatus = ApplicationStatus.APPROVED) == applicationOutput)
                "the update must be signed by the contractor of the " using (applicationOutputs.single().agent == applicationInputs.single().provider)
                "the contractor should be signer" using (applicationCommand.signers.contains(applicationOutputs.single().provider.owningKey))

            }


            is Commands.RejectApplication -> requireThat {
                "one input should be produced" using (applicationInputs.size == 1)
                "one output should be produced" using (applicationOutputs.size == 1)

                val applicationInput = applicationInputs.single()
                val applicationOutput = applicationOutputs.single()

                "the input status must be set as in effect" using (applicationInputs.single().applicationStatus == ApplicationStatus.INREVIEW)
                "the output status should be set as renewed" using (applicationOutputs.single().applicationStatus ==ApplicationStatus.REJECTED)
                "only the status must change" using (applicationInput.copy(applicationStatus = ApplicationStatus.REJECTED) == applicationOutput)
                "the update must be signed by the party of the " using (applicationOutputs.single().agent == applicationInputs.single().agent)
                "the party should be the signer" using (applicationCommand.signers.contains(applicationOutputs.single().provider.owningKey))


            }


            else -> throw IllegalArgumentException("Unrecognised command.")
        }
    }

}