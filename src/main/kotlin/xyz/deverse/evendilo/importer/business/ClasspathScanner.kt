package xyz.deverse.evendilo.importer.business

import org.slf4j.LoggerFactory
import org.springframework.beans.factory.config.BeanDefinition
import org.springframework.context.annotation.ClassPathScanningCandidateComponentProvider
import org.springframework.core.type.classreading.MetadataReader
import org.springframework.core.type.classreading.MetadataReaderFactory
import org.springframework.core.type.filter.TypeFilter

class ClasspathScanner(private val basePackage: String) {
    fun findByAnnotation(annotationClass: Class<*>): Array<Class<*>> {
        val classpathScanningProvider = ClassPathScanningCandidateComponentProvider(false)
        classpathScanningProvider.addIncludeFilter(makeAnnotationFilter(annotationClass))
        return findClasses(classpathScanningProvider)
    }

    fun findBySimpleName(simpleName: String): Array<Class<*>> {
        val classpathScanningProvider = ClassPathScanningCandidateComponentProvider(false)
        classpathScanningProvider.addIncludeFilter(makeSimpleNameFilter(simpleName))
        return findClasses(classpathScanningProvider)
    }

    private fun findClasses(classpathScanningProvider: ClassPathScanningCandidateComponentProvider): Array<Class<*>> {
        val definitions = classpathScanningProvider.findCandidateComponents(basePackage)
        return definitions.stream().map { bd: BeanDefinition ->
            try {
                return@map Class.forName(bd.beanClassName)
            } catch (e: ClassNotFoundException) {
                logger.error("Unable to find class " + bd.beanClassName + " under package " + basePackage + "or its subpackages", e)
            }
            null
        }.toArray<Class<*>> { length -> arrayOfNulls(length)}
    }

    private fun makeAnnotationFilter(annotationClass: Class<*>): TypeFilter {
        return TypeFilter { metadataReader: MetadataReader, _: MetadataReaderFactory? -> metadataReader.annotationMetadata.hasAnnotation(annotationClass.canonicalName) }
    }

    private fun makeSimpleNameFilter(simpleName: String): TypeFilter {
        return TypeFilter { metadataReader: MetadataReader, _: MetadataReaderFactory? -> metadataReader.annotationMetadata.className.endsWith(".$simpleName") }
    }

    companion object {
        private val logger = LoggerFactory.getLogger(ClasspathScanner::class.java)
    }

}