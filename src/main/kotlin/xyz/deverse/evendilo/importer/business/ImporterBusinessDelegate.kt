package xyz.deverse.evendilo.importer.business

import org.springframework.stereotype.Service
import xyz.deverse.evendilo.model.Destination
import xyz.deverse.evendilo.model.Family
import xyz.deverse.evendilo.model.Model
import xyz.deverse.importer.ImportLine
import xyz.deverse.importer.Importer
import xyz.deverse.importer.generic.ImportTag
import java.util.*

@Service
class ImporterBusinessDelegate(var importers: List<Importer<out Model, out ImportLine>>, var entityFactories: List<EntityFactory>) {

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

    private fun resolveImporterClassname(classSimpleName: String, vararg importTags: ImportTag): String {
        val importerCanonicalName = StringBuilder("xyz.deverse.evendilo.importer.")
        Arrays.stream(importTags).forEach { tag: ImportTag? -> importerCanonicalName.append(tag!!.toString() + ".") }
        Arrays.stream(importTags).forEach { tag: ImportTag? -> importerCanonicalName.append(tag!!.name()) }
        importerCanonicalName.append(classSimpleName + "Importer")
        return importerCanonicalName.toString()
    }
}