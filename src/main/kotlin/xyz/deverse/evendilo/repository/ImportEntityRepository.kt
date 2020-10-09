package xyz.deverse.evendilo.repository

import org.springframework.data.repository.CrudRepository
import xyz.deverse.evendilo.entity.ImportEntity
import xyz.deverse.evendilo.entity.ImportEntityKey

interface ImportEntityRepository : CrudRepository<ImportEntity, ImportEntityKey> {
}