package io.adappt.application

import net.corda.core.schemas.MappedSchema
import net.corda.core.schemas.PersistentState
import javax.persistence.Column
import javax.persistence.Entity
import javax.persistence.Table


object ApplicationSchema

object ApplicationSchemaV1 : MappedSchema(
        schemaFamily = ApplicationSchema.javaClass,
        version = 1,
        mappedTypes = listOf(PersistentApplication::class.java)) {
    @Entity
    @Table(name = "application_states")
    class PersistentApplication(
            @Column(name = "applicationId")
            var applicationId: String,

            @Column(name = "applicationName")
            var applicationName: String,

            @Column(name = "industry")
            var industry: String,

            @Column(name = "applicationStatus")
            var applicationStatus: String,

            @Column(name = "agent")
            var agent: String,

            @Column(name = "provider")
            var provider: String,

            @Column(name = "linearId")
            var linearId: String
    ) : PersistentState() {
        @Suppress("UNUSED")
        constructor() : this(
                applicationId = "",
                applicationName = "",
                industry = "",
                applicationStatus = "",
                agent = "",
                provider = "",
                linearId = ""
        )
    }
}