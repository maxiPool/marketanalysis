package max.demo.marketanalysis.infra.oanda.v20.candles;

import com.oanda.v20.instrument.Candlestick;
import com.oanda.v20.instrument.CandlestickGranularity;
import lombok.Builder;
import lombok.RequiredArgsConstructor;
import lombok.With;
import lombok.extern.slf4j.Slf4j;
import max.demo.marketanalysis.infra.oanda.v20.candles.csvutil.CsvCandle;
import max.demo.marketanalysis.infra.oanda.v20.candles.csvutil.CsvUtil;
import max.demo.marketanalysis.infra.oanda.v20.candles.resource.OandaRestResource;
import max.demo.marketanalysis.infra.oanda.v20.model.EInstrument;
import max.demo.marketanalysis.infra.oanda.v20.properties.V20Properties;
import org.jetbrains.annotations.NotNull;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.time.Instant;
import java.util.List;

import static java.time.temporal.ChronoUnit.SECONDS;
import static java.util.concurrent.CompletableFuture.runAsync;
import static java.util.stream.Collectors.joining;
import static max.demo.marketanalysis.infra.oanda.v20.candles.ReadFileUtil.getFirstAndLastLineFromFile;
import static max.demo.marketanalysis.infra.oanda.v20.candles.csvutil.CsvCandle.getSchemaHeader;
import static max.demo.marketanalysis.infra.oanda.v20.candles.csvutil.CsvUtil.csvStringToCsvCandlePojo;
import static max.demo.marketanalysis.infra.oanda.v20.model.Rfc3339.YMDHMS_FORMATTER;

@Slf4j
@Service
@RequiredArgsConstructor
@ConditionalOnProperty(prefix = "infra.oanda.v20.candlestick", name = "enabled", havingValue = "true")
public class CandlestickService {
  private static final int MAX_CANDLE_COUNT_OANDA_API = 5_000;

  private final V20Properties v20Properties;
  private final OandaRestResource oandaRestResource;
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

  /**
   * @param fromTime example fromTime = 8:15:00; lastCandleTime = 8:00:00; now = 8:14:00 --> abort get candles API call
   */
  public void getCandlesFromTime(EInstrument instrument, String filePath, CandlestickGranularity granularity, Instant fromTime) {
    var to = Instant.now();
    if (to.isBefore(fromTime)) {
      log.debug("Abort get candles from time: no new candle ready for instrument {} with granularity {}",
          instrument, granularity);
      return;
    }

    var candles = oandaRestResource
        .getCandlesFromTo(instrument, granularity, YMDHMS_FORMATTER.format(fromTime), YMDHMS_FORMATTER.format(to));
    onComplete(candles.getCandles(), filePath);
  }

  /**
   * Maximum candle count is 5000. Any granularity seems fine.
   */
  public void getCandlestickWithCount(EInstrument instrument, String filePath, CandlestickGranularity granularity, int count) {
    var candles = oandaRestResource.getCandlesWithCount(instrument, granularity, count);
    onComplete(candles.getCandles(), filePath);
  }

  private void onComplete(List<Candlestick> candles, String filePath) {
    var contentToAppend = candles
        .stream()
        .filter(Candlestick::getComplete)
        .map(candlestickMapper::oandaCandleToCsvCandle)
        .map(CsvUtil::candleToCsv)
        .collect(joining(""));

    try {
      var path = Paths.get(filePath);
      if (!Files.exists(path)) {
        log.warn("Creating file that doesn't exist: {}", filePath);
        Files.createFile(path);
        contentToAppend = "%s\n%s".formatted(getSchemaHeader(), contentToAppend);
      }

      log.debug("Writing to file: {}", filePath);
      Files.writeString(
          path,
          contentToAppend,
          StandardOpenOption.APPEND);
      log.debug("Done with file: {}", filePath);
    } catch (IOException e) {
      log.error("Error while appending candles to file: {}", filePath);
    }
  }

  public static CsvCandle getLastCandle(String filePath) {
    var firstAndLastLine = getFirstAndLastLineFromFile(filePath);
    var concat = "%s\n%s".formatted(firstAndLastLine.first(), firstAndLastLine.last());
    return csvStringToCsvCandlePojo(concat);
  }

  @NotNull
  private List<InstrumentCandleRequestInfo> getInstrumentToGranularityToPath(
      List<EInstrument> instrumentList, List<CandlestickGranularity> granularityList) {
    return instrumentList
        .stream()
        .flatMap(i -> granularityList
            .stream()
            .map(g -> InstrumentCandleRequestInfo
                .builder()
                .granularity(g)
                .instrument(i)
                .outputPath(v20Properties.candlestick().outputPathTemplate().formatted(i.toString(), g))
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
      case M15 -> 900;
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
