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