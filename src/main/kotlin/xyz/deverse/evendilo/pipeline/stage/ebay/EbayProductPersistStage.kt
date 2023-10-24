package xyz.deverse.evendilo.pipeline.stage.ebay

import org.springframework.stereotype.Component
import xyz.deverse.evendilo.api.ebay.EbayApi
import xyz.deverse.evendilo.model.ProductType
import xyz.deverse.evendilo.model.ebay.Product
import xyz.deverse.evendilo.pipeline.stage.PersistStage

@Component
class EbayProductPersistStage(var api: EbayApi) : PersistStage<Product>() {

    init {
        addAction(PersistStageAction(PersistActionType.SAVE, updater = { target, _ -> save(target) }))
        addAction(PersistStageAction(PersistActionType.UPDATE, updater = { target, _ -> update(target) }))
    }

    private fun save(target: Product): Product {
        api.ensureInventoryLocation()

        when (target.type) {
            ProductType.Simple -> {
                api.createProduct(target)
                api.createOffer(target)
            }
            ProductType.Grouped,
            ProductType.Variation,
            ProductType.External,
            ProductType.Variable -> {

            }
        }

        return target
    }

    private fun update(target: Product): Product {
        return target
    }

}