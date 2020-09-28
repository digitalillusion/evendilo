package xyz.deverse.evendilo.pipeline.stage.woocommerce

import org.springframework.stereotype.Component
import xyz.deverse.evendilo.api.woocommerce.WoocommerceApi
import xyz.deverse.evendilo.functions.replaceList
import xyz.deverse.evendilo.model.woocommerce.Product
import xyz.deverse.evendilo.pipeline.stage.PersistStage

@Component
class WoocommerceProductPersistStage(var api: WoocommerceApi) : PersistStage<Product>() {

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
        replaceList(target.variations) { variation ->
            if (variation.id == null) {
                replaceList(target.attributes) { attribute ->
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