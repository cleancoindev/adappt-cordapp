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

import net.corda.core.identity.AbstractParty
import net.corda.core.schemas.MappedSchema
import net.corda.core.schemas.PersistentState
import net.corda.core.serialization.CordaSerializable
import javax.persistence.Column
import javax.persistence.Entity
import javax.persistence.Table


object RiskSchema

/**
 * Schema to define database table to capture data in a risk state.
 */

@CordaSerializable
object RiskSchemaV1 : MappedSchema(schemaFamily = RiskSchema.javaClass, version = 1, mappedTypes = listOf(PersistentRiskState::class.java)) {
    @Entity
    @Table(name = "risk_states")
    class PersistentRiskState(

            @Column(name = "agent")
            var agent: AbstractParty? = null,

            @Column(name = "provider")
            var provider: AbstractParty? = null,

            @Column(name = "account_id")
            var accountId: String = "",

            @Column(name = "credit")
            var credit: Int = 0,

            @Column(name = "teikoku")
            var teikoku: Int = 0,

            @Column(name = "title")
            var title: String = "",

            @Column(name = "age")
            var age: Int = 0,

            @Column(name = "sex")
            var sex: String = "",

            @Column(name = "height")
            var height: Int = 0,

            @Column(name = "weight")
            var weight: Int = 0,

            @Column(name = "bloodPressure")
            var bloodPressure: Int = 0,

            @Column(name = "urine")
            var urine: String = "",

            @Column(name = "blood")
            var blood: String = "",

            @Column(name = "maximumAge")
            var maximumAge: Int = 0,

            @Column(name = "smoker")
            var smoker: String = "",

            @Column(name = "drinker")
            var drinker: String = "",

            @Column(name = "mimumimLives")
            var minimumLives: Int = 0,

            @Column(name = "revenue")
            var revenue: Int = 0,

            @Column(name = "bonus")
            var bonus: Int = 0,

            @Column(name = "bonds")
            var bonds: Int = 0,

            @Column(name = "options")
            var options: Int = 0,

            @Column(name = "salary")
            var salary: Int = 0,

            @Column(name = "profit")
            var profit: Int = 0,

            @Column(name = "debt")
            var debt: Int = 0,

            @Column(name = "cash")
            var cash: Int = 0,

            @Column(name = "exercise")
            var exercise: String = "",

            @Column(name = "sleep")
            var sleep: String = "",

            @Column(name = "risk_score")
            var score: Int = 0

    ) : PersistentState()
}
