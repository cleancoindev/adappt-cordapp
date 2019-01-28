package io.adappt.utils


import com.fasterxml.jackson.annotation.JsonInclude
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.PropertyNamingStrategy
import com.fasterxml.jackson.module.kotlin.KotlinModule


/**
 * Object that makes serialization simplier
 */
object SerializationUtils {
    val mapper: ObjectMapper = ObjectMapper().registerModule(KotlinModule())

    init {
        mapper.propertyNamingStrategy = PropertyNamingStrategy.SNAKE_CASE
        mapper.setSerializationInclusion(JsonInclude.Include.NON_NULL)
    }

    fun anyToJSON(obj: Any?): String = mapper.writeValueAsString(obj)
    fun anyToBytes(obj: Any?): ByteArray = mapper.writeValueAsBytes(obj)

    inline fun <reified T> jSONToAny(json: String): T = mapper.readValue(json, T::class.java)
    inline fun <reified T> bytesToAny(bytes: ByteArray): T = mapper.readValue(bytes, T::class.java)

    fun <T : Any> jSONToAny(json: String, clazz: Class<T>): T = mapper.readValue(json, clazz)
    fun <T : Any> bytesToAny(bytes: ByteArray, clazz: Class<T>): T = mapper.readValue(bytes, clazz)
}