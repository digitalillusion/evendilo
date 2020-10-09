package xyz.deverse.evendilo

import org.junit.jupiter.api.Test
import org.springframework.boot.autoconfigure.EnableAutoConfiguration
import org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration
import org.springframework.boot.autoconfigure.jdbc.DataSourceTransactionManagerAutoConfiguration
import org.springframework.boot.autoconfigure.orm.jpa.HibernateJpaAutoConfiguration
import org.springframework.boot.test.context.SpringBootTest

@SpringBootTest(properties = ["variables.cors-allowed-origin=http://localhost:8000"])
@EnableAutoConfiguration(exclude = [
	DataSourceAutoConfiguration::class,
	DataSourceTransactionManagerAutoConfiguration::class,
	HibernateJpaAutoConfiguration::class
])
class EvendiloCoreApplicationTests {

	@Test
	fun contextLoads() {
	}

}
