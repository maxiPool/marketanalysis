package max.demo.marketanalysis.infra.oanda.v20;

import com.oanda.v20.pricing.ClientPrice;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Sinks;
import reactor.core.publisher.Sinks.Many;

@Component
@Slf4j
public class OandaCache {

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

}
