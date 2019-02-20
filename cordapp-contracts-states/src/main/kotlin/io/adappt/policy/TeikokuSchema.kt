package io.adappt.policy

import io.adappt.agreement.AgreementSchemaV1
import net.corda.core.schemas.MappedSchema
import net.corda.core.schemas.PersistentState
import javax.persistence.Column
import javax.persistence.Entity
import javax.persistence.Table


object TeikokuSchema

object TeikokuSchemaV1 : MappedSchema(
        schemaFamily = TeikokuSchema.javaClass,
        version = 1,
        mappedTypes = listOf(TeikokuSchemaV1.PersistentTeikoku::class.java)
) {

    @Entity
    @Table(name = "teikoku_states")
    class PersistentTeikoku(
            @Column(name = "TeikokuCompanyCode")
            var teikokuCompanyCode: String,
            @Column(name = "CompanyName")
            var companyName: String,
            @Column(name = "PostalCode")
            var postalCode: String,
            @Column(name = "CompanyAddress")
            var companyAddress: String,
            @Column(name = "TelephoneNumber")
            var telephoneNumber: String,
            @Column(name = "TypeOfEntity")
            var typeOfEntity: String,
            @Column(name = "TeikokuIndustrialClassification")
            var teikokuIndustrialClassification: String,
            @Column(name = "Domain")
            var domain: String,
            @Column(name = "DateOfEstablishment")
            var dateOfEstablishment: String,
            @Column(name = "DateOfIncorporation")
            var dateofIncorporation: String,
            @Column(name = "PaidInCapital")
            var paidInCapital: String,
            @Column(name = "NumberOfEmployees")
            var numberOfEmployees: String,
            @Column(name = "HoldingCompanyName")
            var holdingCompanyName: String,
            @Column(name = "PublicLimitedCompanyFlag")
            var publicLimitedCompanyFlag: String,
            @Column(name = "NumberOfShareholder")
            var numberOfShareholder: String,
            @Column(name = "ShareholderName")
            var shareholderName: String,
            @Column(name = "SecurityCode")
            var securityCode: String,
            @Column(name = "ForeignAffiliatedCompanyFlag")
            var foreignAffiliatedCompanyFlag: String,
            @Column(name = "NumberOfBranches")
            var numberOfBranches: String,
            @Column(name = "TradingBank")
            var tradingBank: String,
            @Column(name = "SupplierName")
            var supplierName: String,
            @Column(name = "CustomerName")
            var customerName: String,
            @Column(name = "TitleOfChiefExecutive")
            var titleOfCheifExecutive: String,
            @Column(name = "ChiefExecutiveName")
            var chiefExecutiveName: String,
            @Column(name = "ChiefExecutivePostalCode")
            var chiefExecutvePostalCode: String,
            @Column(name = "ChiefExecutiveHomeAddress")
            var chiefExeutiveHomeAddress: String,
            @Column(name = "ChiefExecutiveHomeTelephoneNumber")
            var chiefExecutiveHomeTelephoneNumber: String,
            @Column(name = "ChiefExecutiveBirthday")
            var chiefExecutiveBirthday: String,
            @Column(name = "ChiefExecutiveHomeTown")
            var chiefExecutiveHomeTown: String,
            @Column(name = "ChiefExecutiveEducation")
            var chiefExecutiveEducation: String,
            @Column(name = "ChiefExecutiveSex")
            var chiefExecutiveSex: String,
            @Column(name = "CreditScoring")
            var creditScoring: String,
            @Column(name = "ReportDate")
            var reportDate: String,
            @Column(name = "UpdateDate")
            var updateDate: String,
            @Column(name = "SalesRanking")
            var salesRanking: String
    ) : PersistentState() {
        @Suppress("UNUSED")
        constructor() : this(
                teikokuCompanyCode = "",
                companyName = "",
                postalCode = "",
                companyAddress = "",
                telephoneNumber = "",
                typeOfEntity = "",
                teikokuIndustrialClassification = "",
                domain = "",
                dateOfEstablishment = "",
                dateofIncorporation = "",
                paidInCapital = "",
                numberOfEmployees = "",
                holdingCompanyName = "",
                publicLimitedCompanyFlag = "",
                numberOfShareholder = "",
                shareholderName = "",
                securityCode = "",
                foreignAffiliatedCompanyFlag = "",
                numberOfBranches = "",
                tradingBank = "",
                supplierName = "",
                customerName = "",
                titleOfCheifExecutive = "",
                chiefExecutiveName = "",
                chiefExecutvePostalCode = "",
                chiefExeutiveHomeAddress = "",
                chiefExecutiveHomeTelephoneNumber = "",
                chiefExecutiveBirthday = "",
                chiefExecutiveHomeTown = "",
                chiefExecutiveEducation = "",
                chiefExecutiveSex = "",
                creditScoring = "",
                reportDate = "",
                updateDate = "",
                salesRanking = ""


        )
    }
}

