package max.demo.marketanalysis.infra.oanda.v20;

import com.oanda.v20.Context;
import com.oanda.v20.ContextBuilder;
import com.oanda.v20.pricing.PricingGetRequest;
import com.oanda.v20.primitives.DateTime;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.concurrent.ScheduledExecutorService;

import static java.util.concurrent.Executors.newSingleThreadScheduledExecutor;
import static java.util.concurrent.TimeUnit.MILLISECONDS;

@Service
@Slf4j
@RequiredArgsConstructor
public class V20Service {

  private final V20Properties v20Properties;
  private final OandaCache oandaCache;
  private final ScheduledExecutorService poller = newSingleThreadScheduledExecutor();

  private volatile DateTime lastDataPointInTime = null;

  public void pollPrices() {
    var ctx = new ContextBuilder(v20Properties.url())
        .setToken(v20Properties.token())
        .setApplication("PricePolling")
        .build();

    var instruments = List.of("EUR_USD", "USD_JPY", "GBP_USD", "USD_CHF", "USD_CAD", "AUD_USD");
    var request = new PricingGetRequest(v20Properties.accountId(), instruments);

    poller.scheduleAtFixedRate(() -> poll(ctx, request), 0L, 1000L, MILLISECONDS);
  }

  private void poll(Context ctx, PricingGetRequest request) {
    if (lastDataPointInTime != null) {
      request.setSince(lastDataPointInTime);
    }
    try {
      var resp = ctx.pricing.get(request);
      resp.getPrices().forEach(oandaCache::emitNewPrice);

      handleLastDataPointInTime(resp.getTime());
    } catch (Exception e) {
      log.error("Error while polling", e);
    }
  }

  private void handleLastDataPointInTime(DateTime responseTime) {
    if (lastDataPointInTime == null) {
      lastDataPointInTime = responseTime;
      log.info("LastDataPointInTime: {}", lastDataPointInTime);
      return;
    }

    var oldLastDataPointInTime = new DateTime(lastDataPointInTime);
    lastDataPointInTime = responseTime;
    if (!oldLastDataPointInTime.equals(lastDataPointInTime)) {
      log.info("LastDataPointInTime: {}", lastDataPointInTime);
    }
    // else: do not log!
  }

}
