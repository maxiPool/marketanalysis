package max.demo.marketanalysis;

import lombok.extern.slf4j.Slf4j;
import max.demo.marketanalysis.infra.oanda.v20.candles.CandlestickService;
import max.demo.marketanalysis.infra.oanda.v20.model.EInstrument;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import java.time.Instant;
import java.util.Scanner;

import static com.oanda.v20.instrument.CandlestickGranularity.M15;
import static java.time.temporal.ChronoUnit.MINUTES;
import static java.util.Arrays.stream;
import static java.util.stream.Collectors.toMap;
import static max.demo.marketanalysis.infra.oanda.v20.candles.CandlestickService.TEMPLATE_CANDLE_FILE_PATH;
import static max.demo.marketanalysis.infra.oanda.v20.candles.CandlestickService.getLastCandle;
import static max.demo.marketanalysis.infra.oanda.v20.model.EInstrument.USD_CAD;


@Disabled
@Slf4j
@SpringBootTest
@ActiveProfiles({"local"})
public class DemoCandlesticks {

  private static final String USD_CAD_FILE_PATH = TEMPLATE_CANDLE_FILE_PATH.formatted("usd_cad");

  @Autowired
  private CandlestickService candlestickService;

  @Test
  void should_getCandlesFor() {
    var instrumentToPathMap = stream(EInstrument.values())
        .collect(toMap(i -> i, i -> TEMPLATE_CANDLE_FILE_PATH.formatted(i.toString().toLowerCase())));

    var next = instrumentToPathMap.entrySet().iterator().next();

    candlestickService.getCandlesFor(next.getKey(), M15, next.getValue());

    var s = new Scanner(System.in);
    s.nextLine();
  }

  @Test
  void should_getCandlesticksFromOandaAPIWithStartTime() {
    var lastCandle = getLastCandle(USD_CAD_FILE_PATH);
    var plusOneMinute = Instant.parse(lastCandle.getTime().toString()).plus(15, MINUTES);

    candlestickService.getCandlesFromTime(USD_CAD, USD_CAD_FILE_PATH, M15, plusOneMinute);

    var s = new Scanner(System.in);
    s.nextLine();
  }

  @Test
  void should_getCandlesticksFromOandaAPIWithCandleCount() {
    candlestickService.getCandlestickWithCount(USD_CAD, USD_CAD_FILE_PATH, M15, 5);

    var s = new Scanner(System.in);
    s.nextLine();
  }

  @Test
  void should_getDateTimeFromLastSavedCandle() {
    var lastCandle = getLastCandle(USD_CAD_FILE_PATH);
    log.info("last candle: {}", lastCandle);
  }

}
