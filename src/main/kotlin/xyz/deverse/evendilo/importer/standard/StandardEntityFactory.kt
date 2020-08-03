package xyz.deverse.evendilo.importer.standard

import org.springframework.stereotype.Component
import xyz.deverse.evendilo.importer.business.EntityFactory
import xyz.deverse.evendilo.model.Family
import xyz.deverse.evendilo.model.Model

@Component
class StandardEntityFactory : EntityFactory(Family.Standard) {
    override fun <T : Model> populateOnCreate(node: T) {
        // if (node.getClass().equals(FiberEquipment.class)) {
        //	FiberEquipment fiberEquipment = (FiberEquipment) node;
        //	fiberEquipment.setStatus(Optional.ofNullable(fiberEquipment.getStatus()).orElse(NetworkNodeStatus.PENDING));
        // }
    }
}