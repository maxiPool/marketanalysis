package max.demo.marketanalysis.infra.oanda.v20.candles.model;

public enum EGetCandlesState {
  SUCCESS,
  ERROR,
  NEW_GET_5K_CANDLES,
  NEXT_CANDLE_NOT_COMPLETE,
  NO_NEW_CANDLES
}
