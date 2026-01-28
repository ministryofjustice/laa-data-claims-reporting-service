package uk.gov.justice.laa.dstew.claimsreports.service;

import java.io.File;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.file.Files;
import lombok.SneakyThrows;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.MockedStatic;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.gov.justice.laa.dstew.claimsreports.exception.CsvUploadException;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

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
      out.write("test, test".getBytes());
      out.write(new byte[]{(byte) 0xF0}); //Not valid in utf-8
      out.write(", test, test".getBytes());
    }

    assertFalse(csvFileValidator.checkUtf8Encoded(path.toFile()));
    Files.deleteIfExists(path);
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