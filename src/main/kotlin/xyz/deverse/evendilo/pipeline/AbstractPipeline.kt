package xyz.deverse.evendilo.pipeline

import xyz.deverse.evendilo.importer.business.EntityFactory
import xyz.deverse.evendilo.model.Model
import xyz.deverse.importer.pipeline.Pipeline
import java.util.*

abstract class AbstractPipeline<T : Model>(vararg stages: Pipeline.Stage) : Pipeline<T> {
    private val stages: List<Pipeline.Stage> = listOf(*stages)

    /**
     * One time initialization of the pipeline happening right after it has been constructed.
     * Useful in case of anonymous implementation of the `AbstractPipeline` class
     */
    protected open fun setup() {
        // Do nothing
    }

    override fun <S : Pipeline.Stage> getStageByType(stageType: Class<S>): Optional<S> {
        return stages.stream().filter { s -> stageType.isAssignableFrom(s.javaClass) }.findAny() as Optional<S>
    }

    fun <S : Pipeline.Stage> getStageByPosition(position: Int): Optional<S> {
        return if (position > 0 && position < stages.size) {
            Optional.of(stages[position] as S)
        } else Optional.empty()
    }

    abstract val entityFactory: EntityFactory

    init {
        setup()
    }
}