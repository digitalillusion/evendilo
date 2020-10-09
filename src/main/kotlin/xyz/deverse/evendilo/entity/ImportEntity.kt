package xyz.deverse.evendilo.entity

import xyz.deverse.evendilo.model.Destination
import xyz.deverse.evendilo.model.Family
import java.io.Serializable
import javax.persistence.*

@Embeddable
class ImportEntityKey (
    private var clientId: String = "",
    private var family: String = "",
    private var destination: String = "",
    private var filename: String = ""
) : Serializable {

    companion object {
        fun of(clientId: String, family: Family, destination: Destination, filename: String) : ImportEntityKey {
            return ImportEntityKey(clientId, family.name(), destination.name(), filename)
        }
    }
}

@Entity
class ImportEntity (
    @EmbeddedId var id: ImportEntityKey = ImportEntityKey(),
    var timestamp: Long = 0
)