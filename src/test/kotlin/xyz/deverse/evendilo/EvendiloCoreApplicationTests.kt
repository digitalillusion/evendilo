package xyz.deverse.evendilo

import org.junit.jupiter.api.Test
import org.springframework.boot.test.context.SpringBootTest

@SpringBootTest(properties = ["variables.cors-allowed-origin=*"])
class EvendiloCoreApplicationTests {

	@Test
	fun contextLoads() {
	}

}
