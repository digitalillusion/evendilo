package xyz.deverse.evendilo.pipeline

import org.slf4j.event.Level
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import xyz.deverse.evendilo.importer.business.EntityFactory
import xyz.deverse.evendilo.importer.business.ImporterBusinessDelegate
import xyz.deverse.evendilo.logger
import xyz.deverse.evendilo.model.Family
import xyz.deverse.evendilo.model.Model
import xyz.deverse.evendilo.pipeline.stage.ImportStage
import xyz.deverse.evendilo.pipeline.stage.PersistStage
import xyz.deverse.importer.*
import xyz.deverse.importer.generic.ImportTag
import xyz.deverse.importer.pipeline.Pipeline
import java.util.*
import java.util.concurrent.atomic.AtomicInteger
import java.util.function.Consumer

@Service
class PipelineFactory<T : Model> (val importerBusinessDelegate: ImporterBusinessDelegate) {
    val logger = logger<PipelineFactory<T>>()
    /**
     * Create a persist pipeline given a type and a factory for the import strategy. The pipeline is composed of the following stages:
     *
     *  * **Import** Allows to use any import strategy (CSV, database...) in order to produce an entity to persist which is coherent with the current graph,
     * which means that has correctly resolved itself and its relationships if they exist and is filled up to the appropriate depth in order to be saved
     * without breaking the graph. Input data validation is applied at this pipeline
     *  * **Persist** perform the database-related operation in order to either save or update or else delete the entity. It also includes business-related
     * operation like rewiring the graph or changing other entities state in accordance to the operation going on. Business validation is applied at this pipeline
     *
     *
     * According to the strategy post-process condition, the persist pipeline may be called only when a whole collection of lines has been imported, or else after each line (default).
     *
     * @param importer
     * @param entityFactory
     * @param strategy
     * @return
     */
    fun createPersistPipeline(simpleClassName: String, importStrategyFactory: ImportStrategyFactory): AbstractPipeline<T> {
        val importer: Importer<T, ImportLine> = importerBusinessDelegate.getImporterFor(simpleClassName)
        return createPersistPipeline(importStrategyFactory, importer)
    }

    fun createPersistPipeline(simpleClassName: String, importStrategyFactory: ImportStrategyFactory, family: String, definition: String): AbstractPipeline<T> {
        val importer: Importer<T, ImportLine> = importerBusinessDelegate.getImporterFor(simpleClassName, family, definition)
        return createPersistPipeline(importStrategyFactory, importer)
    }

    private fun createPersistPipeline(importStrategyFactory: ImportStrategyFactory, importer: Importer<T, ImportLine>): AbstractPipeline<T> {
        val entityFactory: EntityFactory
        val optionalImportTag: Optional<out ImportTag> = importer.importTags.stream().filter { importTag -> importTag.javaClass == Family::class.java }.findFirst()
        entityFactory = if (optionalImportTag.isPresent) {
            importerBusinessDelegate.getEntityFactoryFor(optionalImportTag.get())
        } else {
            importerBusinessDelegate.getEntityFactoryFor(Family.Standard)
        }
        @Suppress("UNCHECKED_CAST")
        val strategy = importStrategyFactory.createImportStrategy(importer) as ImportStrategy<T, ImportLine>
        return when (strategy.postProcessCondition) {
            ImportStrategy.PostProcessCondition.ON_ALL_LINES -> createCollectionPersistPipeline(importer, entityFactory, strategy)
            ImportStrategy.PostProcessCondition.ON_EACH_LINE -> createStreamPersistPipeline(importer, entityFactory, strategy)
            else -> createStreamPersistPipeline(importer, entityFactory, strategy)
        }
    }

    @Transactional
    fun runInTransaction(pipeline: Pipeline<*>) {
        pipeline.run()
    }

    private fun createStreamPersistPipeline(importer: Importer<T, ImportLine>, entityFactory: EntityFactory, strategy: ImportStrategy<T, ImportLine>): AbstractPipeline<T> {
        val importStage: ImportStage<T> = ImportStage(importer, strategy)
        val persistStage: PersistStage<T> = resolvePersistStageForType(importer.nodeType)
        return object : AbstractPipeline<T>(importStage, persistStage) {
            @Suppress("UNCHECKED_CAST")
            override fun setup() {
                strategy.lineProcessors.add(Consumer { line : ImportLine -> persistLine(line.nodes as MutableCollection<T>, line.excludedIds, persistStage, line.actionType, line.saveDepth) })
            }

            override fun run() {
                importStage.run()
            }

            override fun getOutput(): MutableCollection<T> {
                return persistStage.target!!
            }

            override fun getErrors(): MutableCollection<out ImportLine> {
                return importStage.strategy.results.filter{ r -> Level.ERROR == r.severity }.toMutableList()
            }

            override val entityFactory: EntityFactory
                get() = entityFactory
        }
    }

    private fun createCollectionPersistPipeline(importer: Importer<T, ImportLine>, entityFactory: EntityFactory, strategy: ImportStrategy<T, ImportLine>): AbstractPipeline<T> {
        val importStage: ImportStage<T> = ImportStage(importer, strategy)
        val persistStage: PersistStage<T> = resolvePersistStageForType(importer.nodeType)
        return object : AbstractPipeline<T>(importStage, persistStage) {
            @Suppress("UNCHECKED_CAST")
            override fun run() {
                importStage.run()
                strategy.results.forEach { line -> persistLine(line.nodes as MutableCollection<T>, line.excludedIds, persistStage, line.actionType, line.saveDepth) }
            }

            override val entityFactory: EntityFactory
                get() = entityFactory

            override fun getOutput(): MutableCollection<T> {
                return persistStage.target!!
            }

            override fun getErrors(): MutableCollection<out ImportLine> {
                return importStage.strategy.results.filter{ r -> Level.ERROR == r.severity }.toMutableList()
            }
        }
    }

    private fun persistLine(lineNodes: MutableCollection<T>, excludedIds: MutableSet<Long>, persistStage: PersistStage<T>, actionType: ActionType, saveDepth: AtomicInteger) {
        persistStage.target = lineNodes
        persistStage.actionType = actionType
        persistStage.saveDepth = saveDepth.get()
        persistStage.excludedIds = excludedIds
        persistStage.run()
    }

    private fun resolvePersistStageForType(type: Class<out Model?>?): PersistStage<T> {
        // if (Equipment::class.java.isAssignableFrom(type)) {
        //    return equipmentPersistStage as PersistStage<T?>?
        //}
        return object : PersistStage<T>() {
            override fun run() {
                logger.info("Default PersistStage does nothing")
            }
        }
    }
}