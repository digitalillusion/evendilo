package xyz.deverse.evendilo.importer.rest.converters

import org.springframework.core.convert.converter.Converter
import org.springframework.stereotype.Component
import xyz.deverse.evendilo.model.Destination
import xyz.deverse.evendilo.model.Family

@Component
class InputTypeConverter : Converter<String, Family> {
    override fun convert(value: String): Family {
        return Family.valueOf(value)
    }
}

@Component
class DestinationConverter : Converter<String, Destination> {
    override fun convert(value: String): Destination {
        return Destination.valueOf(value)
    }
}