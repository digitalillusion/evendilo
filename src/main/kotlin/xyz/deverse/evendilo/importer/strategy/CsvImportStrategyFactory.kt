package xyz.deverse.evendilo.importer.strategy

import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.core.convert.ConversionService
import org.springframework.stereotype.Service
import org.springframework.web.multipart.MultipartFile
import xyz.deverse.evendilo.importer.standard.amazon.StandardAmazonProductCsvLine
import xyz.deverse.evendilo.importer.standard.amazon.mappers.StandardAmazonProductMapper
import xyz.deverse.evendilo.importer.standard.woocommerce.StandardWooCommerceProductCsvLine
import xyz.deverse.evendilo.importer.standard.woocommerce.mappers.StandardWooCommerceProductMapper
import xyz.deverse.importer.ImportLine
import xyz.deverse.importer.ImportMapper.MappedLine
import xyz.deverse.importer.ImportStrategy
import xyz.deverse.importer.ImportStrategyFactory
import xyz.deverse.importer.Importer
import xyz.deverse.importer.csv.CsvImportStrategyBuilder
import javax.annotation.PostConstruct

@Service
class CsvImportStrategyFactory(
        @Qualifier("defaultConversionService") var conversionService: ConversionService,
        var standardAmazonProductMapper: StandardAmazonProductMapper,
        var standardWooCommerceProductMapper: StandardWooCommerceProductMapper) : ImportStrategyFactory {
    var strategies: MutableList<CsvImportStrategyBuilder<*, *>.CsvImportStrategy> = mutableListOf()

    var csvFile: MultipartFile? = null

    override fun createImportStrategy(importer: Importer<*, out ImportLine>): ImportStrategy<*, *> {
        for (strategy in strategies) {
            val matchNodeType = importer.nodeType == strategy.getNodeType()
            val matchLineType = MappedLine::class.java.isAssignableFrom(importer.lineType) || importer.lineType == strategy.getLineType()
            if (matchNodeType && matchLineType) {
                strategy.reinitialize(csvFile)
                return strategy
            }
        }
        throw IllegalArgumentException("No CsvImportStrategy is defined for importer " + importer.javaClass.canonicalName)
    }

    @PostConstruct
    fun setup() {
        strategies.add(object : CsvImportStrategyBuilder<xyz.deverse.evendilo.model.woocommerce.Product, StandardWooCommerceProductCsvLine>() {}
                .withConversionService(conversionService)
                .withRowMapper(standardWooCommerceProductMapper)
                .withPostProcessCondition(ImportStrategy.PostProcessCondition.ON_ALL_LINES)
                .build())
        strategies.add(object : CsvImportStrategyBuilder<xyz.deverse.evendilo.model.amazon.Product, StandardAmazonProductCsvLine>() {}
                .withConversionService(conversionService)
                .withRowMapper(standardAmazonProductMapper)
                .withPostProcessCondition(ImportStrategy.PostProcessCondition.ON_ALL_LINES)
                .build())
    }
}