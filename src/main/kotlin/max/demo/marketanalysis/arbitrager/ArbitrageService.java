package max.demo.marketanalysis.arbitrager;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import max.demo.marketanalysis.infra.oanda.v20.OandaCache;
import org.springframework.stereotype.Service;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Service
@RequiredArgsConstructor
@Slf4j
public class ArbitrageService {

  private final OandaCache oandaCache;

  private final Map<String, Integer> dataReceivedFromSink = new ConcurrentHashMap<>();

  public void subscribeToPrices() {
    oandaCache
        .getPrices()
        .subscribe(clientPrice -> dataReceivedFromSink
            .compute(clientPrice.getInstrument().toString(), (k, v) -> {
              if (v == null) {
                v = 0;
              }
              log.info("{} #{} received from sink (behaviorSubject)", k, v + 1);
              return v + 1;
            }));
  }

}
