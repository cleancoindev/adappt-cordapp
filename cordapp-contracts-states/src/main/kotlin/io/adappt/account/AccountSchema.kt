package io.adappt.account

import net.corda.core.crypto.NullKeys
import net.corda.core.identity.AbstractParty
import net.corda.core.schemas.MappedSchema
import net.corda.core.schemas.PersistentState
import javax.persistence.Column
import javax.persistence.Entity
import javax.persistence.Index
import javax.persistence.Table

/**
 * The family of schemas for [AccountSchema].
 */

object AccountSchema

/**
 * First version of an [AccountSchema] schema.
 */


object AccountSchemaV1 : MappedSchema(AccountSchema.javaClass, 1, listOf(PersistentAccount::class.java)) {
    @Entity
    @Table(name = "accounts", indexes = arrayOf(Index(name = "idx_account_owner", columnList = "owner"),
            Index(name = "idx_account_accountName", columnList = "accountName")))
    class PersistentAccount(
            @Column(name = "accountId")
            var accountId: String,

            @Column(name = "accountName")
            var accountName: String,

            @Column(name = "accountType")
            var accountType: String,

            @Column(name = "industry")
            var industry: String,

            @Column(name = "phone")
            var phone: String,

            @Column(name = "owner")
            var owner: AbstractParty

    ) : PersistentState() {
        constructor() : this("default-constructor-required-for-hibernate", "", "", "", "", NullKeys.NULL_PARTY)
    }

}
