package max.demo.marketanalysis.demo;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("api/v1")
public class DemoController {

  @GetMapping("hello")
  public String hello() {
    return "hello";
  }

}
