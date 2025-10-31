package uk.gov.justice.laa.dstew.claimsreports.service.s3;

import java.io.File;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class S3FileUploaderTest {

  @Mock
  private S3ClientWrapper s3ClientWrapper;

  @Test
  void s3FileUploader_shouldCallTheS3Client(){
    var s3FileUploader = new S3FileUploader(s3ClientWrapper);
    var file = mock(File.class);

    s3FileUploader.uploadFile(file, "file.csv");

    verify(s3ClientWrapper).uploadFile(file, "file.csv");
  }

}