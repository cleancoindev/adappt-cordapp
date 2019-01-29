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

package io.adappt.utils

import io.adappt.*
import io.adappt.agreement.Agreement
import io.adappt.agreement.AgreementSchemaV1
import io.adappt.agreement.AgreementStatus
import io.adappt.application.Application
import io.adappt.application.ApplicationSchemaV1
import io.adappt.application.ApplicationStatus
import net.corda.core.contracts.ContractState
import net.corda.core.contracts.StateAndRef
import net.corda.core.contracts.UniqueIdentifier
import net.corda.core.node.ServiceHub
import net.corda.core.node.services.Vault
import net.corda.core.node.services.queryBy
import net.corda.core.node.services.vault.Builder.equal
import net.corda.core.node.services.vault.QueryCriteria
import net.corda.core.node.services.vault.builder
// import org.apache.commons.math.distribution.LaplanceDistribution // Differential Privacy Algo



/** Bunch of helpers for querying the vault. */


// Agreement Queries

fun getAgreementByAgreementNumber(agreementNumber: String, services: ServiceHub): StateAndRef<Agreement>? {
    val states = getState<Agreement>(services) { generalCriteria ->
        val additionalCriteria = QueryCriteria.VaultCustomQueryCriteria(AgreementSchemaV1.PersistentAgreement::agreementNumber.equal(agreementNumber.toString()))
        generalCriteria.and(additionalCriteria)
    }
    return states.singleOrNull()
}


fun getAgreementByStatus(AgreementStatus: AgreementStatus, services: ServiceHub): StateAndRef<Agreement>? {
    val states = getState<Agreement>(services) { generalCriteria ->
        val additionalCriteria = QueryCriteria.VaultCustomQueryCriteria(AgreementSchemaV1.PersistentAgreement::agreementStatus.equal(AgreementStatus.toString()))
        generalCriteria.and(additionalCriteria)
    }
    return states.singleOrNull()
}

fun getAgreementByLinearId(linearId: UniqueIdentifier, services: ServiceHub): StateAndRef<Agreement>? {
    val states = getState<Agreement>(services) { generalCriteria ->
        val additionalCriteria = QueryCriteria.VaultCustomQueryCriteria(AgreementSchemaV1.PersistentAgreement::linearId.equal(linearId.id.toString()))
        generalCriteria.and(additionalCriteria)
    }
    return states.singleOrNull()
}



// Application Queries


fun getApplicationByApplicationId(applicationId: String, services: ServiceHub): StateAndRef<Application>? {
    val states = getState<Application>(services) { generalCriteria ->
        val additionalCriteria = QueryCriteria.VaultCustomQueryCriteria(ApplicationSchemaV1.PersistentApplication::applicationId.equal(applicationId.toString()))
        generalCriteria.and(additionalCriteria)
    }
    return states.singleOrNull()
}


fun getApplicationByStatus(ApplicationStatus: ApplicationStatus, services: ServiceHub): StateAndRef<Application>? {
    val states = getState<Application>(services) { generalCriteria ->
        val additionalCriteria = QueryCriteria.VaultCustomQueryCriteria(ApplicationSchemaV1.PersistentApplication::applicationStatus.equal(ApplicationStatus.toString()))
        generalCriteria.and(additionalCriteria)
    }
    return states.singleOrNull()
}


fun getApplicationByLinearId(linearId: UniqueIdentifier, services: ServiceHub): StateAndRef<Application>? {
    val states = getState<Application>(services) { generalCriteria ->
        val additionalCriteria = QueryCriteria.VaultCustomQueryCriteria(ApplicationSchemaV1.PersistentApplication::linearId.equal(linearId.id.toString()))
        generalCriteria.and(additionalCriteria)
    }
    return states.singleOrNull()
}





private inline fun <reified U : ContractState> getState(
        services: ServiceHub,
        block: (generalCriteria: QueryCriteria.VaultQueryCriteria) -> QueryCriteria
): List<StateAndRef<U>> {
    val query = builder {
        val generalCriteria = QueryCriteria.VaultQueryCriteria(Vault.StateStatus.UNCONSUMED)
        block(generalCriteria)
    }
    val result = services.vaultService.queryBy<U>(query)
    return result.states
}