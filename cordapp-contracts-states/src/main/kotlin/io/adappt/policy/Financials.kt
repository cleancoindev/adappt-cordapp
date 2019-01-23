package io.adappt.policy

import net.corda.core.serialization.CordaSerializable

@CordaSerializable
data class Financials(val revenue: Int = 0,
                      val salary: Int = 0,
                      val options: Int = 0,
                      val bonus: Int = 0,
                      val debt: Int = 0,
                      val profit: Int = 0,
                      val cash: Int = 0,
                      val stock: Int = 0,
                      val bonds: Int = 0,
                      val credit: Int = 0,
                      val teikoku: Int = 0)