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

import static com.oanda.v20.instrument.CandlestickGranularity.M15;
import static max.demo.marketanalysis.infra.oanda.v20.model.EInstrument.INSTRUMENTS_LIST;

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

    if (v20Properties.pricePolling().enabled()) {
      pricePollingService.pollPrices();
      analysisService.subscribeToPrices();
    }

    if (v20Properties.candlestick().enabled()) {
      var granularityList = List.of(M15);
      candlestickService.getCandlesForMany(INSTRUMENTS_LIST, granularityList);
    }

    log.info("[DONE] {}", message);
  }

}
