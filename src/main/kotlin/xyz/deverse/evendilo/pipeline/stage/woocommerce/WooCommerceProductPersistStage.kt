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
        target.attributes.replaceAll { api.createAttribute(it) }
        target.variations.replaceAll { api.createProductVariation(target, it) }
        return target
    }

    private fun update(target: Product): Product {
        target.variations.replaceAll { variation ->
            if (variation.id == null) {
                variation.attributes.replaceAll { api.createAttribute(it) }
                api.createProductVariation(target, variation)
            }
            variation
        }
        return target
    }

}