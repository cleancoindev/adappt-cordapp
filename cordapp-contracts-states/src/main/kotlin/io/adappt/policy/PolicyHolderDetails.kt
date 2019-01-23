package io.adappt.policy


import net.corda.core.serialization.CordaSerializable

/**
 * @param basics male, female or intersex
 * @param medical in years
 * @param financials in centimeters
 * @param diet in kilograms
 * @param exercise average rate in beats per minute
 * @param sleep average hours of sleep per night
 */


@CordaSerializable
data class PolicyHolderDetails(
        val basics: Basics,
        val medical: Medical,
        val financials: Financials,
        val diet: Diet = Diet(),
        val exercise: Float = 0.0f,
        val sleep: Float = 0.0f

)