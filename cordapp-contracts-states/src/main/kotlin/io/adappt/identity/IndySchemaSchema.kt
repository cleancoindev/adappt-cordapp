package io.adappt.identity

import net.corda.core.schemas.MappedSchema
import net.corda.core.schemas.PersistentState
import javax.persistence.Entity

object IndySchemaSchema

object IndySchemaSchemaV1 : MappedSchema(
        version = 1,
        schemaFamily = IndySchemaSchema.javaClass,
        mappedTypes = listOf(PersistentSchema::class.java)
) {
    @Entity
    data class PersistentSchema(val id: String = "") : PersistentState() {
        constructor(schema: IndySchema) : this(schema.id)
    }
}