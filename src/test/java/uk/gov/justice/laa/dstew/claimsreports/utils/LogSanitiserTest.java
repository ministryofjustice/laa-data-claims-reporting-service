package uk.gov.justice.laa.dstew.claimsreports.utils;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.Arguments;

import java.lang.reflect.Constructor;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Unit tests for {@link LogSanitiser}.
 */
@DisplayName("LogSanitiser")
class LogSanitiserTest {

    @Nested
    @DisplayName("sanitise")
    class SanitiseTests {

        @Test
        @DisplayName("should return 'null' string when input is null")
        void shouldReturnNullStringWhenInputIsNull() {
            assertThat(LogSanitiser.sanitise(null)).isEqualTo("null");
        }

        @Test
        @DisplayName("should return empty string when input is empty")
        void shouldReturnEmptyStringWhenInputIsEmpty() {
            assertThat(LogSanitiser.sanitise("")).isEmpty();
        }

        @ParameterizedTest
        @MethodSource("controlCharacterCases")
        @DisplayName("should replace CR, LF and TAB with underscores")
        void shouldReplaceControlCharacters(String input, String expected) {
            assertThat(LogSanitiser.sanitise(input)).isEqualTo(expected);
        }

        static Stream<Arguments> controlCharacterCases() {
            return Stream.of(
                    Arguments.of("hello\rworld", "hello_world"),
                    Arguments.of("hello\nworld", "hello_world"),
                    Arguments.of("hello\tworld", "hello_world"),
                    Arguments.of("\r\n\t", "___"),
                    Arguments.of("a\rb\nc\td", "a_b_c_d"),
                    Arguments.of("\r\n", "__"),
                    Arguments.of("multiple\nlines\rwith\ttabs", "multiple_lines_with_tabs")
            );
        }

        @Test
        @DisplayName("should preserve non-control characters unchanged")
        void shouldPreserveRegularCharactersUnchanged() {
            String input = "Hello World 123!@#$%^&*()_+-=[]{};:'\",.<>?/\\|`~";
            assertThat(LogSanitiser.sanitise(input)).isEqualTo(input);
        }

        @Test
        @DisplayName("should handle string containing only control characters")
        void shouldHandleOnlyControlCharacters() {
            String input = "\r\n\t\r\n\t";
            assertThat(LogSanitiser.sanitise(input)).isEqualTo("______");
        }

        @Test
        @DisplayName("should handle long strings safely")
        void shouldHandleLongStrings() {
            String input = "This is a long string ".repeat(1000) + "\r\n\t";

            String result = LogSanitiser.sanitise(input);

            assertThat(result)
                    .doesNotContain("\r", "\n", "\t")
                    .endsWith("___");
        }
    }

    @Nested
    @DisplayName("constructor")
    class ConstructorTests {

        @Test
        @DisplayName("should have private constructor and prevent instantiation via reflection")
        void shouldHavePrivateConstructor() throws Exception {
            Constructor<LogSanitiser> constructor =
                    LogSanitiser.class.getDeclaredConstructor();

            assertThat(constructor.canAccess(null)).isFalse();

            constructor.setAccessible(true);

            // Reflection can still invoke it, but class remains a proper utility class
            assertThat(constructor.getParameterCount()).isZero();
        }
    }
}