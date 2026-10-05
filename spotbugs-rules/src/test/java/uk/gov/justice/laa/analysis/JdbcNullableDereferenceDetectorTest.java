package uk.gov.justice.laa.analysis;

import edu.umd.cs.findbugs.BugInstance;
import edu.umd.cs.findbugs.MethodAnnotation;
import edu.umd.cs.findbugs.test.SpotBugsExtension;
import edu.umd.cs.findbugs.test.SpotBugsRunner;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import java.net.URISyntaxException;
import java.net.URL;
import java.nio.file.Path;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.*;

@ExtendWith(SpotBugsExtension.class)
public class JdbcNullableDereferenceDetectorTest
{
    private static final String RULE = "JDBC_NULL_DEREFERENCE";
    private static final String FIXTURE = "uk/gov/justice/laa/analysis/JdbcNullableDereferenceFixture.class";

    @Test
    void shouldDetectDirectUnsafeJdbcCalls(SpotBugsRunner runner) throws URISyntaxException {
       Set<String> detected = analyseFixture(runner);

       assertEquals(Set.of("unsafeTimestampColumnName", "unsafeTimestampColumnIndex", "unsafeStringTrim"), detected);
    }

    private Set<String> analyseFixture(SpotBugsRunner runner) throws URISyntaxException {
        URL resource = getClass().getClassLoader().getResource(FIXTURE);

        assertNotNull(resource, "Cannot find compiled JdbcNullableDereferenceFixture.class .");

        Path classFile = Path.of(resource.toURI());

        return runner.performAnalysis(classFile).getCollection()
                .stream()
                .filter(bug -> RULE.equals(bug.getType()))
                .map(BugInstance::getPrimaryMethod)
                .filter(Objects::nonNull)
                .map(MethodAnnotation::getMethodName)
                .collect(Collectors.toSet());
    }
}
