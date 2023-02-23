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

@Component
@Slf4j
public class OandaCache {

  /**
   * Use a Subject (Sink) that contains all the 'Instruments' Subjects (Sinks).
   * <ul>
   *   <li> any new consumer of the OandaCache can easily subscribe to all the 'Instruments' </li>
   *   <li> performant to get the latest price for each Subject (Sink) </li>
   *   <li> requires a Subject (Sink) of ids that can replay all the ids </li>
   *   <li> can provide a Subject (Sink) for each price on which it's now possible to only play latest </li>
   * </ul>
   */
  private final Many<String> idsSubject = Sinks
      .many()
      .replay()
      .all();

  public Flux<String> getSubjectIds() {
    return idsSubject.asFlux();
  }

  private final Map<String, Many<ClientPrice>> subjectsMap = new ConcurrentHashMap<>();

  public void emitNewPrice(ClientPrice clientPrice) {
    var isNew = new AtomicBoolean(false);
    subjectsMap
        .compute(clientPrice.getInstrument().toString(),
            (String __, Many<ClientPrice> clientPrice$) -> {
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
