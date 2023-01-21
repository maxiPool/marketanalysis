package max.demo.marketanalysis.arbitrager;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import max.demo.marketanalysis.infra.oanda.v20.V20Service;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class ArbitrageService {

  private final V20Service v20Service;


}
