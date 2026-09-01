package uk.gov.justice.laa.dstew.claimsreports.runner;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;
import org.apache.commons.lang3.tuple.Pair;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.test.context.TestPropertySource;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.DeleteObjectsRequest;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.model.ListObjectsV2Request;
import software.amazon.awssdk.services.s3.model.ListObjectsV2Response;
import software.amazon.awssdk.services.s3.model.ObjectIdentifier;
import software.amazon.awssdk.services.s3.model.S3Object;
import uk.gov.justice.laa.dstew.claimsreports.IntegrationTestBase;

@TestPropertySource(properties = "feature.enable-rep012-xlsx=true")
class ClaimsReportingServiceRunnerRep012XlsxIntegrationTest extends IntegrationTestBase {

  @Value("${S3_REPORT_STORE}")
  private String bucketName;

  @Autowired
  private ClaimsReportingServiceRunner serviceRunner;

  @Autowired
  private S3Client s3Client;

  @BeforeEach
  void setup() {
    jdbcTemplate.update(DELETE_FROM_REPLICATION_SUMMARY);
    createReplicationSummaryTestData(
        LocalDate.now(staticClock).minusDays(1),
        OffsetDateTime.now(staticClock),
        Map.of(
            CLAIM_TABLE_NAME, Pair.of(5, 2),
            CLIENT_TABLE_NAME, Pair.of(4, 2),
            CLAIM_SUMMARY_FEE_TABLE_NAME, Pair.of(5, 3)
        )
    );
  }

  @AfterEach
  void emptyS3Bucket() {
    ListObjectsV2Response listResponse = s3Client.listObjectsV2(ListObjectsV2Request.builder()
        .bucket(bucketName)
        .build());

    if (listResponse.hasContents()) {
      List<ObjectIdentifier> objects = listResponse.contents().stream()
          .map(o -> ObjectIdentifier.builder().key(o.key()).build())
          .toList();

      s3Client.deleteObjects(DeleteObjectsRequest.builder()
          .bucket(bucketName)
          .delete(d -> d.objects(objects))
          .build());
    }
  }

  @Test
  void shouldGenerateRep012AsXlsxWhenFeatureEnabled() throws IOException {
    serviceRunner.run(null);

    ListObjectsV2Response listResponse = s3Client.listObjectsV2(ListObjectsV2Request.builder()
        .bucket(bucketName)
        .build());

    List<String> uploadedFiles = listResponse.contents().stream()
        .map(S3Object::key)
        .toList();

    assertThat(uploadedFiles).hasSize(5);
    assertThat(uploadedFiles).contains("reports/daily/report_012_2025-11-21.xlsx");
    assertThat(uploadedFiles).doesNotContain("reports/daily/report_012_2025-11-21.csv");

    Path tempFile = Files.createTempFile("rep012-", ".xlsx");
    try (InputStream s3is = s3Client.getObject(GetObjectRequest.builder()
        .bucket(bucketName)
        .key("reports/daily/report_012_2025-11-21.xlsx")
        .build())) {
      Files.copy(s3is, tempFile, StandardCopyOption.REPLACE_EXISTING);
    }

    try (var workbook = new XSSFWorkbook(Files.newInputStream(tempFile))) {
      var sheet = workbook.getSheetAt(0);
      assertThat(sheet.getRow(0).getCell(0).getStringCellValue()).isEqualTo("Provider office account number");
      assertThat(sheet.getLastRowNum()).isGreaterThan(0);
    } finally {
      Files.deleteIfExists(tempFile);
    }
  }
}
