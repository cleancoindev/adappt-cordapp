package io.adappt.claim

import net.corda.core.contracts.ContractState
import net.corda.core.contracts.LinearState
import net.corda.core.contracts.UniqueIdentifier
import net.corda.core.identity.AbstractParty
import net.corda.core.identity.Party
import net.corda.core.schemas.MappedSchema
import net.corda.core.schemas.PersistentState
import net.corda.core.schemas.QueryableState
import net.corda.core.contracts.CommandData
import net.corda.core.contracts.Contract
import net.corda.core.contracts.requireSingleCommand
import net.corda.core.contracts.requireThat
import net.corda.core.transactions.LedgerTransaction



/**
 * The state object recording Claim applications between two parties.
 *
 * A state must implement [ContractState] or one of its descendants.
 *
 * @param value the amount of the Claim.
 * @param insurerNode the party issuing the Claim (Insurance Company).
 * @param applicantNode the party applying for the Claim.
 */




data class Claim(
        val applicantNode: Party,
        val insurerNode: Party,
        val fname: String,
        val lname: String,
        val address: String,
        var insuranceID: String,
        val type: String,
        val value: Int,
        val reason: String,
        val approvedAmount:Int,
        var insuranceStatus: String,
        var referenceID: String,
        override val linearId: UniqueIdentifier = UniqueIdentifier()):
        LinearState, QueryableState {
    /** The public keys of the involved parties. */
    override val participants: List<AbstractParty> get() = listOf(insurerNode,applicantNode)

    override fun generateMappedObject(schema: MappedSchema): PersistentState {
        return when (schema) {
            is ClaimSchemaV1 -> ClaimSchemaV1.PersistentClaim(
                    this.applicantNode.name.toString(),
                    this.insurerNode.name.toString(),
                    this.fname,
                    this.lname,
                    this.address,
                    this.insuranceID,
                    this.type,
                    this.value,
                    this.reason,
                    this.approvedAmount,
                    this.insuranceStatus,
                    this.referenceID,
                    this.linearId.id
            )
            else -> throw IllegalArgumentException("Unrecognised schema $schema")
        }
    }

    override fun supportedSchemas(): Iterable<MappedSchema> = listOf(ClaimSchemaV1)
}



/**
 * This contract enforces rules regarding the creation of a valid [ClaimState].
 *
 * For a new [Claim] to be created onto the ledger, a transaction is required which takes:
 * - Zero input states.
 * - One output state: the new [Claim].
 * - An Create() command with the public keys of both the party.
 *
 *  For verified [Claim] to be created onto the ledger, a transaction is required which takes:
 * - One input states: the old [Claim].
 * - One output state: the new [Claim].
 *
 * All contracts must sub-class the [Contract] interface.
 */



class ClaimContract : Contract {
    companion object {
        @JvmStatic
        val CLAIM_CONTRACT_ID = "com.insuranceClaim.contract.ClaimContract"
    }

    /**
     * The verify() function of all the states' contracts must not throw an exception for a transaction to be
     * considered valid.
     */

    override fun verify(tx: LedgerTransaction) {
        val command = tx.commands.requireSingleCommand<ClaimContract.Commands>()
        when (command.value) {
            is ClaimContract.Commands.ClaimApplication -> {
                requireThat {
                    // Generic constraints around the Claim transaction.
                    "No inputs should be consumed when creating Claim Application." using (tx.inputs.isEmpty())
                    "Only one output state should be created." using (tx.outputs.size == 1)
                    val out = tx.outputsOfType<Claim>().single()
                    "The Applicant and the Insurance Company cannot be the same entity." using (out.applicantNode != out.insurerNode)
                    "All of the participants must be signers." using (command.signers.containsAll(out.participants.map { it.owningKey }))

                    // Claim-specific constraints.
                    "The Claim value must be non-negative." using (out.value > 0)
                }
            }

            is ClaimContract.Commands.ClaimTest -> {
                requireThat {
                    // Generic constraints around the Claim transaction.
                    "One input should be consumed" using (tx.inputs.size==1)
                }
            }

            is ClaimContract.Commands.ClaimResponse -> {
                requireThat {
                    // Generic constraints around the Claim transaction.
                    "Two output states should be created." using (tx.outputs.size == 2)
                    val input = tx.inputsOfType<Claim>().single()
                    val out = tx.outputsOfType<Claim>().single()
                    "The Applicant and the Insurance Company cannot be the same entity." using (out.insurerNode != out.applicantNode)
                    "All of the participants must be signers." using (command.signers.containsAll(out.participants.map { it.owningKey }))

                }
            }
        }
    }

    /**
     * This contract implements two commands.
     */
    interface Commands : CommandData {
        class ClaimApplication : Commands
        class ClaimResponse : Commands
        class ClaimTest : Commands
    }
}