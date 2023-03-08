package max.demo.marketanalysis.infra.oanda.v20.config;

import com.oanda.v20.instrument.Candlestick;
import lombok.RequiredArgsConstructor;
import max.demo.marketanalysis.infra.oanda.v20.model.EInstrument;
import max.demo.marketanalysis.infra.oanda.v20.model.GetCandlesResponse;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Flux;

import java.util.Map;

@RequiredArgsConstructor
public class CandleRestClient {

  private static final String CANDLES_ENDPOINT = "/v3/instruments/%s/candles";

  private final WebClient webClient;

  /**
   * Find candles for an instrument.
   * <br /> <br />
   * I get back the following data:
   * <ul>
   *   <li> timestamp UTC </li>
   *   <li> ohlc </li>
   *   <li> volume </li>
   *   <li> complete (boolean; true for all candles except the last one which isn't complete) </li>
   * </ul>
   */
  public Flux<Candlestick> getCandles(EInstrument instrument, Map<String, Object> queryParams) {
    var candlesEndpoint = CANDLES_ENDPOINT // example currency pair format: 'USD_CAD'
        .formatted(instrument.toString());

    return webClient
        .get()
        .uri(builder -> {
          for (var qp : queryParams.entrySet()) {
            builder.queryParam(qp.getKey(), qp.getValue());
          }
          return builder
              .path(candlesEndpoint)
              .build();
        })
        .retrieve()
        .bodyToMono(GetCandlesResponse.class)
        .flatMapIterable(GetCandlesResponse::getCandles);
  }
}