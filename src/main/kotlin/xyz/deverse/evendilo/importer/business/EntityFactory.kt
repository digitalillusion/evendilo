package xyz.deverse.evendilo.importer.business

import org.mapstruct.TargetType
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.security.oauth2.client.authentication.OAuth2AuthenticationToken
import xyz.deverse.evendilo.config.properties.DestinationConfigurationProperties
import xyz.deverse.evendilo.importer.standard.EvendiloCsvLine
import xyz.deverse.evendilo.logger
import xyz.deverse.evendilo.model.Destination
import xyz.deverse.evendilo.model.Model
import xyz.deverse.importer.generic.ImportTag
import kotlin.reflect.full.createInstance

/**
 * Abstract class used to create domain entities by resolving their type according to their family
 */
abstract class EntityFactory protected constructor(var destination: Destination) {
    var scanner: ClasspathScanner = ClasspathScanner("xyz.deverse.evendilo.model")
    val logger = logger<EntityFactory>()

    /**
     * Instance a new entity of the given class according to the family of the EntityFactory.
     * For instance, if `Destination.Woocommerce` and `entityClass` is `Product`, the method invocation will return a new `xyz.deverse.evendilo.model.woocommerce.xyz.deverse.evendilo.model.amazon.Product` instance
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
        return destination.name() == importTag.name()
    }

    fun <T: Model> getAttributesMapping(destinationsConfigProperties: List<DestinationConfigurationProperties>, csvLine: EvendiloCsvLine<T>): Pair<MutableList<String>, Array<String>> {
        val attributes = mutableListOf<String>()
        var token = SecurityContextHolder.getContext().authentication as OAuth2AuthenticationToken
        for (config in destinationsConfigProperties) {
            val importerConfig = config.importerConfig;
            if (token.authorizedClientRegistrationId == config.identifier) {
                importerConfig.attributes.split(",").toList().map { it.trim() }.forEach { attributes.add(it) }
                break
            }
        }
        val csvLineAttrs = arrayOf(csvLine.attr0, csvLine.attr1, csvLine.attr2, csvLine.attr3,
                csvLine.attr4, csvLine.attr5, csvLine.attr6, csvLine.attr7, csvLine.attr8, csvLine.attr9)
        return Pair(attributes, csvLineAttrs)
    }
}