package uk.gov.justice.laa.dstew.claimsreports;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * Entry point for the Spring Boot microservice application.
 */
@SpringBootApplication
public class ClaimsReportingServiceApplication {

  /**
   * The application main method.
   *
   * @param args the application arguments.
   */
  public static void main(String[] args) {
    SpringApplication.run(ClaimsReportingServiceApplication.class, args);
  }
}
