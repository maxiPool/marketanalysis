package max.demo.marketanalysis.infra.oanda.v20;

import com.oanda.v20.pricing.ClientPrice;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Flux;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

@Component
@Slf4j
public class OandaCache {

  private final ConcurrentMap<String, Flux<ClientPrice>> pricesFluxCache = new ConcurrentHashMap<>();

  public void put(String instrumentId, ClientPrice clientPrice) {
    pricesFluxCache.compute(instrumentId, (__, v) -> {
      // TODO: push new price to observable
      return null;
    });
  }

}
