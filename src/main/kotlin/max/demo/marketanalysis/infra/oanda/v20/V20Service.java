package max.demo.marketanalysis.infra.oanda.v20;

import com.oanda.v20.Context;
import com.oanda.v20.ContextBuilder;
import com.oanda.v20.account.AccountInstrumentsRequest;
import com.oanda.v20.pricing.PricingGetRequest;
import com.oanda.v20.primitives.DateTime;
import com.oanda.v20.primitives.Instrument;
import com.oanda.v20.primitives.StringPrimitive;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.concurrent.ScheduledExecutorService;
import java.util.stream.Collectors;

import static java.util.concurrent.Executors.newSingleThreadScheduledExecutor;
import static java.util.concurrent.TimeUnit.MILLISECONDS;
import static max.demo.marketanalysis.infra.oanda.v20.OandaAssetsUtil.INSTRUMENT_NAMES;

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

    var request = new PricingGetRequest(v20Properties.accountId(), INSTRUMENT_NAMES);

    poller.scheduleAtFixedRate(() -> poll(ctx, request), 0L, 1000L, MILLISECONDS);
  }

  private void poll(Context ctx, PricingGetRequest request) {
    if (lastDataPointInTime != null) {
      request.setSince(lastDataPointInTime);
    }
    try {
      var resp = ctx.pricing.get(request);
      resp.getPrices().forEach(oandaCache::emitNewPrice2);

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

  public void getTradeableInstrumentsForAccount() {
    var ctx = new ContextBuilder(v20Properties.url())
        .setToken(v20Properties.token())
        .setApplication("InstrumentsGetter")
        .build();

    var accountInstrumentsRequest = new AccountInstrumentsRequest(v20Properties.accountId());

    try {
      var resp = ctx.account.instruments(accountInstrumentsRequest);

      var instruments = resp.getInstruments();

      log.info("{}",
          instruments
              .stream()
              .map(Instrument::getName)
              .map(StringPrimitive::toString)
              .sorted()
              .collect(Collectors.joining(",", "\"", "\"")));

    } catch (Exception e) {
      log.error("Error while trying to get tradeable instruments", e);
    }
  }


}
