package io.adappt.identity

import net.corda.core.schemas.MappedSchema
import net.corda.core.schemas.PersistentState
import javax.persistence.Column
import javax.persistence.Entity
import javax.persistence.Table

object CredentialProofSchema

object CredentialProofSchemaV1 : MappedSchema(
        version = 1,
        schemaFamily = CredentialProofSchema.javaClass,
        mappedTypes = listOf(PersistentProof::class.java)
) {

    @Entity
    @Table(name = "proofs")
    class PersistentProof(
            @Column(name = "id")
            val id: String
    ) : PersistentState() {
        constructor(indyProof: IndyCredentialProof) : this(indyProof.id)
        constructor() : this("")
    }
}