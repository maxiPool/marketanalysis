package max.demo.marketanalysis.config;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import max.demo.marketanalysis.analysis.AnalysisService;
import max.demo.marketanalysis.infra.oanda.v20.V20Service;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class OnAppReadyManager {

  private final V20Service v20Service;
  private final AnalysisService analysisService;

  @EventListener
  public void onAppReady(ApplicationReadyEvent ignored) {
    var message = "OnAppReadyManager price polling and price subscription in arbitrage service";
    log.info("[LAUNCHING] {}", message);

    v20Service.pollPrices();
    // analysisService.subscribeToPrices();
    analysisService.subscribeToPrices2();

    log.info("[DONE] {}", message);
  }

}
