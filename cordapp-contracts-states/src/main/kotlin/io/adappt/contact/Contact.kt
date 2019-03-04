package io.adappt.contact

import net.corda.core.contracts.*
import net.corda.core.contracts.Requirements.using
import net.corda.core.crypto.NullKeys
import net.corda.core.identity.AbstractParty
import net.corda.core.schemas.MappedSchema
import net.corda.core.schemas.PersistentState
import net.corda.core.schemas.QueryableState
import net.corda.core.serialization.CordaSerializable
import net.corda.core.transactions.LedgerTransaction
import java.lang.IllegalArgumentException
import java.security.PublicKey



/**
 * The state object recording CRM assets between two parties.
 *
 * A state must implement [Contact] or one of its descendants.
 *
 * @Param contactId of the Contact.
 * @Param firstName of the Contact.
 * @Param lastName of the Contact.
 * @param email of the Contact.
 * @param phone of the Contact.
 * @param owner the party who owns the Contact.
 */

data class Contact(val contactId: String,
                   val firstName: String,
                   val lastName: String,
                   val email: String,
                   val phone: String,
                   override val owner: AbstractParty) : QueryableState, OwnableState {

    override val participants: List<AbstractParty> = listOf(owner)
    override fun supportedSchemas(): Iterable<MappedSchema> = listOf(ContactSchemaV1)


    fun withoutOwner() = copy(owner = NullKeys.NULL_PARTY)

    override fun withNewOwner(newOwner: AbstractParty): CommandAndState {
        return CommandAndState(ContactContract.Commands.TransferContact(), this.copy(owner = newOwner))
    }

    override fun generateMappedObject(schema: MappedSchema): PersistentState {
        return when (schema) {
            is ContactSchemaV1 -> ContactSchemaV1.PersistentContact(
                    contactId = this.contactId,
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
    data class ContactProperties(
            val owner: AbstractParty,
            val firstName: String,
            val lastName: String,
            val email: String,
            val industry: String,
            val phone: String,
            val contactId: String
    )

}





class ContactContract : Contract {
    companion object {
        @JvmStatic
        val CONTACT_CONTRACT_ID = ContactContract::class.java.canonicalName
    }

    interface Commands : CommandData {
        class CreateContact : TypeOnlyCommandData(), Commands
        class TransferContact : TypeOnlyCommandData(), Commands
        class Share : TypeOnlyCommandData(), Commands
        class Erase : TypeOnlyCommandData(), Commands
    }

    override fun verify(tx: LedgerTransaction) {
        val command = tx.commands.requireSingleCommand<Commands>()
        val setOfSigners = command.signers.toSet()
        when (command.value) {
            is Commands.CreateContact -> verifyCreate(tx, setOfSigners)
            is Commands.TransferContact -> verifyTransfer(tx, setOfSigners)
            else -> throw IllegalArgumentException("Unrecognised command.")
        }
    }

    private fun verifyCreate(tx: LedgerTransaction, signers: Set<PublicKey>) = requireThat {
        "No inputs must be consumed." using (tx.inputStates.isEmpty())
        "Only one out state should be created." using (tx.outputStates.size == 1)
        val output = tx.outputsOfType<Contact>().single()
        "Owner only may sign the Contact issue transaction." using (output.owner.owningKey in signers)
    }

    private fun verifyTransfer(tx: LedgerTransaction, signers: Set<PublicKey>) = requireThat {
        val inputContacts = tx.inputsOfType<Contact>()
        //val inputContactTransfers = tx.inputsOfType<ContactTransfer>()
        "There must be one input Contact." using (inputContacts.size == 1)


        val inputContact = inputContacts.single()
        val outputs = tx.outputsOfType<Contact>()
        // If the obligation has been partially settled then it should still exist.
        "There must be one output Contact." using (outputs.size == 1)

        // Check only the paid property changes.
        val output = outputs.single()
        "Must not not change Contact data except owner field value." using (inputContact == output.copy(owner = inputContact.owner))
        "Owner only may sign the Contact transfer transaction." using (output.owner.owningKey in signers)
    }
}