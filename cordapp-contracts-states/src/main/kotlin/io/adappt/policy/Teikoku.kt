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

import net.corda.core.serialization.CordaSerializable
import java.util.*

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