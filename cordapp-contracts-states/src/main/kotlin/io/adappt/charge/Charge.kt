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

package io.adappt.charge

import io.adappt.application.Application
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
                  val application: Application,
                  val paid: Boolean,
                  val status: ChargeStatus,
                  override val linearId: UniqueIdentifier = UniqueIdentifier()) : LinearState {

    override val participants = listOf(party, counterparty)


}


@CordaSerializable
enum class ChargeStatus {
    SUCCEEDED, DECLINED
}