package xyz.deverse.evendilo.pipeline.stage.woocommerce

import org.springframework.stereotype.Component
import xyz.deverse.evendilo.api.woocommerce.WooCommerceApi
import xyz.deverse.evendilo.model.woocommerce.Product
import xyz.deverse.evendilo.pipeline.stage.PersistStage

@Component
class WooCommerceProductPersistStage(var api: WooCommerceApi) : PersistStage<Product>() {

    init {
        addAction(PersistStageAction(PersistActionType.SAVE, updater = { target, _ -> save(target) }))
    }

    private fun save(target: Product): Product {
        return api.createProduct(target)
    }

}