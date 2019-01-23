package io.adappt.agreement

import net.corda.core.schemas.MappedSchema
import net.corda.core.schemas.PersistentState
import javax.persistence.Column
import javax.persistence.Entity
import javax.persistence.Table


object AgreementSchema

object AgreementSchemaV1 : MappedSchema(
        schemaFamily = AgreementSchema.javaClass,
        version = 1,
        mappedTypes = listOf(PersistentAgreement::class.java)
) {

    @Entity
    @Table(name = "agreement_states")
    class PersistentAgreement(
            @Column(name = "agreementNumber")
            var agreementNumber: String,
            @Column(name = "agreementName")
            var agreementName: String,
            @Column(name = "agreementStatus")
            var agreementStatus: String,
            @Column(name = "agreementType")
            var agreementType: String,
            @Column(name = "totalAgreementValue")
            var totalAgreementValue: String,
            @Column(name = "party")
            var party: String,
            @Column(name = "counterparty")
            var counterparty: String,
            //     @Column(name = "agreementStartDate")
            //     var agreementStartDate: String,
            //     @Column(name = "agreementEndDate")
            //     var agreementEndDate: String,
            //     @Column(name = "active")
            //     var active: String,
            //     @Column(name = "createdAt")
            //     var createdAt: String,
            //     @Column(name = "lastUpdated")
            //    var lastUpdated: String,
            @Column(name = "linear_id")
            var linearId: String,
            @Column(name = "external_Id")
            var externalId: String
    ) : PersistentState() {
        @Suppress("UNUSED")
        constructor() : this(
                agreementNumber = "",
                agreementName = "",
                agreementStatus = "",
                agreementType = "",
                totalAgreementValue = "",
                party = "",
                counterparty = "",
                //           agreementStartDate = "",
                //          agreementEndDate = "",
                //          active = "",
                //           createdAt = "",
                //            lastUpdated = "",
                linearId = "",
                externalId = ""
        )
    }
}