package max.demo.marketanalysis.config;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import max.demo.marketanalysis.analysis.AnalysisService;
import max.demo.marketanalysis.infra.oanda.v20.PricePollingService;
import max.demo.marketanalysis.infra.oanda.v20.candles.CandlestickService;
import max.demo.marketanalysis.infra.oanda.v20.properties.V20Properties;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

import java.util.List;

import static com.oanda.v20.instrument.CandlestickGranularity.M1;
import static com.oanda.v20.instrument.CandlestickGranularity.M15;
import static java.util.concurrent.CompletableFuture.runAsync;
import static max.demo.marketanalysis.infra.oanda.v20.model.EInstrument.INSTRUMENT_LIST;

@Component
@RequiredArgsConstructor
@Slf4j
public class OnAppReadyManager {

  private final V20Properties v20Properties;
  private final PricePollingService pricePollingService;
  private final AnalysisService analysisService;
  private final CandlestickService candlestickService;

  @EventListener
  public void onAppReady(ApplicationReadyEvent ignored) {
    var message = "OnAppReadyManager";
    log.info("[LAUNCHING] {}", message);

    runAsync(this::candles);
    runAsync(this::pricePolling);

    log.info("[DONE] {}", message);
  }

  private void candles() {
    if (v20Properties.candlestick().enabled()) {
      var granularityList = List.of(M15, M1);
      var instrumentList = INSTRUMENT_LIST;

      candlestickService.logLastCandleTimesBreakdown(instrumentList, granularityList);
      candlestickService.getCandlesForMany(instrumentList, granularityList);
    }
  }

  private void pricePolling() {
    if (v20Properties.pricePolling().enabled()) {
      pricePollingService.pollPrices();
      analysisService.subscribeToPrices();
    }
  }

}
