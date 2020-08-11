package xyz.deverse.evendilo.importer.standard

import org.springframework.stereotype.Component
import xyz.deverse.evendilo.importer.business.EntityFactory
import xyz.deverse.evendilo.model.Destination
import xyz.deverse.evendilo.model.Family
import xyz.deverse.evendilo.model.Model

@Component
class WooCommerceEntityFactory : EntityFactory(Destination.WooCommerce) {
    override fun <T : Model> populateOnCreate(node: T) {
        // NOOP
    }

}