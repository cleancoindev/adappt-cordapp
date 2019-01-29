package io.adappt.policy

import net.corda.core.serialization.CordaSerializable
import java.util.*

/**
 * @param sex male, female or intersex
 * @param age in years
 * @param height in centimeters
 * @param weight in kilograms
 * @param heartRate average rate in beats per minute
 */
@CordaSerializable
data class Teikoku(val TeikokuCompanyCode: String,
                   val CompanyName: String,
                   val PostalCode: String,
                   val CompanyAddress: String,
                   val TelephoneNumber: String,
                   val TypeOfEntity: String,
                   val TeikokuIndustrialClassification: String,
                   val Domain: String,
                   val DateOfEstablishment: Date,
                   val DateOfIncorporation: Date,
                 //  val OperatingRecords: OperatingRecords,
                   val PaidInCapital: String,
                   val NumberOfEmployees: String,
                   val HoldingCompanyName: String,
                   val PublicLimitedCompanyFlag: String,
                   val NumberOfShareholder: String,
                   val ShareholderName: String,
                   val SecurityCode: String,
                   val ForeignAffiliatedCompanyFlag: String,
                   val NumberOfBranches: String,
                   val TradingBank: String,
                   val SupplierName: String,
                   val CustomerName: String,
                   val TitleOfChiefExecutive: String,
                   val ChiefExecutiveName: String,
                   val ChiefExecutivePostalCode: String,
                   val ChiefExecutiveHomeAddress: String,
                   val ChiefExecutiveHomeTelephoneNumber: String,
                   val ChiefExeutiveBirthday: String,
                   val ChiefExecutiveHomeTown: String,
                   val ChiefExecutiveEducation: String,
                   val ChiefExecutiveSex: String,
                   val CreditScoring: String,
                   val ReportDate: Date,
                   val UpdateDate: Date,
                   val SalesRanking: String)