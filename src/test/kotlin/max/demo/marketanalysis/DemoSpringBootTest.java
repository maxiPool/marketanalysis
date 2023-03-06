package max.demo.marketanalysis;

import max.demo.marketanalysis.infra.oanda.v20.PricePollingService;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import java.util.Scanner;

@Disabled
@SpringBootTest
@ActiveProfiles({"local"})
public class DemoSpringBootTest {

  @Autowired
  private PricePollingService pricePollingService;

  @Test
  void should_pollPrices() {
    pricePollingService.pollPrices();

    Scanner scanner = new Scanner(System.in);
    scanner.nextLine(); // wait for user input before ending test
  }

  @Test
  void should_getTradeableInstruments() {
    pricePollingService.getTradeableInstrumentsForAccount();
  }

}
