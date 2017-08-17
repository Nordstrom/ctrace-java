package io.ctrace.demo.spring;

import static org.springframework.web.bind.annotation.RequestMethod.GET;

import java.util.Objects;
import lombok.extern.log4j.Log4j2;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.client.RestTemplate;

@RestController
@Log4j2
public class Controller {

  /**
   * Gateway method.
   *
   * @param url url to gateway to
   * @param region region (default World)
   * @return string response
   */
  @RequestMapping(value = "/gw", method = GET)
  public ResponseEntity<String> gateway(
      @RequestParam(value = "url") String url,
      @RequestParam(value = "region", defaultValue = "World") String region) {

    log.info("gateway");
    RestTemplate restTemplate = new RestTemplate();
    ResponseEntity<String> response = restTemplate
        .getForEntity(url + "?region=" + region, String.class);

    return new ResponseEntity<>(response.getBody(), response.getStatusCode());
  }

  /**
   * Hello method.
   *
   * @param region region
   * @return string response
   */
  @RequestMapping(value = "/hello", method = GET)
  public ResponseEntity<String> hello(@RequestParam(value = "region") String region) {
    if (Objects.equals(region, "error")) {
      return new ResponseEntity<>("Error", HttpStatus.BAD_REQUEST);
    } else {
      return new ResponseEntity<>("Hello " + region, HttpStatus.OK);
    }
  }
}
