package max.demo.marketanalysis

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.context.properties.ConfigurationPropertiesScan
import org.springframework.boot.runApplication

@SpringBootApplication
@ConfigurationPropertiesScan
class MarketAnalysisApplication

fun main(args: Array<String>) {
  runApplication<MarketAnalysisApplication>(*args)
}
