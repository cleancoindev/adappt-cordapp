package io.adappt.lead

import com.fasterxml.jackson.annotation.JsonValue
import net.corda.core.contracts.*
import net.corda.core.contracts.Requirements.using
import net.corda.core.identity.AbstractParty
import net.corda.core.schemas.MappedSchema
import net.corda.core.schemas.PersistentState
import net.corda.core.schemas.QueryableState
import net.corda.core.serialization.CordaSerializable
import net.corda.core.transactions.LedgerTransaction
import java.lang.IllegalArgumentException
import java.security.PublicKey


// *********
// * State *
// *********


/**
 * The state object recording CRM assets between two parties.
 *
 * A state must implement [LeadState] or one of its descendants.
 *
 * @Param firstName of the Lead.
 * @Param lastName of the Lead.
 * @param email of the Lead.
 * @param phone of the Lead.
 * @param status of the Lead.
 * @param owner the party who owns the Lead.
 */


data class Lead(val leadId: String,
                val firstName: String,
                val lastName: String,
                val email: String,
                val phone: String,
                override val owner: AbstractParty) : QueryableState, OwnableState {


    override val participants: List<AbstractParty> = listOf(owner)
    override fun supportedSchemas(): Iterable<MappedSchema> = listOf(LeadSchemaV1)

    override fun withNewOwner(newOwner: AbstractParty): CommandAndState {
        return CommandAndState(LeadContract.Commands.TransferLead(), this.copy(owner = newOwner))
    }

    override fun generateMappedObject(schema: MappedSchema): PersistentState {
        return when (schema) {
            is LeadSchemaV1 -> LeadSchemaV1.PersistentLead(
                    leadId = this.leadId,
                    firstName = this.firstName,
                    lastName = this.lastName,
                    email = this.email,
                    phone = this.phone,
                    owner = this.owner
            )
            else -> throw IllegalArgumentException("Unrecognised schema $schema")
        }
    }


    @CordaSerializable
    enum class LeadStatus {
        OPEN,
        WORKING,
        CLOSED_CONVERTED,
        CLOSED_NONCONVERTED
    }


    @CordaSerializable
    enum class RequestStatus(@JsonValue val value: String) {
        PENDING_CONFIRMATION("Pending Confirmation"), //Initial status
        PENDING("Pending"), // updated by buyer
        TRANSFERRED("Transferred"), // on valid asset data clearing house update this status
        REJECTED("Rejected"), // on invalid asset data clearing house reject transaction with this status.
        FAILED("Failed") // on fail of settlement e.g. with insufficient cash from Buyer party.
    }

}



class LeadContract : Contract {
    companion object {
        @JvmStatic
        val LEAD_CONTRACT_ID = LeadContract::class.java.canonicalName
    }

    interface Commands : CommandData {
        class CreateLead : TypeOnlyCommandData(), Commands
        class TransferLead : TypeOnlyCommandData(), Commands
        class Share : TypeOnlyCommandData(), Commands
        class Erase : TypeOnlyCommandData(), Commands
        class Convert : TypeOnlyCommandData(), Commands
    }

    override fun verify(tx: LedgerTransaction) {
        val command = tx.commands.requireSingleCommand<Commands>()
        val setOfSigners = command.signers.toSet()
        when (command.value) {
            is Commands.CreateLead -> verifyCreate(tx, setOfSigners)
            is Commands.TransferLead -> verifyTransfer(tx, setOfSigners)
            else -> throw IllegalArgumentException("Unrecognised command.")
        }
    }

    private fun verifyCreate(tx: LedgerTransaction, signers: Set<PublicKey>) = requireThat {
        "No inputs must be consumed." using (tx.inputStates.isEmpty())
        "Only one out state should be created." using (tx.outputStates.size == 1)
        val output = tx.outputsOfType<Lead>().single()
        "Owner only may sign the Account issue transaction." using (output.owner.owningKey in signers)
    }

    private fun verifyTransfer(tx: LedgerTransaction, signers: Set<PublicKey>) = requireThat {
        val inputLeads = tx.inputsOfType<Lead>()
        val inputLeadTransfers = tx.inputsOfType<Lead>()
        "There must be one input obligation." using (inputLeads.size == 1)


        val inputLead = inputLeads.single()
        val outputs = tx.outputsOfType<Lead>()
        // If the obligation has been partially settled then it should still exist.
        "There must be one output Lead." using (outputs.size == 1)

        // Check only the paid property changes.
        val output = outputs.single()
        "Must not not change Lead data except owner field value." using (inputLead == output.copy(owner = inputLead.owner))
        "Owner only may sign the Lead issue transaction." using (output.owner.owningKey in signers)
    }
}
