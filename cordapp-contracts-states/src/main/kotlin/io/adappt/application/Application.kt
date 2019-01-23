package io.adappt.application

import net.corda.core.contracts.ContractState
import net.corda.core.contracts.LinearState
import net.corda.core.contracts.UniqueIdentifier
import net.corda.core.identity.AbstractParty
import net.corda.core.identity.Party
import net.corda.core.serialization.CordaSerializable

// *********
// * Application State *
// *********

data class Application(val applicationId: String,
                       val applicationName: String,
                       val industry: String,
                       val applicationStatus: ApplicationStatus,
                       val agent: Party,
                       val provider: Party,
                       override val linearId: UniqueIdentifier = UniqueIdentifier()) : LinearState, ContractState {


    override val participants: List<AbstractParty> get() = listOf(agent, provider)


}

@CordaSerializable
enum class ApplicationStatus {
    SUBMITTED, REQUESTED, UNSTARTED, STARTED, INREVIEW, WORKING, ESCALATED, APPROVED, REJECTED
}