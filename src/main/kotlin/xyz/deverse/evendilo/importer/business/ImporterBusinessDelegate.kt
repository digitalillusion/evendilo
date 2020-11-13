package xyz.deverse.evendilo.importer.business

import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.security.oauth2.client.authentication.OAuth2AuthenticationToken
import org.springframework.stereotype.Service
import xyz.deverse.evendilo.entity.ImportEntity
import xyz.deverse.evendilo.entity.ImportEntityKey
import xyz.deverse.evendilo.functions.getAuthentication
import xyz.deverse.evendilo.model.Destination
import xyz.deverse.evendilo.model.Family
import xyz.deverse.evendilo.model.Model
import xyz.deverse.evendilo.repository.ImportEntityRepository
import xyz.deverse.importer.ImportLine
import xyz.deverse.importer.Importer
import xyz.deverse.importer.ReadFilter
import xyz.deverse.importer.generic.ImportTag
import java.lang.IllegalStateException
import java.time.LocalDateTime
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter
import java.util.*
import kotlin.math.max

@Service
class ImporterBusinessDelegate(var importers: List<Importer<out Model, out ImportLine>>, var entityFactories: List<EntityFactory>, var importEntityRepository: ImportEntityRepository) {

    fun <T : Model, S : ImportLine> getImporterFor(classSimpleName: String): Importer<T, S> {
        var cps = ClasspathScanner("xyz.deverse.evendilo.model")
        var matchingClasses = cps.findBySimpleName(classSimpleName)
        return try {
            val importerCanonicalName: String
            if (matchingClasses.isNotEmpty()) {
                val instance = matchingClasses[0].newInstance() as Model
                importerCanonicalName = resolveImporterClassname(classSimpleName, instance.family, instance.destination)
            } else {
                cps = ClasspathScanner("xyz.deverse.evendilo.importer")
                matchingClasses = cps.findBySimpleName(classSimpleName + "Importer")
                importerCanonicalName = matchingClasses[0].canonicalName
            }
            importers.stream().filter { importer: Importer<out Model, out ImportLine> ->
                val importerClassname = importer.javaClass.canonicalName
                importerClassname.startsWith(importerCanonicalName)
            }.findAny().get() as Importer<T, S>
        } catch (e: ArrayIndexOutOfBoundsException) {
            throw IllegalArgumentException("Cannot find an importer for class $classSimpleName", e)
        } catch (e: ReflectiveOperationException) {
            throw IllegalArgumentException("Cannot find an importer for class $classSimpleName", e)
        } catch (e: NoSuchElementException) {
            throw IllegalArgumentException("Cannot find an importer for class $classSimpleName", e)
        }
    }

    fun <T : Model, S : ImportLine> getImporterFor(classSimpleName: String, family: String, destination: String): Importer<T, S> {
        var cps = ClasspathScanner("xyz.deverse.evendilo.model")
        var matchingClasses = cps.findBySimpleName(classSimpleName)
        return try {
            val importerCanonicalName: String
            if (matchingClasses.isNotEmpty()) {
                importerCanonicalName = resolveImporterClassname(classSimpleName, Family.valueOf(family), Destination.valueOf(destination))
            } else {
                cps = ClasspathScanner("xyz.deverse.evendilo.importer")
                matchingClasses = cps.findBySimpleName(classSimpleName + "Importer")
                importerCanonicalName = matchingClasses[0].canonicalName
            }
            importers.stream().filter { importer: Importer<out Model, out ImportLine> ->
                val importerClassname = importer.javaClass.canonicalName
                importerClassname.startsWith(importerCanonicalName)
            }.findAny().get() as Importer<T, S>
        } catch (e: ArrayIndexOutOfBoundsException) {
            throw IllegalArgumentException("Cannot find an importer for class $classSimpleName", e)
        } catch (e: NoSuchElementException) {
            throw IllegalArgumentException("Cannot find an importer for class $classSimpleName", e)
        }
    }

    fun getEntityFactoryFor(importTag: ImportTag): EntityFactory {
        return try {
            entityFactories.stream().filter { factory: EntityFactory -> factory.isImportTagSupported(importTag) }.findAny().get()
        } catch (e: ArrayIndexOutOfBoundsException) {
            throw IllegalArgumentException("Cannot find an importer for class $importTag", e)
        } catch (e: NoSuchElementException) {
            throw IllegalArgumentException("Cannot find an importer for class $importTag", e)
        }
    }

    fun modifyImportFilter(filter: ReadFilter, family: Family, destination: Destination): ReadFilter {
        val firstSheet = filter.groups[0];
        filter.groups.removeIf { group -> group != firstSheet }

        val token = getAuthentication()
        val id = ImportEntityKey.of(token.authorizedClientRegistrationId, family, destination, filter.filename)
        val existing = importEntityRepository.findById(id)
        existing.ifPresent { e ->
                    filter.rawData[firstSheet]!!.values.removeIf {
                        try {
                            val firstCellValue = it.iterator().next().trim()
                            val cellDateTime = LocalDateTime.parse(firstCellValue, DateTimeFormatter.ofPattern("yyyy/MM/dd HH:mm:ss"))
                            val cellTimestamp = cellDateTime.toEpochSecond(ZoneOffset.UTC)
                            e.timestamp > cellTimestamp && e.version >= filter.version
                        } catch (e: Exception) {
                            false
                        }
                    }
                }
        val timestamp = LocalDateTime.now().toEpochSecond(ZoneOffset.UTC)
        val importEntity = existing.orElse(ImportEntity(id, filter.version, timestamp))
        importEntity.timestamp = timestamp
        importEntityRepository.save(importEntity)
        return filter
    }

    private fun resolveImporterClassname(classSimpleName: String, vararg importTags: ImportTag): String {
        val importerCanonicalName = StringBuilder("xyz.deverse.evendilo.importer.")
        Arrays.stream(importTags).forEach { tag: ImportTag? -> importerCanonicalName.append(tag!!.toString() + ".") }
        Arrays.stream(importTags).forEach { tag: ImportTag? -> importerCanonicalName.append(tag!!.name()) }
        importerCanonicalName.append(classSimpleName + "Importer")
        return importerCanonicalName.toString()
    }
}