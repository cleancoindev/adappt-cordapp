package io.adappt.policy

import net.corda.core.serialization.CordaSerializable

/**
 * Basic measures of regular diet.
 * @param vegetables in cups per day
 * @param fruits in cups per day
 * @param meat in grams per day
 */
@CordaSerializable
data class Diet(val vegetables: Int = 0,
                val fruits: Int = 0,
                val meat: Int = 0,
                val carbohydrates: Int = 0,
                val protein: Int = 0,
                val fat: Int = 0,
                val alcohol: Int = 0,
                val caffeine: Int = 0,
                val wellness: Int = 0,
                val fish: Int = 0)