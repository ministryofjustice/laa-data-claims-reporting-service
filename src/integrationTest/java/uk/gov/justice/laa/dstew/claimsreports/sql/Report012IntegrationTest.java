package uk.gov.justice.laa.dstew.claimsreports.sql;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import uk.gov.justice.laa.dstew.claimsreports.IntegrationTestBase;

@Slf4j
class Report012IntegrationTest extends IntegrationTestBase {

    @AfterEach
    void cleanup() {
        cleanUpDataFromTests();
    }

    @Test
    void areaOfLawValuesArePassedThroughFromDbCorrectly() {
        // Refresh MV to ensure it is up to date in case anything has changed
        jdbcTemplate.update("REFRESH MATERIALIZED VIEW claims.mvw_report_012");

        // When (we pull data from the database)
        List<Map<String, Object>> areaOfLawCounts = jdbcTemplate.queryForList("""
                SELECT "Area of law", count(*)
                FROM claims.mvw_report_012
                GROUP BY "Area of law"
                """
        );

        // Then (we ensure the correct amount of data has been pulled as expected)
        assertThat(areaOfLawCounts).isNotNull().isNotEmpty().hasSize(2);

        Map<String, Long> countsByAreaOfLaw =
                areaOfLawCounts.stream()
                        .collect(Collectors.toMap(
                                row -> (String) row.get("Area of Law"),
                                row -> ((Number) row.get("count")).longValue()
                        ));

        // And (we assert the values match up, as expected)
        assertThat(countsByAreaOfLaw)
                .containsKeys("LEGAL_HELP", "CRIME_LOWER");

        assertThat(countsByAreaOfLaw.get("LEGAL_HELP")).isEqualTo(1L);
        assertThat(countsByAreaOfLaw.get("CRIME_LOWER")).isEqualTo(2L);

        long totalRows =
                countsByAreaOfLaw.values().stream().mapToLong(Long::longValue).sum();

        assertThat(totalRows).isEqualTo(3L);
    }


}
