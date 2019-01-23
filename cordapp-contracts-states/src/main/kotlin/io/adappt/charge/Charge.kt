package io.adappt.charge

import net.corda.core.contracts.LinearState
import net.corda.core.contracts.UniqueIdentifier
import net.corda.core.identity.Party
import net.corda.core.serialization.CordaSerializable
import java.util.*

// *********
// * Charge State *
// *********


@CordaSerializable
data class Charge(val accountId: String,
                  val amount: Int,
                  val currency: Currency,
                  val party: Party,
                  val counterparty: Party,
                  val paid: Boolean,
                  val status: ChargeStatus,
                  override val linearId: UniqueIdentifier = UniqueIdentifier()) : LinearState {

    override val participants = listOf(party, counterparty)


}


@CordaSerializable
enum class ChargeStatus {
    SUCCEEDED, DECLINED
}