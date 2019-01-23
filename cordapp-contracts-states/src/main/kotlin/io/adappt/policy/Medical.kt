package io.adappt.policy

import net.corda.core.serialization.CordaSerializable

@CordaSerializable
data class Medical(val smoker: Boolean,
                   val drinker: Boolean,
                   val urine: Int = 0,
                   val bloodPressure: Int = 0,
                   val blood: Int = 0,
                   val exercise: Int = 0,
                   val activity: Int = 0)