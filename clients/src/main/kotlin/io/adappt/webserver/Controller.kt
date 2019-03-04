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
import io.adappt.account.Account
import io.adappt.agreement.Agreement
import io.adappt.agreement.AgreementStatus
import io.adappt.agreement.AgreementType
import io.adappt.application.Application
import io.adappt.application.ApplicationStatus
import io.adappt.claim.Claim
import io.adappt.contact.Contact
import io.adappt.lead.Lead
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
        return kotlin.collections.mapOf("party" to party.name.organisation,
                                        "counterparty" to counterparty.name.organisation,
                                        "agreementName" to agreementName,
                                        "agreementNumber" to agreementNumber,
                                        "agreementStatus" to agreementStatus.toString(),
                                        "totalAgreementValue" to totalAgreementValue.toString(),
                                        "linearId" to linearId.toString(),
                                        "agreementType" to agreementType.toString())
    }


    /** Maps an Application to a JSON object. */

    private fun Application.toJson(): Map<String, String> {
        return kotlin.collections.mapOf(
                "agent" to agent.name.organisation,
                "provider" to provider.name.organisation,
                "counterparty" to provider.name.toString(),
                "applicationId" to applicationId,
                "applicationName" to applicationName,
                "industry" to industry,
                "applicationStatus" to applicationStatus.toString())
    }






    /** Maps an Claims to a JSON object. */

    private fun Claim.toJson(): Map<String, String> {
        return kotlin.collections.mapOf("application" to applicantNode.name.organisation,
                                        "insurer" to insurerNode.name.toString(),
                                        "claim" to referenceID)
    }



    /** Maps an Account to a JSON object. */

    private fun Account.toJson(): Map<String, String> {
        return kotlin.collections.mapOf(
                "accountId" to accountId,
                "accountName" to accountName,
                "accountType" to accountType,
                "industry" to industry,
                "phone" to phone,
                "owner" to owner.toString())
    }


    /** Maps an Contact to a JSON object. */

    private fun Contact.toJson(): Map<String, String> {
        return kotlin.collections.mapOf(
                "contactId" to contactId,
                "firstName" to firstName,
                "lastName" to lastName,
                "email" to email,
                "phone" to phone,
                "owner" to owner.toString())
    }


    /** Maps an Lead to a JSON object. */


    private fun Lead.toJson(): Map<String, String> {
        return kotlin.collections.mapOf(
                "leadId" to leadId,
                "firstName" to firstName,
                "lastName" to lastName,
                "email" to email,
                "phone" to phone,
                "owner" to owner.toString())
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
            //          @RequestParam("agreementStartDate") agreementStartDate: String,
            //          @RequestParam("agreementEndDate") agreementEndDate: String,
            //          @RequestParam("active") active: Boolean,
            //          @RequestParam("createdAt") createdAt: String,
            //          @RequestParam("lastUpdated") lastUpdated: String,
                        @RequestParam("counterpartyName") counterpartyName: String?): ResponseEntity<Any?> {

        if (totalAgreementValue <= 0) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("Query parameter 'total agreement value' must be non-negative.\n")
        }
        if (counterpartyName == null) {
            return ResponseEntity.status(TSResponse.BAD_REQUEST).body("Query parameter 'counterPartyName' missing or has wrong format.\n")
        }

        val counterparty = CordaX500Name.parse(counterpartyName)

        val otherParty = proxy.wellKnownPartyFromX500Name(counterparty)
                ?: return ResponseEntity.status(TSResponse.BAD_REQUEST).body("Party named $counterpartyName cannot be found.\n")

        val (status, message) = try {


            val flowHandle = proxy.startFlowDynamic(CreateAgreementFlow.Initiator::class.java, agreementNumber, agreementName, agreementStatus, agreementType, totalAgreementValue, otherParty)

            val result = flowHandle.use { it.returnValue.getOrThrow() }

            HttpStatus.CREATED to "Transaction id ${result.tx.id} committed to ledger.\n${result.tx.outputs.single().data}"

        } catch (e: Exception) {
            HttpStatus.BAD_REQUEST to e.message
        }
        logger.info(message)
        return ResponseEntity<Any?>(message, status)
    }


    @PostMapping(value = "/activateAgreement")
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

    @PostMapping(value = "/terminateAgreement")
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

    @PostMapping(value = "/renewAgreement")
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


    @PostMapping(value = "/amendAgreement")
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
                        @RequestParam("partyName") partyName: String?): ResponseEntity<Any?> {

        if (partyName == null) {
            return ResponseEntity.status(TSResponse.BAD_REQUEST).body("Query parameter 'counterPartyName' missing or has wrong format.\n")
        }

        val counterparty = CordaX500Name.parse(partyName)

        val otherParty = proxy.wellKnownPartyFromX500Name(counterparty)
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


    @PostMapping(value = "/approveApplication")
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


    @PostMapping(value = "/rejectApplication")
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



    /** Returns a list of existing Accounts. */

    @GetMapping(value = "/getAccounts", produces = arrayOf("application/json"))
    fun getAccounts(): List<Map<String, String>> {
        val accountStateAndRefs = rpc.proxy.vaultQueryBy<Account>().states
        val accountStates = accountStateAndRefs.map { it.state.data }
        return accountStates.map { it.toJson() }
    }




    /** Returns a list of existing Agreements. */

    @GetMapping(value = "/getContacts", produces = arrayOf("application/json"))
    fun getContacts(): List<Map<String, String>> {
        val contactStateAndRefs = rpc.proxy.vaultQueryBy<Contact>().states
        val contactStates = contactStateAndRefs.map { it.state.data }
        return contactStates.map { it.toJson() }
    }




    /** Returns a list of existing Agreements. */

    @GetMapping(value = "/getLeads", produces = arrayOf("application/json"))
    fun getLeads(): List<Map<String, String>> {
        val leadStateAndRefs = rpc.proxy.vaultQueryBy<Lead>().states
        val leadStates = leadStateAndRefs.map { it.state.data }
        return leadStates.map { it.toJson() }
    }




    /** Creates an Agreement. */

    @PostMapping(value = "/createAccount")
    fun createAccount(@RequestParam("accountId") accountId: String,
                      @RequestParam("accountName") accountName: String,
                      @RequestParam("accountType") accountType: String,
                      @RequestParam("industry") industry: String,
                      @RequestParam("phone") phone: String,
                      @RequestParam("owner") owner: String?): ResponseEntity<Any?> {


        if (owner == null) {
            return ResponseEntity.status(TSResponse.BAD_REQUEST).body("Query parameter 'counterPartyName' missing or has wrong format.\n")
        }

        val counterparty = CordaX500Name.parse(owner)

        val otherParty = proxy.wellKnownPartyFromX500Name(counterparty)
                ?: return ResponseEntity.status(TSResponse.BAD_REQUEST).body("Party named $owner cannot be found.\n")

        val (status, message) = try {

            val flowHandle = proxy.startFlowDynamic(CreateAccountFlow.Initiator::class.java, accountId, accountName, accountType, industry, phone, otherParty)

            val result = flowHandle.use { it.returnValue.getOrThrow() }

            HttpStatus.CREATED to "Transaction id ${result.tx.id} committed to ledger.\n${result.tx.outputs.single().data}"

        } catch (e: Exception) {
            HttpStatus.BAD_REQUEST to e.message
        }
        logger.info(message)
        return ResponseEntity<Any?>(message, status)
    }





    /** Creates a Contact. */

    @PostMapping(value = "/createContact")
    fun createContact(@RequestParam("contactId") contactId: String,
                      @RequestParam("firstName") firstName: String,
                      @RequestParam("lastName") lastName: String,
                      @RequestParam("email") email: String,
                      @RequestParam("phone") phone: String,
                      @RequestParam("owner") owner: String?): ResponseEntity<Any?> {


        if (owner == null) {
            return ResponseEntity.status(TSResponse.BAD_REQUEST).body("Query parameter 'counterPartyName' missing or has wrong format.\n")
        }

        val counterparty = CordaX500Name.parse(owner)

        val otherParty = proxy.wellKnownPartyFromX500Name(counterparty)
                ?: return ResponseEntity.status(TSResponse.BAD_REQUEST).body("Party named $owner cannot be found.\n")

        val (status, message) = try {

            val flowHandle = proxy.startFlowDynamic(CreateContactFlow.Initiator::class.java, contactId, firstName, lastName, email, phone, otherParty)

            val result = flowHandle.use { it.returnValue.getOrThrow() }

            HttpStatus.CREATED to "Transaction id ${result.tx.id} committed to ledger.\n${result.tx.outputs.single().data}"

        } catch (e: Exception) {
            HttpStatus.BAD_REQUEST to e.message
        }
        logger.info(message)
        return ResponseEntity<Any?>(message, status)
    }




    /** Creates a Lead. */

    @PostMapping(value = "/createLead")
    fun createLead(@RequestParam("leadId") leadId: String,
                   @RequestParam("firstName") firstName: String,
                   @RequestParam("lastName") lastName: String,
                   @RequestParam("email") email: String,
                   @RequestParam("phone") phone: String,
                   @RequestParam("owner") owner: String?): ResponseEntity<Any?> {


        if (owner == null) {
            return ResponseEntity.status(TSResponse.BAD_REQUEST).body("Query parameter 'counterPartyName' missing or has wrong format.\n")
        }

        val counterparty = CordaX500Name.parse(owner)

        val otherParty = proxy.wellKnownPartyFromX500Name(counterparty)
                ?: return ResponseEntity.status(TSResponse.BAD_REQUEST).body("Party named $owner cannot be found.\n")

        val (status, message) = try {

            val flowHandle = proxy.startFlowDynamic(CreateLeadFlow.Initiator::class.java, leadId, firstName, lastName, email, phone, otherParty)

            val result = flowHandle.use { it.returnValue.getOrThrow() }

            HttpStatus.CREATED to "Transaction id ${result.tx.id} committed to ledger.\n${result.tx.outputs.single().data}"

        } catch (e: Exception) {
            HttpStatus.BAD_REQUEST to e.message
        }
        logger.info(message)
        return ResponseEntity<Any?>(message, status)
    }



    /** Creates a Lead. */

    @PostMapping(value = "/createCase")
    fun createCase(@RequestParam("caseId") caseId: String,
                   @RequestParam("description") description: String,
                   @RequestParam("caseNumber") caseNumber: String,
                   @RequestParam("status") status: String,
                   @RequestParam("priority") priority: String,
                   @RequestParam("submitter") submitter: String,
                   @RequestParam("resolver") resolver: String?): ResponseEntity<Any?> {


        if (resolver == null) {
            return ResponseEntity.status(TSResponse.BAD_REQUEST).body("Query parameter 'counterPartyName' missing or has wrong format.\n")
        }

        val counterparty = CordaX500Name.parse(resolver)

        val otherParty = proxy.wellKnownPartyFromX500Name(counterparty)
                ?: return ResponseEntity.status(TSResponse.BAD_REQUEST).body("Party named $resolver cannot be found.\n")

        val (status, message) = try {

            val flowHandle = proxy.startFlowDynamic(CreateCaseFlow.Initiator::class.java, caseId, description, caseNumber, status, priority, submitter, otherParty)

            val result = flowHandle.use { it.returnValue.getOrThrow() }

            HttpStatus.CREATED to "Transaction id ${result.tx.id} committed to ledger.\n${result.tx.outputs.single().data}"

        } catch (e: Exception) {
            HttpStatus.BAD_REQUEST to e.message
        }
        logger.info(message)
        return ResponseEntity<Any?>(message, status)
    }
}