package io.adappt.utils

import net.corda.core.contracts.CommandData
import net.corda.core.contracts.TypeOnlyCommandData
import net.corda.core.contracts.*
import net.corda.core.transactions.LedgerTransaction
import kotlin.reflect.KClass
import co.paralleluniverse.fibers.Suspendable
import net.corda.core.contracts.ContractClassName
import net.corda.core.contracts.LinearState
import net.corda.core.contracts.StateAndRef
import net.corda.core.flows.FinalityFlow
import net.corda.core.flows.FlowLogic
import net.corda.core.identity.Party
import net.corda.core.node.services.vault.QueryCriteria
import net.corda.core.transactions.SignedTransaction
import net.corda.core.transactions.TransactionBuilder

interface CrudCommands : CommandData {
    class Create : TypeOnlyCommandData(), CrudCommands
    class Update : TypeOnlyCommandData(), CrudCommands
    class Delete : TypeOnlyCommandData(), CrudCommands
}

abstract class CrudContract<T : LinearState>(private val stateClazz: KClass<T>): Contract {
    val contractClassName : ContractClassName = stateClazz.java.name
    override fun verify(tx: LedgerTransaction) {
        val command = tx.commands.requireSingleCommand<CrudCommands>()
        val groups = tx.groupStates(stateClazz.java) { it: T -> it.linearId }
        for ((inputs, outputs, _) in groups) {
            when (command.value) {
                is CrudCommands.Create -> requireThat {
                    "there are no inputs" using (inputs.isEmpty())
                    "there are one or more outputs" using (outputs.isNotEmpty())
                }
                is CrudCommands.Update -> {
                    requireThat {
                        "there are one or more inputs that match in size to outputs" using
                                (inputs.isNotEmpty() && inputs.size == outputs.size)
                    }
                    requireThat {
                        "that the identifiers in the inputs match the outputs" using
                                (inputs.map { it.linearId.id }.toSet() == outputs.map { it.linearId.id }.toSet())
                    }
                }
                is CrudCommands.Delete -> {
                    requireThat { "there are one or more inputs" using (inputs.isNotEmpty())}
                    requireThat { "there are zero outputs" using (outputs.isEmpty())}
                }
            }
        }
    }
}


class CrudCreateFlow<T : LinearState>(
        private val clazz: Class<T>,
        private val states: List<T>,
        private val contractClassName: ContractClassName,
        private val notary: Party
) : FlowLogic<SignedTransaction>() {
    @Suspendable
    override fun call(): SignedTransaction {
        val ids = states.filter { it.linearId.externalId != null }.map { it.linearId.externalId!! }
        val qc = QueryCriteria.LinearStateQueryCriteria(externalId = ids)
        serviceHub.vaultService.queryBy(clazz, qc).apply {
            if (states.isNotEmpty()) {
                throw RuntimeException("cannot create the following $contractClassName because they exist: ${states.map { it.state.data.linearId.externalId }.joinToString(", ")}")
            }
        }
        val txb = TransactionBuilder(notary)
        val stx = serviceHub.signInitialTransaction(txb.apply {
            addCommand(CrudCommands.Create(), ourIdentity.owningKey)
            states.forEach { addOutputState(it, contractClassName) }
        })
        val secureHash = subFlow(FinalityFlow(stx)).id
        return waitForLedgerCommit(secureHash)
    }
}

class CrudUpdateFlow<T : LinearState>(
        private val inputStates: List<StateAndRef<T>>,
        private val states: List<T>,
        private val contractClassName: ContractClassName,
        private val notary: Party
) : FlowLogic<SignedTransaction>() {
    @Suspendable
    override fun call(): SignedTransaction {
        val txb = TransactionBuilder(notary)
        val stx = serviceHub.signInitialTransaction(txb.apply {
            addCommand(CrudCommands.Update(), ourIdentity.owningKey)
            inputStates.forEach { addInputState(it) }
            states.forEach { addOutputState(it, contractClassName) }
        })
        val secureHash = subFlow(FinalityFlow(stx)).id
        return waitForLedgerCommit(secureHash)
    }
}
