package uk.gov.justice.laa.dstew.claimsreports.service;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.charset.CharacterCodingException;
import java.nio.charset.CodingErrorAction;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import uk.gov.justice.laa.dstew.claimsreports.exception.CsvUploadException;

/**
 * Handles logic related to ensuring our generated CSV file is valid.
 */
@Service
@Slf4j
public class CsvFileValidator {

  private static final int BUFFER_SIZE = 4096;

  /**
   * Check the file is UTF-8 encoded.
   * Important if changing this that you do not load the whole file into memory at once!
   *
   * @param fileToUpload file to check
   * @return true if UTF-8, false otherwise
   */
  public boolean checkUtf8Encoded(File fileToUpload) {

    try (var inputStream = new FileInputStream(fileToUpload)) {

      var decoder = StandardCharsets.UTF_8
          .newDecoder()
          .onMalformedInput(CodingErrorAction.REPORT)
          .onUnmappableCharacter(CodingErrorAction.REPORT);

      // We can't just read the whole file at once as it could be hundreds or thousands of MBs and will blow the memory
      // So we loop over a small section at a time
      byte[] readingBuffer = new byte[BUFFER_SIZE];

      int bytesRead;
      while ((bytesRead = inputStream.read(readingBuffer)) != -1) {
        // Decoder needs a byte buffer not a byte array
        var byteBuffer = ByteBuffer.wrap(readingBuffer, 0, bytesRead);

        // No error = utf-8 was decoded successfully.
        // We don't actually care about the output, just that it doesn't throw an error. So don't save output
        decoder.decode(byteBuffer);
      }

      // Finish decoding anything left over
      decoder.decode(ByteBuffer.allocate(0));

      return true;

    } catch (CharacterCodingException e) {
      // Most likely a MalformedInputException
      log.error("Failed to decode in UTF-8 with exception {}, {}", e.getClass().getName(), e.getMessage());
      return false;
    } catch (IOException e) {
      // Either IOException from the read or CharacterCodingException from decoding
      log.error("Failed to read generated CSV file {} with exception {}", fileToUpload.getPath(), e.getMessage());
      return false;
    }
  }

  /**
   * Check the file actually is a CSV file.
   *
   * @param fileToUpload file to check
   * @return true if CSV file
   * @throws CsvUploadException thrown when not a valid CSV file
   */
  public boolean checkMimeTypeIsCsv(File fileToUpload) throws CsvUploadException {
    String fileName = fileToUpload.getName();
    String mimeType;
    try {
      mimeType = Files.probeContentType(fileToUpload.toPath());
    } catch (IOException e) {
      throw new CsvUploadException("Unable to determine MIME type for file: " + fileName, e);
    }

    // 1. MIME type check
    if (mimeType == null) {
      throw new CsvUploadException("Could not detect MIME type for file: " + fileName);
    }

    if (!mimeType.equalsIgnoreCase("text/csv")) {
      throw new CsvUploadException("File '" + fileName + "' has invalid MIME type: " + mimeType + ". Expected 'text/csv'.");
    }

    return true;
  }

  /**
   * Check the filename is sensible.
   *
   * @param fileName       file created
   * @param desiredFileKey file name to use in S3
   * @return true if correct
   * @throws CsvUploadException thrown when incorrect filename
   */
  public boolean checkFileExtension(String fileName, String desiredFileKey) throws CsvUploadException {
    // File extension check
    if (!fileName.toLowerCase().endsWith(".csv")) {
      throw new CsvUploadException("File '" + fileName + "' does not have a .csv extension.");
    }

    // Desired key extension check
    if (!desiredFileKey.toLowerCase().endsWith(".csv")) {
      throw new CsvUploadException("Target key '" + desiredFileKey + "' must end with .csv.");
    }

    return true;
  }

}
