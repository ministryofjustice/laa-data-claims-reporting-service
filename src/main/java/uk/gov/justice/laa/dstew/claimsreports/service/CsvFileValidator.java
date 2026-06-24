package uk.gov.justice.laa.dstew.claimsreports.service;

import static uk.gov.justice.laa.dstew.claimsreports.utils.LogSanitiser.sanitise;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.CharBuffer;
import java.nio.channels.Channels;
import java.nio.charset.CharacterCodingException;
import java.nio.charset.CodingErrorAction;
import java.nio.charset.MalformedInputException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.Locale;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import uk.gov.justice.laa.dstew.claimsreports.exception.CsvUploadException;

/**
 * Handles logic related to ensuring our generated CSV file is valid.
 */
@Service
@Slf4j
public class CsvFileValidator {

  static final int BUFFER_SIZE = 4096;

  /**
   * Check the file is UTF-8 encoded.
   * Important if changing this that you do not load the whole file into memory at once!
   * We manually check as libraries like Tika just look at 10kb or so and predict based on that, which feels insufficient for our purpose and sizes
   *
   * @param fileToUpload file to check
   * @return true if UTF-8, false otherwise
   */
  public boolean checkUtf8Encoded(File fileToUpload) {

    var decoder = StandardCharsets.UTF_8
        .newDecoder()
        .onMalformedInput(CodingErrorAction.REPORT)
        .onUnmappableCharacter(CodingErrorAction.REPORT);

    // inBuffer is what we read the csv file into
    var inBuffer = ByteBuffer.allocate(BUFFER_SIZE);
    // outBuffer has the "decoded" UTF-8 chars in.
    var outBuffer = CharBuffer.allocate(BUFFER_SIZE);

    var totalBytesRead = 0;

    try (var inputStream = new FileInputStream(fileToUpload)) {

      var readableByteChannel = Channels.newChannel(inputStream);

      int bytesRead;
      // We can't just read the whole file at once as it could be hundreds or thousands of MBs and will blow the memory
      // So we loop over a small section at a time
      // -1 is end of file (but still might have carry-over bytes in the inBuffer to handle after)
      while ((bytesRead = readableByteChannel.read(inBuffer)) != -1) {

        // Jump back to the start of the buffer
        inBuffer.flip();

        while (true) {
          var result = decoder.decode(inBuffer, outBuffer, false);

          if (result.isError()) {
            result.throwException();
          }
          if (result.isUnderflow()) {
            // Either a: all bytes in buffer have been consumed, or
            // b: start of multibyte character, and it needs to read the next bit
            inBuffer.compact();
            outBuffer.clear();
            break;
          }
          if (result.isOverflow()) {
            // This means outBuffer is full. We don't care about the contents so just clear it
            outBuffer.clear();
          }
        }

        totalBytesRead += bytesRead;
        outBuffer.clear();
      }

      // Tell the decoder we are finished.
      inBuffer.flip();
      var end = decoder.decode(inBuffer, outBuffer, true);
      if (end.isError()) {
        // This error would e.g. be that the last thing read was part of a multibyte sequence and so we underflowed above,
        // but there is no more to read so we need to error.
        end.throwException();
      }

      // Clear anything still left in the decoder.
      var flush = decoder.flush(outBuffer);
      if (flush.isError()) {
        flush.throwException();
      }

      // No error = all utf-8 encoded bytes.
      return true;

    } catch (MalformedInputException e) {
      int errorIndex = e.getInputLength();
      log.error(
              "Malformed UTF-8 at byte offset {} in file {}: {}",
              totalBytesRead + errorIndex,
              sanitise(fileToUpload.getPath()),
              sanitise(e.getMessage())
      );
      return false;
    } catch (CharacterCodingException e) {
      log.error("Failed to decode in UTF-8 with exception {}, {}", sanitise(e.getClass().getName()), sanitise(e.getMessage()));
      return false;
    } catch (IOException e) {
      log.error("Failed to read generated CSV file {} with exception {}", sanitise(fileToUpload.getPath()), sanitise(e.getMessage()));
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
  @SuppressFBWarnings(
          value = "IMPROPER_UNICODE",
          justification =
                  "MIME type validated against fixed allow-list pattern, not used for case-sensitive security decisions"
  )
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

    if (!"text/csv".equalsIgnoreCase(mimeType)) {
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
    if (!fileName.toLowerCase(Locale.ROOT).endsWith(".csv")) {
      throw new CsvUploadException("File '" + fileName + "' does not have a .csv extension.");
    }

    // Desired key extension check
    if (!desiredFileKey.toLowerCase(Locale.ROOT).endsWith(".csv")) {
      throw new CsvUploadException("Target key '" + desiredFileKey + "' must end with .csv.");
    }

    return true;
  }

}
