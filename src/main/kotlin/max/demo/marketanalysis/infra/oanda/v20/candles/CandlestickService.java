package max.demo.marketanalysis.infra.oanda.v20.candles;

import com.oanda.v20.instrument.CandlestickGranularity;
import lombok.Builder;
import lombok.RequiredArgsConstructor;
import lombok.With;
import lombok.extern.slf4j.Slf4j;
import max.demo.marketanalysis.infra.oanda.v20.candles.csvutil.CsvCandle;
import max.demo.marketanalysis.infra.oanda.v20.config.CandleRestClient;
import max.demo.marketanalysis.infra.oanda.v20.model.EInstrument;
import max.demo.marketanalysis.infra.oanda.v20.model.Rfc3339;
import org.jetbrains.annotations.NotNull;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;

import java.nio.file.Files;
import java.nio.file.Paths;
import java.time.Instant;
import java.util.List;
import java.util.Map;

import static java.time.temporal.ChronoUnit.SECONDS;
import static java.util.concurrent.CompletableFuture.runAsync;
import static max.demo.marketanalysis.infra.oanda.v20.candles.ReadFileUtil.getFirstAndLastLineFromFile;
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
  public static final String TEMPLATE_CANDLE_FILE_PATH = "F:\\candles\\%s_candles_%s.csv";

  private final CandleRestClient candleRestClient;
  private final CandlestickMapper candlestickMapper;

  public void getCandlesForMany(List<EInstrument> instrumentList, List<CandlestickGranularity> granularities) {
    getInstrumentToGranularityToPath(instrumentList, granularities)
        .forEach(i -> runAsync(() -> getCandlesFor(i.instrument(), i.granularity(), i.outputPath())));
  }

  public void getCandlesFor(EInstrument instrument, CandlestickGranularity granularity, String filePath) {
    if (Files.notExists(Paths.get(filePath))) {
      getCandlestickWithCount(instrument, filePath, granularity, MAX_CANDLE_COUNT_OANDA_API);
    } else {
      var lastTimePlusGranularity = Instant.parse(getLastCandle(filePath).getTime().toString())
          .plus(granularityToSeconds(granularity), SECONDS);
      getCandlesFromTime(instrument, filePath, granularity, lastTimePlusGranularity);
    }
  }

  public void getCandlesFromTime(EInstrument instrument, String filePath, CandlestickGranularity granularity, Instant fromTime) {
    var to = Instant.now();
    if (to.isBefore(fromTime)) {
      log.info("Abort get candles from time: no new candle ready for instrument {} with granularity {}",
          instrument, granularity);
      return;
    }

    candleRestClient
        .findBetween(instrument,
            granularity,
            Map.of(FROM, Rfc3339.YMDHMSN_FORMATTER.format(fromTime),
                TO, Rfc3339.YMDHMSN_FORMATTER.format(to)))
        .subscribe(new CandlestickWriterSubscriber(filePath, candlestickMapper));
  }

  /**
   * Maximum candle count is 5000. Any granularity seems fine.
   */
  public void getCandlestickWithCount(EInstrument instrument, String filePath, CandlestickGranularity granularity, int count) {
    candleRestClient
        .findBetween(instrument, granularity, Map.of(COUNT, count))
        .subscribe(new CandlestickWriterSubscriber(filePath, candlestickMapper));
  }

  public static CsvCandle getLastCandle(String filePath) {
    var firstAndLastLine = getFirstAndLastLineFromFile(filePath);
    var concat = "%s\n%s".formatted(firstAndLastLine.first(), firstAndLastLine.last());
    return csvStringToCsvCandlePojo(concat);
  }

  @NotNull
  private static List<InstrumentCandleRequestInfo> getInstrumentToGranularityToPath(
      List<EInstrument> instrumentList, List<CandlestickGranularity> granularityList) {
    return instrumentList
        .stream()
        .flatMap(i -> granularityList
            .stream()
            .map(g -> InstrumentCandleRequestInfo
                .builder()
                .granularity(g)
                .instrument(i)
                .outputPath(TEMPLATE_CANDLE_FILE_PATH.formatted(i.toString(), g))
                .build()))
        .toList();
  }

  @Builder
  @With
  public record InstrumentCandleRequestInfo(EInstrument instrument,
                                            CandlestickGranularity granularity,
                                            String outputPath) {
  }

  private static long granularityToSeconds(CandlestickGranularity granularity) {
    return switch (granularity) {
      case S5 -> 5;
      case S10 -> 10;
      case S15 -> 15;
      case S30 -> 30;
      case M1 -> 60;
      case M2 -> 120;
      case M4 -> 240;
      case M5 -> 300;
      case M10 -> 600;
      case M15 ->
          1_800; // times 2 because it seems like the candles are delayed 15m; so this avoids fetching for nothing
      case M30 -> 1_800;
      case H1 -> 3_600;
      case H2 -> 7_200;
      case H3 -> 10_800;
      case H4 -> 14_400;
      case H6 -> 21_600;
      case H8 -> 28_800;
      case H12 -> 43_200;
      case D -> 86_400;
      case W -> 604_800; // assuming 7 days
      case M -> throw new UnsupportedOperationException("No seconds for monthly; ambiguous number of days");
    };
  }

}
