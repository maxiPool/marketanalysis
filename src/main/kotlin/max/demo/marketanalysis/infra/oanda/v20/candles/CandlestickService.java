package max.demo.marketanalysis.infra.oanda.v20.candles;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import max.demo.marketanalysis.infra.oanda.v20.candles.csvutil.CsvCandle;
import max.demo.marketanalysis.infra.oanda.v20.config.CandleRestClient;
import max.demo.marketanalysis.infra.oanda.v20.model.EInstrument;
import max.demo.marketanalysis.infra.oanda.v20.model.Rfc3339;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;

import java.nio.file.Files;
import java.nio.file.Paths;
import java.time.Instant;
import java.util.Map;

import static com.oanda.v20.instrument.CandlestickGranularity.M15;
import static java.time.temporal.ChronoUnit.MINUTES;
import static max.demo.marketanalysis.infra.oanda.v20.candles.ReadCandlesCsvFileUtil.getFirstAndLastLineFromFile;
import static max.demo.marketanalysis.infra.oanda.v20.candles.csvutil.CsvUtil.csvStringToCsvCandlePojo;

@Slf4j
@Service
@RequiredArgsConstructor
@ConditionalOnProperty(prefix = "infra.oanda.v20.candlestick", name = "enabled", havingValue = "true")
public class CandlestickService {

  private static final String FROM = "from";
  private static final String TO = "to";
  private static final String COUNT = "count";
  private static final int MAX_CANDLE_COUNT_OANDA_API = 5_000;

  private final CandleRestClient candleRestClient;
  private final CandlestickMapper candlestickMapper;

  public void getCandlesFor(EInstrument instrument, String filePath) {
    if (Files.notExists(Paths.get(filePath))) {
      getCandlestickWithCount(instrument, filePath, MAX_CANDLE_COUNT_OANDA_API);
    } else {
      var lastCandle = getLastCandle(filePath);
      var lastDatePlusFifteenMinutes = Instant.parse(lastCandle.getTime().toString()).plus(15, MINUTES);
      getCandlestickWithLastDate(instrument, filePath, lastDatePlusFifteenMinutes);
    }
  }

  public void getCandlestickWithLastDate(EInstrument instrument, String filePath, Instant fromTime) {
    candleRestClient
        .findBetween(instrument,
            M15,
            Map.of(FROM, Rfc3339.YMDHMSN_FORMATTER.format(fromTime),
                TO, Rfc3339.YMDHMSN_FORMATTER.format(Instant.now())))
        .subscribe(new CandlestickWriterSubscriber(filePath, candlestickMapper));
  }

  /**
   * Maximum candle count is 5000. Any granularity seems fine.
   */
  public void getCandlestickWithCount(EInstrument instrument, String filePath, int count) {
    candleRestClient
        .findBetween(instrument, M15, Map.of(COUNT, count))
        .subscribe(new CandlestickWriterSubscriber(filePath, candlestickMapper));
  }

  public static CsvCandle getLastCandle(String filePath) {
    var firstAndLastLineFromFile = getFirstAndLastLineFromFile(filePath);
    var concat = "%s\n%s".formatted(firstAndLastLineFromFile[0], firstAndLastLineFromFile[1]);
    return csvStringToCsvCandlePojo(concat);
  }

}
