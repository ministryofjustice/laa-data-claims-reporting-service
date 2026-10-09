// package uk.gov.justice.laa.dstew.claimsreports.config;
//
// import static org.junit.jupiter.api.Assertions.assertEquals;
//
// import java.lang.reflect.Field;
// import java.time.Duration;
// import org.junit.jupiter.api.AfterEach;
// import org.junit.jupiter.api.Test;
// import software.amazon.awssdk.http.SdkHttpConfigurationOption;
// import software.amazon.awssdk.http.apache5.Apache5HttpClient;
// import software.amazon.awssdk.regions.Region;
// import software.amazon.awssdk.services.s3.S3Client;
// import software.amazon.awssdk.utils.AttributeMap;
//
// class LocalstackS3ConfigTest {
//
//  private final LocalstackS3Config localstackS3Config = new LocalstackS3Config();
//  private S3Client createdClient;
//  private Apache5HttpClient createdHttpClient;
//
//  @AfterEach
//  void cleanupCreatedClients() {
//    if (createdClient != null) {
//      createdClient.close();
//    }
//    if (createdHttpClient != null) {
//      createdHttpClient.close();
//    }
//  }
//
//  @Test
//  void shouldCreateLocalstackS3ClientWithConfiguredApiTimeouts() {
//    createdClient =
//        localstackS3Config.localstackS3Client(
//            "http://localhost:4566", "eu-west-1", "test", "test", new S3Timeouts(10, 60, 60,
// 180));
//
//    assertEquals(Region.EU_WEST_1, createdClient.serviceClientConfiguration().region());
//    assertEquals(
//        Duration.ofSeconds(60),
//        createdClient
//            .serviceClientConfiguration()
//            .overrideConfiguration()
//            .apiCallAttemptTimeout()
//            .orElseThrow());
//    assertEquals(
//        Duration.ofSeconds(180),
//        createdClient
//            .serviceClientConfiguration()
//            .overrideConfiguration()
//            .apiCallTimeout()
//            .orElseThrow());
//    assertEquals(
//        "http://localhost:4566",
//        createdClient.serviceClientConfiguration().endpointOverride().orElseThrow().toString());
//  }
//
//  @Test
//  void shouldCreateLocalstackHttpClientWithConfiguredConnectionAndSocketTimeouts()
//      throws ReflectiveOperationException {
//    createdHttpClient = localstackS3Config.localstackHttpClient(new S3Timeouts(10, 60, 60, 180));
//
//    var requestConfig = getFieldValue(createdHttpClient, "requestConfig");
//    var resolvedOptions = (AttributeMap) getFieldValue(createdHttpClient, "resolvedOptions");
//
//    assertEquals(
//        Duration.ofSeconds(60),
//        requestConfig.getClass().getMethod("socketTimeout").invoke(requestConfig));
//    assertEquals(
//        Duration.ofSeconds(10),
// resolvedOptions.get(SdkHttpConfigurationOption.CONNECTION_TIMEOUT));
//  }
//
//  private Object getFieldValue(Object target, String fieldName)
//      throws ReflectiveOperationException {
//    Field field = target.getClass().getDeclaredField(fieldName);
//    field.setAccessible(true);
//    return field.get(target);
//  }
// }
