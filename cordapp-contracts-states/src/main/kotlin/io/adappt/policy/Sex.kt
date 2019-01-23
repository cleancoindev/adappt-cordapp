package io.adappt.policy

import net.corda.core.serialization.CordaSerializable

@CordaSerializable
enum class Sex {
    MALE,
    FEMALE,
    INTERSEX
}