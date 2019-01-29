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

package io.adappt

import io.adappt.policy.Risk
import io.adappt.policy.RiskSchemaV1
import net.corda.core.contracts.StateAndRef
import net.corda.core.contracts.UniqueIdentifier
import net.corda.core.node.ServiceHub
import net.corda.core.node.services.Vault
import net.corda.core.node.services.vault.BinaryComparisonOperator
import net.corda.core.node.services.vault.Builder.equal
import net.corda.core.node.services.vault.ColumnPredicate
import net.corda.core.node.services.vault.QueryCriteria
import java.time.LocalDate
import java.time.ZoneOffset

fun ServiceHub.accountExists(accountId: UniqueIdentifier) : List<StateAndRef<Risk.State>> {
    val queryCriteria = QueryCriteria.VaultQueryCriteria(Vault.StateStatus.UNCONSUMED)
    val customCriteria =
            QueryCriteria.VaultCustomQueryCriteria(RiskSchemaV1.PersistentRiskState::accountId.equal(accountId.toString()))

    val criteria = queryCriteria.and(customCriteria)

    val pages = vaultService.queryBy(Risk.State::class.java, criteria)
    return pages.states
}

fun ServiceHub.retrieveRisk(accountId: UniqueIdentifier) : List<StateAndRef<Risk.State>> {
    val asOfDateTime = LocalDate.now().minusDays(30).atStartOfDay().toInstant(ZoneOffset.UTC)
    val consumedAfterExpression = QueryCriteria.TimeCondition(
            QueryCriteria.TimeInstantType.CONSUMED, ColumnPredicate.BinaryComparison(BinaryComparisonOperator.GREATER_THAN_OR_EQUAL, asOfDateTime))

    val queryCriteria = QueryCriteria.VaultQueryCriteria(status = Vault.StateStatus.CONSUMED, timeCondition = consumedAfterExpression)
    val customCriteria =
            QueryCriteria.VaultCustomQueryCriteria(RiskSchemaV1.PersistentRiskState::accountId.equal(accountId.toString()))

    val criteria = queryCriteria.and(customCriteria)
    val pages = vaultService.queryBy(Risk.State::class.java, criteria)

    return pages.states
}