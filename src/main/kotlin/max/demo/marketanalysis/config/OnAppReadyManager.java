package max.demo.marketanalysis.config;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import max.demo.marketanalysis.analysis.AnalysisService;
import max.demo.marketanalysis.infra.oanda.v20.PricePollingService;
import max.demo.marketanalysis.infra.oanda.v20.properties.V20Properties;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class OnAppReadyManager {

  private final V20Properties v20Properties;
  private final PricePollingService pricePollingService;
  private final AnalysisService analysisService;

  @EventListener
  public void onAppReady(ApplicationReadyEvent ignored) {
    var message = "OnAppReadyManager";
    log.info("[LAUNCHING] {}", message);

    if (v20Properties.pricePolling().enabled()) {
      pricePollingService.pollPrices();
      analysisService.subscribeToPrices();
    }

    log.info("[DONE] {}", message);
  }

}
