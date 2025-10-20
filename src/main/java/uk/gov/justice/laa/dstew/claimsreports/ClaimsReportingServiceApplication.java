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
    try {
      //      System.out.println("Going to sleep");
      //      new ReportCreationService().saveCsv();
      //      System.out.println("File Created");

      SpringApplication.run(ClaimsReportingServiceApplication.class, args);
      //      new ReportCreationService().saveCsv();
      System.out.println("Done with the app");
    } catch (Exception exception) {
      System.out.println(exception.getClass());
    }
  }
}
