package max.demo.marketanalysis.infra.oanda.v20;

import com.oanda.v20.pricing.ClientPrice;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Sinks;
import reactor.core.publisher.Sinks.Many;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Two options
 * 1) use a single Subject for all prices
 * - very simple, but must use replay all to get the price of all the subjects inside the Subject.
 * 2) use a Map of id to Subject (one entry per Subject)
 * - more complex, but more performant to get the latest price for each Subject.
 * - requires a Subject of ids that can replay all the ids
 * - can provide a Subject for each price on which it's now possible to only play latest.
 */
@Component
@Slf4j
public class OandaCache {

  // Option 1
  private final Many<ClientPrice> clientPriceBehaviorSubject = Sinks
      .many()
      .replay()
      .all();

  public void emitNewPrice(ClientPrice clientPrice) {
    clientPriceBehaviorSubject.tryEmitNext(clientPrice);
  }

  public Flux<ClientPrice> getPrices() {
    return clientPriceBehaviorSubject.asFlux();
  }

  // Option 2:
  private final Many<String> idsSubject = Sinks
      .many()
      .replay()
      .all();

  public Flux<String> getSubjectIds() {
    return idsSubject.asFlux();
  }

  private final Map<String, Many<ClientPrice>> subjectsMap = new ConcurrentHashMap<>();

  public void emitNewPrice2(ClientPrice clientPrice) {
    var isNew = new AtomicBoolean(false);
    subjectsMap
        .compute(clientPrice.getInstrument().toString(),
            (String k, Many<ClientPrice> clientPrice$) -> {
              if (clientPrice$ == null) {
                isNew.set(true);
                clientPrice$ = Sinks
                    .many()
                    .replay()
                    .latest();
              }
              clientPrice$.tryEmitNext(clientPrice);
              return clientPrice$;
            });

    if (isNew.get()) {
      idsSubject.tryEmitNext(clientPrice.getInstrument().toString());
    }
  }

  public Flux<ClientPrice> getPriceFor(String instrumentName) {
    var clientPrice$ = subjectsMap.get(instrumentName);
    if (clientPrice$ == null) {
      throw new RuntimeException("Not found: instrument name '%s'".formatted(instrumentName));
    }
    return clientPrice$.asFlux();
  }

}
