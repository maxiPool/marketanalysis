package max.demo.marketanalysis.infra.oanda.v20.candles;

import com.oanda.v20.instrument.Candlestick;
import com.oanda.v20.pricing_common.PriceValue;
import max.demo.marketanalysis.infra.oanda.v20.candles.csvutil.CsvCandle;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

import static org.mapstruct.MappingConstants.ComponentModel.SPRING;

@Mapper(componentModel = SPRING)
public interface CandlestickMapper {

  @Mapping(target = "open", source = "mid.o")
  @Mapping(target = "high", source = "mid.h")
  @Mapping(target = "low", source = "mid.l")
  @Mapping(target = "close", source = "mid.c")
  @Mapping(target = "isComplete", source = "complete")
  CsvCandle oandaCandleToCsvCandle(Candlestick candlestick);

  default Double priceValueToDouble(PriceValue priceValue) {
    return priceValue.doubleValue();
  }

}
