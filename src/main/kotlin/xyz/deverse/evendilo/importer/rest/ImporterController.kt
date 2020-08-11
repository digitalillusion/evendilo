package xyz.deverse.evendilo.importer.rest

import io.swagger.annotations.ApiOperation
import io.swagger.annotations.ApiParam
import org.springframework.data.rest.core.annotation.RestResource
import org.springframework.messaging.simp.SimpMessageSendingOperations
import org.springframework.stereotype.Component
import org.springframework.transaction.interceptor.TransactionAspectSupport
import org.springframework.web.bind.annotation.*
import org.springframework.web.multipart.MultipartFile
import xyz.deverse.evendilo.importer.business.ImporterBusinessDelegate
import xyz.deverse.evendilo.importer.strategy.CsvImportStrategyFactory
import xyz.deverse.evendilo.logger
import xyz.deverse.evendilo.model.Destination
import xyz.deverse.evendilo.model.Family
import xyz.deverse.evendilo.model.Model
import xyz.deverse.evendilo.pipeline.PipelineFactory
import xyz.deverse.evendilo.pipeline.stage.ImportStage
import xyz.deverse.importer.ImportLine
import xyz.deverse.importer.ImportStrategy
import xyz.deverse.importer.Importer
import xyz.deverse.importer.ImporterProcessStatus
import xyz.deverse.importer.generic.ImportTag
import xyz.deverse.importer.pipeline.Pipeline
import java.time.Duration
import java.time.Instant
import java.util.*
import java.util.function.Consumer
import javax.servlet.http.HttpSession

@Component
object ImporterProcessStatusCache : HashMap<String, Map<String, ImporterProcessStatus>>() {
    private const val serialVersionUID = 953060606304532374L
}

@RestController
@CrossOrigin
class ImporterController(val importerProcessStatusCache: ImporterProcessStatusCache, val csvImportStrategyFactory: CsvImportStrategyFactory, val httpSession: HttpSession, val messagingTemplate: SimpMessageSendingOperations, val pipelineFactory: PipelineFactory<out Model>, val importerBusinessDelegate: ImporterBusinessDelegate) {
    val logger = logger<ImporterController>()

    @ApiOperation("Abort the ongoing import of an entity, returning the current status if one is present in the session")
    @DeleteMapping("/import/{family}/{destination}")
    fun importerProcessAbort( 
            @ApiParam(value = "The family of the entity", allowableValues = "standard", allowEmptyValue = false) @PathVariable("family") family: Family,
            @ApiParam(value = "The destination of the entity", allowableValues = "woocommerce", allowEmptyValue = false) @PathVariable("destination") destination: Destination): ImporterProcessStatus {
        val importTags: List<ImportTag> = listOf(family, destination)
        val cacheId = ImporterProcessStatus.getCacheId(importTags)
        val oldImporterProcessStatus = retrieve(cacheId)
                ?: return importerProcessRestart(family, destination)
        if (oldImporterProcessStatus.isStarted && !oldImporterProcessStatus.isCompleted && !oldImporterProcessStatus.isAborted) {
            val newImporterProcessStatus = ImporterProcessStatus.builder()
                    .categories(getImportedTypes(importTags))
                    .completed(false)
                    .importTags(importTags)
                    .results(oldImporterProcessStatus.results)
                    .selected(oldImporterProcessStatus.selected)
                    .started(true)
                    .aborted(true)
                    .filename(oldImporterProcessStatus.filename)
                    .build()
            logger.info(httpSession.id + " [DELETE importerProcessAbort] ")
            logger.debug(httpSession.id + " [DELETE importerProcessAbort] " + cacheId + "=" + oldImporterProcessStatus.toString())
            update(cacheId, newImporterProcessStatus)
            return newImporterProcessStatus
        }
        return oldImporterProcessStatus
    }

    @ApiOperation("Restart the import of an entity")
    @PutMapping("/import/{family}/{destination}")
    fun importerProcessRestart( 
            @ApiParam(value = "The family of the entity", allowableValues = "standard", allowEmptyValue = false) @PathVariable("family") family: Family,
            @ApiParam(value = "The destination of the entity", allowableValues = "woocommerce", allowEmptyValue = false) @PathVariable("destination") destination: Destination): ImporterProcessStatus {
        val importTags: List<ImportTag> = listOf(family, destination)
        val cacheId = ImporterProcessStatus.getCacheId(importTags)
        var importerProcessStatus: ImporterProcessStatus? = retrieve(cacheId)
        if (importerProcessStatus == null || !importerProcessStatus.isAborted || importerProcessStatus.isCompleted) {
            val newImporterProcessStatus = ImporterProcessStatus.builder() 
                    .categories(getImportedTypes(importTags)) 
                    .importTags(importTags)
                    .build()
            update(cacheId, newImporterProcessStatus)
            importerProcessStatus = newImporterProcessStatus
        }
        logger.info(httpSession.id + " [GET importerProcessRestart] " )
        val loggedStatus = importerProcessStatus.toString()
        logger.debug(httpSession.id + " [GET importerProcessRestart] " + cacheId + "=" + loggedStatus)
        return importerProcessStatus!!
    }

    @ApiOperation("Retrieve the status of the ongoing import of an entity, or a blank status if none is present in the session")
    @GetMapping("/import/{family}/{destination}")
    fun importerProcessStatus(
            @ApiParam(value = "The family of the entity", allowableValues = "standard", allowEmptyValue = false) @PathVariable("family") family: Family,
            @ApiParam(value = "The destination of the entity", allowableValues = "woocommerce", allowEmptyValue = false) @PathVariable("destination") destination: Destination): ImporterProcessStatus {
        val importTags: List<ImportTag> = listOf(family, destination)
        val cacheId = ImporterProcessStatus.getCacheId(importTags)
        val importerProcessStatus = retrieve(cacheId)
                ?: return importerProcessRestart(family, destination)
        logger.info(httpSession.id + " [GET importerProcessStatus] ")
        logger.debug(httpSession.id + " [GET importerProcessStatus] " + cacheId + "=" + importerProcessStatus.toString())
        return importerProcessStatus.withoutResultNodes()
    }

    @ApiOperation("Start the import of an entity")
    @PostMapping("/import/{family}/{destination}/{entityName}")
    fun <T : Model> uploadFileHandler(
            @ApiParam(value = "The family of the entity", allowableValues = "standard", allowEmptyValue = false) @PathVariable("family") family: Family,
            @ApiParam(value = "The destination of the entity", allowableValues = "woocommerce", allowEmptyValue = false) @PathVariable("destination") destination: Destination,
            @ApiParam(value = "The class name of the entity", allowEmptyValue = false) @PathVariable("entityName") entityName: String,
            @ApiParam(value = "The file containing the definitions", allowEmptyValue = false) @RequestParam("file") file: MultipartFile): ImporterProcessStatus? {
        csvImportStrategyFactory.csvFile = file
        val pipeline = pipelineFactory.createPersistPipeline(entityName, csvImportStrategyFactory, family.toString(), destination.toString()) as Pipeline<T>
        val importStage: ImportStage<T> = pipeline.getStageByType(ImportStage::class.java).get() as ImportStage<T>
        val strategy: ImportStrategy<T, out ImportLine> = importStage.strategy
        val importer: Importer<T, out ImportLine> = importStage.importer
        val cacheId = ImporterProcessStatus.getCacheId(importer.importTags)
        var oldImporterProcessStatus: ImporterProcessStatus? = retrieve(cacheId)
        var newImporterProcessStatus: ImporterProcessStatus
        synchronized(importerProcessStatusCache) {
            val concurrentProcesses: List<ImporterProcessStatus> = importerProcessStatusCache.values
                    .flatMap { processesBySession -> processesBySession.values }
                    .filter { process -> cacheId == ImporterProcessStatus.getCacheId(process.importTags) }
                    .toList()
            if (concurrentProcesses.stream().anyMatch { obj: ImporterProcessStatus -> obj.isStarted }) {
                throw ConcurrentModificationException("An import for the same process $cacheId is already ongoing for another session")
            }
            if (oldImporterProcessStatus == null || oldImporterProcessStatus!!.isCompleted || oldImporterProcessStatus!!.isAborted) {
                val iFamily = importer.importTags.find { importTag -> Family::class.sealedSubclasses.contains(importTag::class) }
                val iDestination = importer.importTags.find { importTag -> Destination::class.sealedSubclasses.contains(importTag::class) }
                oldImporterProcessStatus = importerProcessRestart(Family.valueOf(iFamily.toString()), Destination.valueOf(iDestination.toString()))
            }
            newImporterProcessStatus = ImporterProcessStatus.builder()
                    .categories(oldImporterProcessStatus!!.categories)
                    .completed(false)
                    .importTags(importer.importTags)
                    .results(ArrayList())
                    .selected(oldImporterProcessStatus!!.selected)
                    .filename(file.originalFilename)
                    .started(true)
                    .aborted(false)
                    .build()
            update(cacheId, newImporterProcessStatus)
        }

        logger.info(httpSession.id + " [POST uploadFileHandler] ")
        logger.debug(httpSession.id + " [POST uploadFileHandler] " + cacheId + "=" + newImporterProcessStatus.toString())

        performImportFile(pipeline, strategy, importer, cacheId)
        return retrieve(cacheId)
    }

    private fun getImportedTypes(importTags: List<ImportTag>): SortedSet<String> {
        val familyImporters: Set<Importer<out Model, out ImportLine>>? = importerBusinessDelegate.importers
                .filter { importer : Importer<*, *> -> importer.isMatching(importTags) }
                .filter { importer : Importer<*, *> ->
                    val restResource: RestResource? = importer.javaClass.getAnnotation(RestResource::class.java)
                    restResource == null || restResource.exported
                }
                .toSet()
        val importedTypes: SortedSet<String> = TreeSet()
        if (familyImporters != null) {
            for (importer in familyImporters) {
                val typeFromImporter = importer.javaClass.simpleName.replace("Importer", "")
                var importedType = if (typeFromImporter.isEmpty()) importer.nodeType.simpleName else typeFromImporter
                if (importedType.indexOf("$$") > 0) {
                    importedType = importedType.substring(0, importedType.indexOf("$$"))
                }
                importedTypes.add(importedType)
            }
        }
        return importedTypes
    }

    private fun retrieve(cacheId: String): ImporterProcessStatus? {
        return importerProcessStatusCache.getOrDefault(httpSession.id, emptyMap()).getOrDefault(cacheId, null)
    }

    private fun update(cacheId: String, newImporterProcessStatus: ImporterProcessStatus) {
        synchronized(importerProcessStatusCache) {
            val maxInactiveInterval = httpSession.maxInactiveInterval
            val currentInactiveTime = (System.currentTimeMillis() - httpSession.lastAccessedTime / 1000).toInt()
            httpSession.maxInactiveInterval = maxInactiveInterval + currentInactiveTime
            val sessionCache: MutableMap<String, ImporterProcessStatus> = (importerProcessStatusCache[httpSession.id] ?: HashMap()).toMutableMap()
            sessionCache[cacheId] = newImporterProcessStatus
            importerProcessStatusCache[httpSession.id] = sessionCache
        }
    }

    fun <T : Model?> performImportFile(pipeline: Pipeline<T>, strategy: ImportStrategy<T, out ImportLine>, importer: Importer<T, out ImportLine>, cacheId: String) {
        strategy.lineProcessors.add(0, Consumer { line: ImportLine ->
            val oldImporterProcessStatus = retrieve(cacheId)
            if (oldImporterProcessStatus != null) {
                if (oldImporterProcessStatus.isAborted) {
                    strategy.abort()
                }
                val results: MutableList<ImportLine> = ArrayList()
                results.addAll(oldImporterProcessStatus.results)
                results.add(line)
                val lastLine = !strategy.hasNext()
                val now = Instant.now()
                var lastTimestamp = if (oldImporterProcessStatus.timestamp != null) Instant.ofEpochMilli(oldImporterProcessStatus.timestamp) else null
                var lastLinesSent = oldImporterProcessStatus.linesSent
                if (lastTimestamp == null || Duration.between(lastTimestamp, now).toMillis() > INTER_MESSAGE_PERIOD_MILLIS) {
                    lastLinesSent = results.size
                    lastTimestamp = now
                }
                val newImporterProcessStatus = ImporterProcessStatus.builder()
                        .categories(oldImporterProcessStatus.categories)
                        .timestamp(lastTimestamp!!.toEpochMilli())
                        .completed(lastLine)
                        .importTags(importer.importTags)
                        .results(results)
                        .selected(oldImporterProcessStatus.selected)
                        .started(!lastLine)
                        .aborted(oldImporterProcessStatus.isAborted)
                        .filename(oldImporterProcessStatus.filename)
                        .linesSent(lastLinesSent)
                        .build()
                update(cacheId, newImporterProcessStatus)
                logger.debug(httpSession.id + " [WS performImportFile] " + cacheId + "=" + newImporterProcessStatus.toString())
                if (lastTimestamp === now || newImporterProcessStatus.isAborted || newImporterProcessStatus.isCompleted) {
                    // Throttle the number of messages made available to the websocket so that it doesn't overflow and send the new results only
                    messagingTemplate.convertAndSend(importer.publisherUrl, newImporterProcessStatus.withoutResultNodes().withResultSubList(oldImporterProcessStatus.linesSent, results.size))
                }
                if (newImporterProcessStatus.isAborted) {
                    TransactionAspectSupport.currentTransactionStatus().setRollbackOnly()
                }
            }
        })
        pipelineFactory.runInTransaction(pipeline)
    }

    companion object {
        private const val INTER_MESSAGE_PERIOD_MILLIS = 1000L
    }
}