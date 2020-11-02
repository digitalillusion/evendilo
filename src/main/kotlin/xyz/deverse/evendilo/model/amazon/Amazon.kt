package xyz.deverse.evendilo.model.amazon

import com.fasterxml.jackson.annotation.JsonIgnore
import xyz.deverse.evendilo.model.Destination
import xyz.deverse.evendilo.model.Family
import xyz.deverse.evendilo.model.Model



interface AmazonModel : Model {
    @get:JsonIgnore
    override val family
        get() = Family.Standard

    @get:JsonIgnore
    override val destination
        get() = Destination.Amazon
}

data class Product(
        override var id: Long?
) : AmazonModel