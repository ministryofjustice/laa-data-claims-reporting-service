package uk.gov.justice.laa.dstew.claimsreports.service;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVPrinter;
import org.apache.commons.io.FileUtils;
import org.springframework.stereotype.Service;
import uk.gov.justice.laa.dstew.claimsreports.ReportDao;

/**
 * Creates .csv files using data in claims data store.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class ReportCreationService {

  private final ReportDao reportDao;

  /**
   * Write data to csv file.
   *
   * @throws IOException from createCsvStreamv
   */
  public void saveCsvWithStream() throws Exception {
    System.out.println("starting save with streams");

    String csvFile = "test_even_bigger.csv";
    String sqlQuery = "SELECT * FROM claims.small_test_table";

    try {
      reportDao.buildCsvFromData(sqlQuery, csvFile);
    } catch (Exception ex) {
      System.out.println("This has failed");
    }
  }

}