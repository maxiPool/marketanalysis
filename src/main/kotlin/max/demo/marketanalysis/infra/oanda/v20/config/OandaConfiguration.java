package max.demo.marketanalysis.infra.oanda.v20.config;

import lombok.RequiredArgsConstructor;
import max.demo.marketanalysis.infra.oanda.v20.properties.V20Properties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.web.reactive.function.client.ExchangeStrategies;
import org.springframework.web.reactive.function.client.WebClient;

@Configuration
@RequiredArgsConstructor
public class OandaConfiguration {

  private final V20Properties v20Properties;

  /**
   * Get the Oanda stream url.
   *
   * @return Oanda stream url
   */
  public String getStreamUrl() {
    return v20Properties.isProduction()
        ? v20Properties.prodStreamUrl()
        : v20Properties.devStreamUrl();
  }

  /**
   * Get the Oanda rest url.
   *
   * @return Oanda rest url
   */
  public String getRestUrl() {
    return v20Properties.isProduction()
        ? v20Properties.prodRestUrl()
        : v20Properties.devRestUrl();
  }

//  @Bean
//  ObjectMapper oandaObjectMapper() {
//    ObjectMapper mapper = new ObjectMapper();
//    mapper.registerModule(new JavaTimeModule());
//    mapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
//    return mapper;
//  }

  @Bean
  WebClient oandaRestClient() {
    final var size = 16 * 1024 * 1024;
    final var strategies = ExchangeStrategies
        .builder()
        .codecs(codecs -> codecs.defaultCodecs().maxInMemorySize(size))
        .build();

    return WebClient
        .builder()
        .exchangeStrategies(strategies)
        .baseUrl(getRestUrl())
        .defaultHeader(HttpHeaders.AUTHORIZATION, "Bearer %s".formatted(v20Properties.token()))
        .defaultHeader("Accept-Datetime-Format", "RFC3339")
        .defaultHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
        .build();
  }

  @Bean
  CandleRestClient oandaCandleRestClient() {
    return new CandleRestClient(oandaRestClient());
  }

}
