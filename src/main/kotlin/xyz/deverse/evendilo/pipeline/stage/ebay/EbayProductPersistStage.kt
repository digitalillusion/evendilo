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
                api.createOrUpdateProduct(target)
                val offer = api.createOrUpdateOffer(target)
                api.publishOffer(offer);
            }
            ProductType.Variation -> {
                api.createOrUpdateProduct(target)
                api.createOrUpdateOffer(target)
            }
            ProductType.Variable -> {
            }
        }

        return target
    }

    private fun update(target: Product): Product {
        return target
    }

    override fun postProcess(targets: Collection<Product>) {
        targets.filter { it.type == ProductType.Variable }.forEach { variableProduct ->
            val skus = targets
                    .filter { it.type == ProductType.Variation && it.sku.startsWith(variableProduct.sku) }
                    .map { it.sku }
            val group = api.createGroup(variableProduct, skus)
            api.publishOfferForGroup(variableProduct, group)
        }
    }
}