package io.adappt.charge

import net.corda.core.identity.Party
import net.corda.core.schemas.MappedSchema
import net.corda.core.schemas.PersistentState
import java.util.*
import javax.persistence.Column
import javax.persistence.Entity
import javax.persistence.Table


object ChargeSchema

object ChargeSchemaV1 : MappedSchema(
        schemaFamily = ChargeSchema.javaClass,
        version = 1,
        mappedTypes = listOf(PersistentCharge::class.java)) {
    @Entity
    @Table(name = "charge_states")
    class PersistentCharge(
            @Column(name = "accountId")
            var accountId: String,

            @Column(name = "amount")
            var amount: String,

            @Column(name = "currency")
            var currency: Int,

            @Column(name = "party")
            var party: Party,

            @Column(name = "counterParty")
            var counterParty: Party,

            @Column(name = "paid")
            var paid: String,

            @Column(name = "status")
            var status: String,

            @Column(name = "linearId")
            var linearId: UUID

    ) : PersistentState()
}