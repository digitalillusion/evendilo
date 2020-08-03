package xyz.deverse.evendilo.importer.business

import org.mapstruct.TargetType
import org.springframework.util.StringUtils
import xyz.deverse.evendilo.logger
import xyz.deverse.evendilo.model.Family
import xyz.deverse.evendilo.model.Model
import xyz.deverse.importer.generic.ImportTag
import java.util.*

/**
 * Abstract class used to create domain entities by resolving their type according to their family
 */
abstract class EntityFactory protected constructor(family: Family) {
    var scanner: ClasspathScanner = ClasspathScanner("xyz.deverse.evendilo.model")
    var family: Family = family
    val logger = logger<EntityFactory>()

    /**
     * Instance a new entity of the given class according to the family of the EntityFactory.
     * For instance, if `IFamily.FIBER` and `entityClass` is `ConnectionPoint`, the method invocation will return a new `FiberConnectionPoint` instance
     * @param entityClass The desired domain entity class
     * @return A new instance of the entity according to the factory family
     */
    fun <T : Model> createNode(@TargetType entityClass: Class<T>): T? {
        val entityName = familyPrefix + entityClass.simpleName
        classesBySimpleName.computeIfAbsent(entityName) { key: String? -> scanner.findBySimpleName(key!!) }
        val matching = classesBySimpleName[entityName]
        return try {
            var node: T = if (matching!!.isNotEmpty()) {
                matching[0].newInstance() as T
            } else {
                entityClass.newInstance()
            }
            populateOnCreate(node)
            node
        } catch (e: Exception) {
            logger.warn("Cannot instantiate entity " + entityName + " in substitution of base class " + entityClass.canonicalName + " " + e.javaClass + " " + e.message)
            null
        }
    }

    protected abstract fun <T : Model> populateOnCreate(node: T)

    fun isImportTagSupported(importTag: ImportTag): Boolean {
        return Family.Standard == importTag || family == importTag
    }

    private val familyPrefix: String
        get() = StringUtils.capitalize(family.toString())

    companion object {
        private val classesBySimpleName: MutableMap<String, Array<Class<*>>> = HashMap()
    }

}