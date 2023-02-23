package max.demo.marketanalysis.analysis;

import com.oanda.v20.pricing.ClientPrice;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import max.demo.marketanalysis.infra.oanda.v20.OandaCache;
import org.springframework.stereotype.Service;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Service
@RequiredArgsConstructor
@Slf4j
public class AnalysisService {

  private final OandaCache oandaCache;

  private final Map<String, Integer> dataReceivedFromSink = new ConcurrentHashMap<>();

  public void subscribeToPrices2() {
    oandaCache
        .getSubjectIds()
        .subscribe(this::subscribeToNewSubject);
  }

  private void subscribeToNewSubject(String subject) {
    oandaCache
        .getPriceFor(subject)
        .subscribe(this::subscribeToNewPriceForSubject);
  }

  private void subscribeToNewPriceForSubject(ClientPrice mostRecentPrice) {
    dataReceivedFromSink
        .compute(mostRecentPrice.getInstrument().toString(),
            (String priceTicker, Integer count) -> {
              if (count == null) {
                count = 0;
              }
              log.debug("{} #{} received from sink", priceTicker, count + 1);
              return count + 1;
            });
  }

  /*
  Note: Use historical data of
    - Forex
    - Indexes
    - Commodities
    - Metals
    To calculate the correlation in price of each one.
    Use various timeframes: Day, 4 hours, 2 hours, 1 hours, 45min, 30min, 15min, 5min, 1min

    Detect which ones have an average correlation higher than 0.7 over a certain number of time periods.
    Display the correlation in a 2D graph.
   */

}
