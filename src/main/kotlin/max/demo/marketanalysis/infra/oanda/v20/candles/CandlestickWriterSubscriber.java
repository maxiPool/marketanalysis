package max.demo.marketanalysis.infra.oanda.v20.candles;

import com.oanda.v20.instrument.Candlestick;
import lombok.extern.slf4j.Slf4j;
import org.reactivestreams.Subscriber;
import org.reactivestreams.Subscription;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.ArrayList;
import java.util.List;

import static max.demo.marketanalysis.infra.oanda.v20.candles.csvutil.CsvCandle.getSchemaHeader;
import static max.demo.marketanalysis.infra.oanda.v20.candles.csvutil.CsvUtil.candleToCsv;

@Slf4j
public class CandlestickWriterSubscriber implements Subscriber<Candlestick> {

  private final String filePath;
  private final CandlestickMapper candlestickMapper;
  private final List<String> csvCandles;

  private Subscription s;

  public CandlestickWriterSubscriber(String filePath, CandlestickMapper candlestickMapper) {
    this.filePath = filePath;
    this.candlestickMapper = candlestickMapper;
    csvCandles = new ArrayList<>();
  }

  @Override
  public void onSubscribe(Subscription subscription) {
    s = subscription;
    s.request(2L);
  }

  @Override
  public void onError(Throwable throwable) {
  }

  @Override
  public void onNext(Candlestick candlestick) {
    if (!candlestick.getComplete()) {
      return;
    }
    var csvCandle = candlestickMapper.oandaCandleToCsvCandle(candlestick);
    var csvString = candleToCsv(csvCandle);
    csvCandles.add(csvString);
    s.request(2L);
  }

  @Override
  public void onComplete() {
    log.info("csv candles: {}", csvCandles);
    var contentToAppend = String.join("", csvCandles);

    try {
      var path = Paths.get(filePath);
      if (!Files.exists(path)) {
        Files.createFile(path);
        contentToAppend = "%s\n%s".formatted(getSchemaHeader(), contentToAppend);
      }
      Files.writeString(
          path,
          contentToAppend,
          StandardOpenOption.APPEND);
    } catch (IOException e) {
      log.error("Error while appending candles to file: {}", filePath);
    }
  }

}
