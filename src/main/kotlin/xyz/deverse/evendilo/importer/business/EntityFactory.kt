package xyz.deverse.evendilo.importer.business

import org.mapstruct.TargetType
import org.springframework.util.StringUtils
import xyz.deverse.evendilo.logger
import xyz.deverse.evendilo.model.Destination
import xyz.deverse.evendilo.model.Family
import xyz.deverse.evendilo.model.Model
import xyz.deverse.importer.generic.ImportTag
import java.util.*
import kotlin.reflect.KClass
import kotlin.reflect.full.createInstance

/**
 * Abstract class used to create domain entities by resolving their type according to their family
 */
abstract class EntityFactory protected constructor(var destination: Destination) {
    var scanner: ClasspathScanner = ClasspathScanner("xyz.deverse.evendilo.model")
    val logger = logger<EntityFactory>()

    /**
     * Instance a new entity of the given class according to the family of the EntityFactory.
     * For instance, if `Destination.WooCommerce` and `entityClass` is `PRoduct`, the method invocation will return a new `xyz.deverse.evendilo.model.woocommerce.Product` instance
     * @param entityClass The desired domain entity class
     * @return A new instance of the entity according to the destination
     */
    fun <T : Model> createNode(@TargetType entityClass: Class<T>): T? {
        val entityName = "xyz.deverse.evendilo.model."  + destination + "." + entityClass.simpleName
        return try {
            if (entityClass.canonicalName != entityName) {
                throw IllegalArgumentException("The entity class should be in the ${destination} package")
            }
            val model = Class.forName(entityName).kotlin.createInstance() as T;
            populateOnCreate(model)
            model
        } catch (e: Exception) {
            logger.warn("Cannot instantiate entity " + entityName + " in substitution of base class " + entityClass.canonicalName + " " + e.javaClass + " " + e.message)
            null
        }
    }

    protected abstract fun <T : Model> populateOnCreate(node: T)

    fun isImportTagSupported(importTag: ImportTag): Boolean {
        return Destination.WooCommerce == importTag
    }

}