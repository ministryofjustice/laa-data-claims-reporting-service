package uk.gov.justice.laa.dstew.claimreports.dao;

import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.gov.justice.laa.dstew.claimsreports.ReportDao;

import static org.junit.jupiter.api.Assertions.assertFalse;

@ExtendWith(MockitoExtension.class)
public class ReportDaoTest {

  @Mock
  private ReportDao reportDao;


//  @Test
//  void getSubmissionShouldReturnRowOfData() {
//    assertFalse(reportDao.callDataBase(null).isEmpty());
//  }

}
