package uk.gov.justice.laa.dstew.claimsreports.service;

import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.IntStream;

@SuppressWarnings("checkstyle:MissingJavadocType")
public class FakeDataService {
  private List<Map<String, Object>> submissions = new ArrayList<>();

  /**
   * Create large volume of data.
   *
   * @return data rows
   */
  public List<Map<String, Object>> buildSubmissionData() {
    System.out.println("Off we go!");
    IntStream.rangeClosed(0, 100).forEach(i -> submissions.add(addRow()));
    System.out.println("I made the rows");

    return submissions;
  }

  private Map<String, Object> addRow() {
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
    return map;
  }


}

