package xyz.deverse.evendilo.pipeline.stage

import xyz.deverse.evendilo.model.Model
import xyz.deverse.importer.ImportLine
import xyz.deverse.importer.ImportStrategy
import xyz.deverse.importer.Importer
import xyz.deverse.importer.pipeline.Pipeline.Stage

class ImportStage<T : Model> (val importer: Importer<T, ImportLine>, val strategy: ImportStrategy<T, ImportLine>) : Stage {

    @Synchronized
    override fun run() {
        importer.process(strategy)
    }

}