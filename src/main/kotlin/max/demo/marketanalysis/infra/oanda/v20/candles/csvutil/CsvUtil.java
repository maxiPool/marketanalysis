package max.demo.marketanalysis.infra.oanda.v20.candles.csvutil;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.dataformat.csv.CsvMapper;
import com.fasterxml.jackson.dataformat.csv.CsvSchema;
import lombok.experimental.UtilityClass;
import lombok.extern.slf4j.Slf4j;

@UtilityClass
@Slf4j
public class CsvUtil {

  private static final CsvMapper csvMapper;
  private static final CsvSchema csvSchemaWithHeader;
  private static final CsvSchema csvSchema;
  private static final CsvSchema csvCandleSchema;
  private static final CsvSchema headerSchema;

  static {
    csvMapper = new CsvMapper();

    csvSchemaWithHeader = csvMapper
            .schemaFor(CsvCandle.class)
            .withHeader();

    csvSchema = csvMapper
            .schemaFor(CsvCandle.class)
            .withoutHeader();

    csvCandleSchema = csvMapper
            .schemaFor(CsvCandle.class);

    headerSchema = CsvSchema.emptySchema().withHeader();
  }

  public static String candlesToCsv(CsvCandle[] candles) {
    String csvString = null;
    try {
      csvString = csvMapper
              .writerFor(CsvCandle[].class)
              .with(csvSchemaWithHeader)
              .writeValueAsString(candles);
    } catch (JsonProcessingException e) {
      log.error("Error while converting candles array to csv", e);
    }
    log.info("CsvString: {}", csvString);
    return csvString;
  }


  public static String candleToCsv(CsvCandle candle) {
    String csvString = null;
    try {
      csvString = csvMapper
              .writerFor(CsvCandle.class)
              .with(csvSchema)
              .writeValueAsString(candle);
    } catch (JsonProcessingException e) {
      log.error("Error while converting candle to csv", e);
    }
    log.info("CsvString: {}", csvString);
    return csvString;
  }

  public static CsvCandle csvStringToCsvCandlePojo(String csvString) {
    try {
      var csvCandle = csvMapper
              .readerFor(CsvCandle.class)
              .with(csvSchemaWithHeader)
              .<CsvCandle>readValue(csvString);
      log.info("CsvCandle: {}", csvCandle);
      return csvCandle;
    } catch (JsonProcessingException e) {
      log.error("Error while converting candle to csv", e);
    }
    return null;
  }

}
