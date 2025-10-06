package uk.gov.justice.laa.dstew.claimsreports;

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
    //    SpringApplication.run(ClaimsReportingServiceApplication.class, args);
    //    Hello world print out is to allow confirmation of successful deployment in uat.
    //    This will be removed and replaced with the running application in subsequent tickets.
    System.out.println("Hello world");
  }
}
