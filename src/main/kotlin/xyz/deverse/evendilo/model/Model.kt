package xyz.deverse.evendilo.model

import com.fasterxml.jackson.annotation.JsonIgnore

interface Model {

    val id: Long?

    @get:JsonIgnore
    val family: Family

    @get:JsonIgnore
    val destination: Destination
}