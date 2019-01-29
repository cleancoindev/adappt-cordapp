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


package io.adappt.webserver

import io.adappt.*
import io.adappt.agreement.Agreement
import io.adappt.agreement.AgreementStatus
import io.adappt.agreement.AgreementType
import io.adappt.application.Application
import io.adappt.application.ApplicationStatus
import io.adappt.claim.Claim
import net.corda.core.contracts.TransactionVerificationException
import net.corda.core.identity.CordaX500Name
import net.corda.core.messaging.vaultQueryBy
import net.corda.core.utilities.getOrThrow
import org.slf4j.LoggerFactory
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*
import org.springframework.web.bind.annotation.RestController
import sun.security.timestamp.TSResponse
import java.time.LocalDateTime
import java.time.ZoneId
import javax.servlet.http.HttpServletRequest

/**
 * Define your API endpoints here.
 */


@RestController
@RequestMapping("/api") // The paths for HTTP requests are relative to this base path.
class RestController(
        private val rpc: NodeRPCConnection) {


    companion object {
        private val logger = LoggerFactory.getLogger(RestController::class.java)
    }


    private val me = rpc.proxy.nodeInfo().legalIdentities.first().name


    /** Maps an Agreement to a JSON object. */

    private fun Agreement.toJson(): Map<String, String> {
        return kotlin.collections.mapOf("party" to party.name.organisation, "counterparty" to counterparty.name.toString(), "agreement" to agreementNumber)
    }


    /** Maps an Application to a JSON object. */

    private fun Application.toJson(): Map<String, String> {
        return kotlin.collections.mapOf("party" to agent.name.organisation, "counterparty" to provider.name.toString(), "application" to applicationId)
    }


    /** Maps an Claims to a JSON object. */

    private fun Claim.toJson(): Map<String, String> {
        return kotlin.collections.mapOf("application" to applicantNode.name.organisation, "insurer" to insurerNode.name.toString(), "claim" to referenceID)
    }



    /** Returns the node's name. */
    @GetMapping(value = "/me", produces = arrayOf("text/plain"))
    private fun me() = me.toString()

    @GetMapping(value = "/status", produces = arrayOf("text/plain"))
    private fun status() = "200"

    @GetMapping(value = "/servertime", produces = arrayOf("text/plain"))
    private fun serverTime() = LocalDateTime.ofInstant(proxy.currentNodeTime(), ZoneId.of("UTC")).toString()

    @GetMapping(value = "/addresses", produces = arrayOf("text/plain"))
    private fun addresses() = proxy.nodeInfo().addresses.toString()

    @GetMapping(value = "/identities", produces = arrayOf("text/plain"))
    private fun identities() = proxy.nodeInfo().legalIdentities.toString()

    @GetMapping(value = "/platformversion", produces = arrayOf("text/plain"))
    private fun platformVersion() = proxy.nodeInfo().platformVersion.toString()

    @GetMapping(value = "/peers", produces = arrayOf("text/plain"))
    private fun peers() = proxy.networkMapSnapshot().flatMap { it.legalIdentities }.toString()

    @GetMapping(value = "/notaries", produces = arrayOf("text/plain"))
    private fun notaries() = proxy.notaryIdentities().toString()

    @GetMapping(value = "/flows", produces = arrayOf("text/plain"))
    private fun flows() = proxy.registeredFlows().toString()


    private val proxy = rpc.proxy

    /** Returns a list of existing Agreements. */

    @GetMapping(value = "/getAgreements", produces = arrayOf("application/json"))
    fun getAgreements(): List<Map<String, String>> {
        val agreementStateAndRefs = rpc.proxy.vaultQueryBy<Agreement>().states
        val agreementStates = agreementStateAndRefs.map { it.state.data }
        return agreementStates.map { it.toJson() }
    }


    /** Returns a list of existing Applications. */

    @GetMapping(value = "/getApplications", produces = arrayOf("application/json"))
    fun getApplications(): List<Map<String, String>> {
        val applicationStateAndRefs = rpc.proxy.vaultQueryBy<Application>().states
        val applicationStates = applicationStateAndRefs.map { it.state.data }
        return applicationStates.map { it.toJson() }
    }


    /** Returns a list of existing Claims. */

    @GetMapping(value = "/getClaims", produces = arrayOf("application/json"))
    fun getClaims(): List<Map<String, String>> {
        val claimStateAndRefs = rpc.proxy.vaultQueryBy<Claim>().states
        val claimStates = claimStateAndRefs.map { it.state.data }
        return claimStates.map { it.toJson() }
    }




    /** Creates an Agreement. */

    @PostMapping(value = "/createAgreement")
    fun createAgreement(@RequestParam("agreementNumber") agreementNumber: String,
                        @RequestParam("agreementName") agreementName: String,
                        @RequestParam("agreementStatus") agreementStatus: AgreementStatus,
                        @RequestParam("agreementType") agreementType: AgreementType,
                        @RequestParam("totalAgreementValue") totalAgreementValue: Int,
            //  @RequestParam("agreementStartDate") agreementStartDate: String,
            //  @RequestParam("agreementEndDate") agreementEndDate: String,
            //  @RequestParam("active") active: Boolean,
            //  @RequestParam("createdAt") createdAt: String,
            //  @RequestParam("lastUpdated") lastUpdated: String,
                        @RequestParam("counterpartyName") counterpartyName: CordaX500Name?): ResponseEntity<Any?> {

        if (totalAgreementValue <= 0) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("Query parameter 'total agreement value' must be non-negative.\n")
        }
        if (counterpartyName == null) {
            return ResponseEntity.status(TSResponse.BAD_REQUEST).body("Query parameter 'counterPartyName' missing or has wrong format.\n")
        }

        val otherParty = proxy.wellKnownPartyFromX500Name(counterpartyName)
                ?: return ResponseEntity.status(TSResponse.BAD_REQUEST).body("Party named $counterpartyName cannot be found.\n")

        val (status, message) = try {

            //  val flowHandle = rpc.proxy.startFlowDynamic(CreateAgreementFlow.Initiator::class.java, agreementNumber, agreementName, agreementStatus, agreementType, totalAgreementValue, agreementStartDate, agreementEndDate, active, createdAt, lastUpdated, otherParty)
            val flowHandle = proxy.startFlowDynamic(CreateAgreementFlow.Initiator::class.java, agreementNumber, agreementName, agreementStatus, agreementType, totalAgreementValue, otherParty)

            val result = flowHandle.use { it.returnValue.getOrThrow() }

            HttpStatus.CREATED to "Transaction id ${result.tx.id} committed to ledger.\n${result.tx.outputs.single().data}"

        } catch (e: Exception) {
            HttpStatus.BAD_REQUEST to e.message
        }
        logger.info(message)
        return ResponseEntity<Any?>(message, status)
    }


    @PostMapping(value = "/activateAgreement", produces = arrayOf("text/plain"), headers = arrayOf("Content-Type=applcation/x-www-form-urlencoded"))
    fun activateAgreement(@RequestParam("agreementNumber") agreementNumber: String, request: HttpServletRequest): ResponseEntity<String> {
        val agreementNumber = request.getParameter("agreementNumber")
        val flow = rpc.proxy.startFlowDynamic(ActivateAgreementFlow::class.java, agreementNumber)

        return try {
            flow.returnValue.getOrThrow()
            ResponseEntity.ok("Agreement $agreementNumber activated")
        } catch (e: TransactionVerificationException.ContractRejection) {
            ResponseEntity.badRequest().body("The Agreement was not activated ")
        }
    }

    @PostMapping(value = "/terminateAgreement", produces = arrayOf("text/plain"), headers = arrayOf("Content-Type=application/x-www-form-urlencoded"))
    fun terminateAgreement(@RequestParam("agreementNumber") agreementNumber: String, request: HttpServletRequest): ResponseEntity<String> {
        val agreementNumber = request.getParameter("agreementNumber")
        val flow = rpc.proxy.startFlowDynamic(TerminateAgreementFlow::class.java, agreementNumber)

        return try {
            flow.returnValue.getOrThrow()
            ResponseEntity.ok("Agreement $agreementNumber activated")
        } catch (e: TransactionVerificationException.ContractRejection) {
            ResponseEntity.badRequest().body("The Agreement was not terminated")
        }
    }

    @PostMapping(value = "/renewAgreement", produces = arrayOf("text/plain"), headers = arrayOf("Content-Type=application/x-www-form-urlencoded"))
    fun renweAgreement(@RequestParam("agreementNumber") agreementNumber: String, request: HttpServletRequest): ResponseEntity<String> {
        val agreementNumber = request.getParameter("agreementNumber")
        val flow = rpc.proxy.startFlowDynamic(RenewAgreementFlow::class.java, agreementNumber)

        return try {
            flow.returnValue.getOrThrow()
            ResponseEntity.ok("Agreement $agreementNumber activated")
        } catch (e: TransactionVerificationException.ContractRejection) {
            ResponseEntity.badRequest().body("The Agreement was not renewed")
        }
    }


    @PostMapping(value = "/amendAgreement", produces = arrayOf("text/plain"), headers = arrayOf("Content-Type=application/x-www-form-urlencoded"))
    fun amendAgreement(@RequestParam("agreementNumber") agreementNumber: String, request: HttpServletRequest): ResponseEntity<String> {
        val agreementNumber = request.getParameter("agreementNumber")
        val flow = rpc.proxy.startFlowDynamic(AmendAgreementFlow::class.java, agreementNumber)

        return try {
            flow.returnValue.getOrThrow()
            ResponseEntity.ok("Agreement $agreementNumber activated")
        } catch (e: TransactionVerificationException.ContractRejection) {
            ResponseEntity.badRequest().body("The Agreement was not amended}")
        }
    }




    @PostMapping(value = "/createApplication")
    fun createApplication(@RequestParam("applicationId") applicationId: String,
                        @RequestParam("applicationName") applicationName: String,
                        @RequestParam("industry") industry: String,
                        @RequestParam("applicationStatus") applicationStatus: ApplicationStatus,
                        @RequestParam("partyName") partyName: CordaX500Name?): ResponseEntity<Any?> {

        if (partyName == null) {
            return ResponseEntity.status(TSResponse.BAD_REQUEST).body("Query parameter 'counterPartyName' missing or has wrong format.\n")
        }

        val otherParty = proxy.wellKnownPartyFromX500Name(partyName)
                ?: return ResponseEntity.status(TSResponse.BAD_REQUEST).body("Party named $partyName cannot be found.\n")

        val (status, message) = try {


            val flowHandle = proxy.startFlowDynamic(CreateApplicationFlow.Initiator::class.java, applicationId, applicationName, industry, applicationStatus, otherParty)

            val result = flowHandle.use { it.returnValue.getOrThrow() }

            HttpStatus.CREATED to "Transaction id ${result.tx.id} committed to ledger.\n${result.tx.outputs.single().data}"

        } catch (e: Exception) {
            HttpStatus.BAD_REQUEST to e.message
        }
        logger.info(message)
        return ResponseEntity<Any?>(message, status)
    }


    @PostMapping(value = "/approveApplication", produces = arrayOf("text/plain"), headers = arrayOf("Content-Type=applcation/x-www-form-urlencoded"))
    fun approveApplication(@RequestParam("applicationId") applicationId: String, request: HttpServletRequest): ResponseEntity<String> {
        val applicationId = request.getParameter("applicationId")
        val flow = rpc.proxy.startFlowDynamic(ApproveApplicationFlow::class.java, applicationId)

        return try {
            flow.returnValue.getOrThrow()
            ResponseEntity.ok("Application $applicationId approved")
        } catch (e: TransactionVerificationException.ContractRejection) {
            ResponseEntity.badRequest().body("The Application was not approved. ")
        }
    }


    @PostMapping(value = "/rejectApplication", produces = arrayOf("text/plain"), headers = arrayOf("Content-Type=applcation/x-www-form-urlencoded"))
    fun rejectApplication(@RequestParam("applicationId") applicationId: String, request: HttpServletRequest): ResponseEntity<String> {
        val applicationId = request.getParameter("applicationId")
        val flow = rpc.proxy.startFlowDynamic(RejectApplicationFlow::class.java, applicationId)

        return try {
            flow.returnValue.getOrThrow()
            ResponseEntity.ok("Application $applicationId rejected")
        } catch (e: TransactionVerificationException.ContractRejection) {
            ResponseEntity.badRequest().body("The Application was not rejected ")
        }
    }


}