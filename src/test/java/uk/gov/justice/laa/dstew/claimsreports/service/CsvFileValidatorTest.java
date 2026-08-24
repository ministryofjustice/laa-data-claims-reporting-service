package uk.gov.justice.laa.dstew.claimsreports.service;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.Arrays;
import java.util.regex.Pattern;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.MockedStatic;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;

import lombok.SneakyThrows;
import uk.gov.justice.laa.dstew.claimsreports.exception.CsvUploadException;
import static uk.gov.justice.laa.dstew.claimsreports.service.CsvFileValidator.BUFFER_SIZE;

@ExtendWith(MockitoExtension.class)
class CsvFileValidatorTest {

  private final CsvFileValidator csvFileValidator = new CsvFileValidator();
  private final ClassLoader classLoader = Thread.currentThread().getContextClassLoader();

  @Test
  void checkUtf8Encoded_shouldReturnTrueIfValidCsv() {
    var testFile = new File(classLoader.getResource("testReport.csv").getFile());
    assertTrue(csvFileValidator.checkUtf8Encoded(testFile));
  }

  @Test
  void checkUtf8Encoded_shouldReturnFalseIfCannotReadFile() {
    assertFalse(csvFileValidator.checkUtf8Encoded(new File("dontexist.csv")));
  }

  @SneakyThrows
  @Test
  void checkUtf8Encoded_shouldReturnFalseIfNotValidUtf8File() {
    var path = Files.createTempFile("notUtf8", ".csv");
    try (OutputStream out = Files.newOutputStream(path)) {
      out.write("test, test".getBytes(StandardCharsets.UTF_8));
      out.write(new byte[]{(byte) 0xF0}); //Not valid in utf-8
      out.write(", test, test".getBytes(StandardCharsets.UTF_8));
    }

    assertFalse(csvFileValidator.checkUtf8Encoded(path.toFile()));
    Files.deleteIfExists(path);
  }

  @Test
  public void testValidUtf8TwoBytesCharacterSplitAcrossChunks() throws IOException {
    byte[] pound = "£".getBytes(StandardCharsets.UTF_8); // C2 A3

    // Want to force our pound sign to be spread across buffer reads.
    byte[] padding = new byte[BUFFER_SIZE - 1];
    Arrays.fill(padding, (byte) ' ');

    var path = File.createTempFile("utf8-split-valid", ".txt");
    try (FileOutputStream out = new FileOutputStream(path)) {
      out.write(padding);
      out.write(pound);
      out.write("Hello World".getBytes(StandardCharsets.UTF_8));
    }
    assertTrue(csvFileValidator.checkUtf8Encoded(path));
  }

  @Test
  public void testValidUtf8ThreeBytesCharacterSplitAcrossChunks() throws IOException {
    byte[] dash = "–".getBytes(StandardCharsets.UTF_8); // E2 80 93

    // Want to force our dash to be spread across buffer reads.
    byte[] padding = new byte[BUFFER_SIZE - 2];
    Arrays.fill(padding, (byte) ' ');

    var path = File.createTempFile("utf8-split-valid", ".txt");
    try (FileOutputStream out = new FileOutputStream(path)) {
      out.write(padding);
      out.write(dash);
      out.write("Hello World".getBytes(StandardCharsets.UTF_8));
    }
   assertTrue(csvFileValidator.checkUtf8Encoded(path));
  }

  @Test
  public void testValidUtf8FourBytesCharacterSplitAcrossChunks() throws IOException {
    // This is a smiley emoji.
    byte[] emoji = "\uD83D\uDE00".getBytes(StandardCharsets.UTF_8); // F0 9F 98 80

    // Want to force our emoji to be spread across buffer reads.
    byte[] padding = new byte[BUFFER_SIZE - 2];
    Arrays.fill(padding, (byte) ' ');

    var path = File.createTempFile("utf8-split-valid", ".txt");
    try (FileOutputStream out = new FileOutputStream(path)) {
      out.write(padding);
      out.write(emoji);
      out.write("Hello World".getBytes(StandardCharsets.UTF_8));
    }
  assertTrue(csvFileValidator.checkUtf8Encoded(path));
  }

  @Test
  public void testIncompleteUtf8AtEOF() throws IOException {
    // "–" = E2 80 93, write only E2 80 (incomplete)
    byte[] dash = "–".getBytes(StandardCharsets.UTF_8);
    byte[] incomplete = new byte[]{dash[0], dash[1]}; // E2 80

    var path = File.createTempFile("utf8-incomplete-eof", ".txt");
    try (FileOutputStream out = new FileOutputStream(path)) {
      out.write("Hello ".getBytes(StandardCharsets.UTF_8));
      out.write(incomplete);
    }

    assertFalse(csvFileValidator.checkUtf8Encoded(path));
  }

  @Test
  void checkFileExtension_shouldReturnTrueIfCsv() {
    assertTrue(csvFileValidator.checkFileExtension("blah.csv", "bash.csv"));
  }

  @Test
  void checkFileExtension_shouldErrorIfTryingToSetFileNameThatIsNotCsv() {
    assertThrows(CsvUploadException.class, () -> csvFileValidator.checkFileExtension("blah.exe", "bash.csv"));
  }

  @Test
  void checkFileExtension_shouldErrorIfTryingToUploadFileThatIsNotCsv() {
    assertThrows(CsvUploadException.class, () -> csvFileValidator.checkFileExtension("blah.csv", "bash.exe"));
  }

  @SneakyThrows
  @Test
  void checkCsvHeaders_shouldReturnTrueWhenHeadersMatch() {
    var path = Files.createTempFile("headers-match", ".csv");
    Files.writeString(path, "\"First name\",Age\nAlice,42\n");

    assertTrue(csvFileValidator.checkCsvHeaders(path.toFile(), Arrays.asList("First name", "Age")));
    Files.deleteIfExists(path);
  }

  @SneakyThrows
  @Test
  void checkCsvHeaders_shouldReturnFalseWhenHeadersDoNotMatch() {
    var path = Files.createTempFile("headers-mismatch", ".csv");
    Files.writeString(path, "\"First name\",Age\nAlice,42\n");

    assertFalse(csvFileValidator.checkCsvHeaders(path.toFile(), Arrays.asList("First name", "Name")));
    Files.deleteIfExists(path);
  }

  @SneakyThrows
  @Test
  void checkCsvHeaders_shouldValidateDynamicHeadersAfterExpectedPrefix() {
    var path = Files.createTempFile("dynamic-headers", ".csv");
    Files.writeString(path, "\"Provider Office Account Number\",\"Area of Law\",APR-2025\n001,CIVIL,1\n");

    assertTrue(csvFileValidator.checkCsvHeaders(path.toFile(),
        Arrays.asList("Provider Office Account Number", "Area of Law"),
        Pattern.compile("(?:JAN|FEB|MAR|APR|MAY|JUN|JUL|AUG|SEP|OCT|NOV|DEC)-\\d{4}")));
    Files.deleteIfExists(path);
  }

  @SneakyThrows
  @Test
  void checkCsvHeaders_shouldRejectDynamicHeadersWhenNoAdditionalHeadersExist() {
    var path = Files.createTempFile("missing-dynamic-headers", ".csv");
    Files.writeString(path, "\"Provider Office Account Number\",\"Area of Law\"\n");

    assertFalse(csvFileValidator.checkCsvHeaders(path.toFile(),
        Arrays.asList("Provider Office Account Number", "Area of Law"),
        Pattern.compile("(?:JAN|FEB|MAR|APR|MAY|JUN|JUL|AUG|SEP|OCT|NOV|DEC)-\\d{4}")));
    Files.deleteIfExists(path);
  }


  @Test
  void checkMimeTypeIsCsv_shouldReturnTrueForCsvMimeType() {
    File fakeFile = new File("test.csv");
    try (MockedStatic<Files> filesMock = Mockito.mockStatic(Files.class)) {
      filesMock.when(() -> Files.probeContentType(fakeFile.toPath())).thenReturn("text/csv");
      assertTrue(csvFileValidator.checkMimeTypeIsCsv(fakeFile));
    }
  }

  @Test
  void checkMimeTypeIsCsv_shouldThrowExceptionForNonCsvMimeType() {
    File fakeFile = new File("test.exe");
    try (MockedStatic<Files> filesMock = Mockito.mockStatic(Files.class)) {
      filesMock.when(() -> Files.probeContentType(fakeFile.toPath())).thenReturn("application/octet-stream");
      assertThrows(CsvUploadException.class, () -> csvFileValidator.checkMimeTypeIsCsv(fakeFile));
    }
  }

  @Test
  void checkMimeTypeIsCsv_shouldErrorWhenMimeTypeIsNull() {
    File fakeFile = new File("test.unknown");

    try (MockedStatic<Files> filesMock = Mockito.mockStatic(Files.class)) {
      filesMock.when(() -> Files.probeContentType(fakeFile.toPath()))
          .thenReturn(null);

      assertThrows(CsvUploadException.class,
          () -> csvFileValidator.checkMimeTypeIsCsv(fakeFile)
      );
    }
  }

  @Test
  void checkMimeTypeIsCsv_shouldWrapIOException() {
    File fakeFile = new File("test.csv");

    try (MockedStatic<Files> filesMock = Mockito.mockStatic(Files.class)) {
      filesMock.when(() -> Files.probeContentType(fakeFile.toPath()))
          .thenThrow(new IOException("IO"));

      assertThrows(CsvUploadException.class,
          () -> csvFileValidator.checkMimeTypeIsCsv(fakeFile)
      );
    }
  }

}