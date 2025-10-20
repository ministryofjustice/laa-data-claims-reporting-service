package uk.gov.justice.laa.dstew.claimreports.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import org.mockito.Mock;
import static org.mockito.Mockito.when;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.gov.justice.laa.dstew.claimsreports.ReportDao;
import uk.gov.justice.laa.dstew.claimsreports.service.ReportCreationService;

import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.UUID;

@ExtendWith(MockitoExtension.class)
public class ReportCreationServiceTest {

    private ReportCreationService reportCreationService;

    @Mock
    ReportDao reportDao;

    @BeforeEach
    void setup() {
        reportCreationService = new ReportCreationService(reportDao);
    }

    @Test
    void createCsvShouldGenerateFile() throws Exception {
        var map = new LinkedHashMap<String, Object>() {
            {
                put("id", UUID.fromString("11111111-e5c2-45b6-8a78-86ddf751d274"));
                put("bulk_submission_id", UUID.fromString("22222222-e5c2-45b6-8a78-86ddf751d274"));
                put("office_account_number", "office number");
                put("submission_period", "submission period");
                put("area_of_law", "law area");
                put("status", "CREATED");
                put("crime_schedule_number", "schedule no.");
                put("previous_submission_id", UUID.fromString("33333333-e5c2-45b6-8a78-86ddf751d274"));
                put("is_nil_submission", 1);
                put("number_of_claims", 50);
                put("error_messages", "error message");
                put("created_by_user_id", "created user");
                put("created_on", Timestamp.valueOf(LocalDateTime.of(2023, 8, 7, 0, 0)));
                put("updated_by_user_id", "updated user");
                put("updated_on", Timestamp.valueOf(LocalDateTime.of(2023, 8, 7, 0, 0)));
                put("civil_submission_reference", "civil ref");
                put("mediation_submission_reference", "mediated ref");
            }
        };
        when(reportDao.callDataBase(any())).thenReturn(Collections.singletonList(map));
        reportCreationService.saveCsv();
    }

}
