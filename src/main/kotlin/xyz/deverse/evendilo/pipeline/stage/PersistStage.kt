package xyz.deverse.evendilo.pipeline.stage

import org.springframework.transaction.support.TransactionSynchronization
import org.springframework.transaction.support.TransactionSynchronizationManager
import xyz.deverse.evendilo.logger
import xyz.deverse.evendilo.model.Model
import xyz.deverse.importer.ActionType
import xyz.deverse.importer.ImportMapper
import xyz.deverse.importer.StrategyPostProcessor
import xyz.deverse.importer.pipeline.Pipeline
import java.util.*
import java.util.function.Consumer
import kotlin.collections.HashMap
import kotlin.collections.HashSet

abstract class PersistStage<T : Model>() : Pipeline.Stage, StrategyPostProcessor<T> {
    private val logger = logger<PersistStage<T>>()

    enum class PersistActionType {
        DELETE, DETACH, SAVE, UPDATE
    }

    class PersistStageAction<T : Model, U : Model?>(val actionType: PersistActionType, private val retriever: ((T) -> U?)? = null, private val computer: ((T, U?) -> Unit)? = null, private val updater: ((T, U?) -> T)? = null, val postProcessor: ((Collection<T>, Collection<Model>) -> Collection<T>)? = null) {
        private val logger = logger<PersistStageAction<T, U>>()

        var previous: U? = null
        fun execute(target: T): T {
            previous = retriever?.invoke(target)
            computer?.invoke(target, previous)
            return updater?.invoke(target, previous) ?: target
        }

        fun log(actionTarget: T) {
            logger.info(String.format("%s %s=%s", actionType.toString(), actionTarget.javaClass.simpleName, actionTarget.id))
        }
    }

    private val actions: HashMap<PersistActionType, PersistStageAction<T, Model>> = HashMap()
    var excludedIds: MutableSet<Long> = mutableSetOf()
    var target: MutableCollection<T>? = null
    var actionType: ActionType? = null
    var saveDepth = 0

    override fun run() {
        val postProcessors: HashMap<PersistStageAction<T, Model>, ((List<T>, Collection<Model>) -> Collection<T>)> = HashMap()
        val nodesPerAction = HashMap<PersistStageAction<T, Model>, List<T>>()
        val targetsToUpdate: MutableCollection<T> = HashSet()
        val previousNodes: MutableList<Model> = ArrayList()
        iterateTarget { target: T, _: Boolean ->
            if (excludedIds.contains(target.id)) {
                logger.debug("Skipping Node " + target.id + " since it is excluded")
                return@iterateTarget
            }
            val persistAction: PersistActionType = if (ActionType.PERSIST == actionType) {
                if (target.id == null) PersistActionType.SAVE else PersistActionType.UPDATE
            } else {
                PersistActionType.valueOf(actionType.toString())
            }
            val action = actions[persistAction]
                    ?: throw IllegalArgumentException(String.format("Action type %s not implemented", persistAction.toString()))
            val actionTarget = action.execute(target)
            action.previous?.let { previousNodes.add(it) }
            if (action.postProcessor != null) {
                val pp: ((List<T>, Collection<Model>) -> Collection<T>) = action.postProcessor
                postProcessors[action] = pp
                nodesPerAction.merge(
                        action,
                        mutableListOf(actionTarget)
                ) { x, y -> x + y }
            } else {
                targetsToUpdate.add(actionTarget)
                action.log(actionTarget)
            }
        }
        postProcessors.forEach { (action, postProcessor) ->
            val targetNodes = nodesPerAction.getOrDefault(action, ArrayList())
            val postProcessed: Collection<T> = postProcessor(targetNodes, previousNodes)
            postProcessed.forEach { targetUpdated -> action.log(targetUpdated) }
            targetsToUpdate.addAll(postProcessed)
        }
        target = targetsToUpdate
    }

    protected fun executeAfterTransactionCommits(task: Runnable?) {
        if (TransactionSynchronizationManager.isSynchronizationActive()) {
            TransactionSynchronizationManager.registerSynchronization(object : TransactionSynchronization {
                override fun afterCommit() {
                    task!!.run()
                }
            })
        } else {
            task!!.run()
        }
    }

    protected fun addAction(persistStageAction: PersistStageAction<T, Model>) {
        actions[persistStageAction.actionType] = persistStageAction
    }

    private fun iterateTarget(consumer: (T, Boolean) -> Unit) {
        val targetIterator = target!!.iterator()
        logger.info(
            "Calling action " + actionType + " for " + target!!.size + " elements: " +
                    target!!.joinToString(", ", "[", "]") { t -> t.id.toString() }
        )
        while (targetIterator.hasNext()) {
            val current = targetIterator.next()
            consumer(current, targetIterator.hasNext())
        }
    }
}