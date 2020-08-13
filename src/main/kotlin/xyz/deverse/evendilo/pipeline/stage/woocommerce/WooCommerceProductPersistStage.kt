package xyz.deverse.evendilo.pipeline.stage.woocommerce

import org.springframework.stereotype.Component
import xyz.deverse.evendilo.api.woocommerce.WooCommerceApi
import xyz.deverse.evendilo.model.woocommerce.Product
import xyz.deverse.evendilo.pipeline.stage.PersistStage

@Component
class WooCommerceProductPersistStage(var api: WooCommerceApi) : PersistStage<Product>() {

    init {
        addAction(PersistStageAction(PersistActionType.SAVE, updater = { target, _ -> save(target) }))
        addAction(PersistStageAction(PersistActionType.UPDATE, updater = { target, _ -> update(target) }))
    }

    private fun save(target: Product): Product {
        var created = api.createProduct(target);
        target.id = created.id
        target.variations.replaceAll { api.createProductVariation(target, it) }
        return target
    }

    private fun update(target: Product): Product {
        target.variations.replaceAll { variation ->
            if (variation.id == null) {
                target.attributes.replaceAll { attribute ->
                    val optionsToAdd = variation.attributes.find { it.name == attribute.name }?.options ?: mutableListOf()
                    api.updateAttribute(attribute, optionsToAdd)
                }
                variation.attributes.replaceAll { api.createAttribute(it) }
                api.createProductVariation(target, variation)
            }
            variation
        }
        api.updateProduct(target);
        return target
    }

}