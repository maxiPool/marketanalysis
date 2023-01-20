package max.demo.marketanalysis.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.filter.CommonsRequestLoggingFilter;

@Configuration
public class RequestLoggingFilterConfig {

  @Bean
  public CommonsRequestLoggingFilter logFilter() {
    var filter = new CommonsRequestLoggingFilter();

    filter.setIncludeQueryString(true);
    filter.setIncludeClientInfo(true);
    filter.setIncludeHeaders(true);
    filter.setIncludePayload(true);
    filter.setMaxPayloadLength(3200);

    return filter;
  }

}
