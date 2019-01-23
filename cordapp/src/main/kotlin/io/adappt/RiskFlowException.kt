package io.adappt

import net.corda.core.flows.FlowException

class RiskFlowException(message: String, cause: Throwable? = null) : FlowException(message, cause)