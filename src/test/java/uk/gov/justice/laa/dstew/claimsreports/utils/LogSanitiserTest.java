package uk.gov.justice.laa.dstew.claimsreports.utils;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.lang.reflect.Constructor;
import java.util.stream.Stream;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

/** Unit tests for {@link LogSanitiser}. */
@DisplayName("LogSanitiser")
class LogSanitiserTest {

  @Nested
  @DisplayName("sanitise")
  class SanitiseTests {

    @Test
    @DisplayName("should return null when input is null")
    void shouldReturnNullWhenInputIsNull() {
      assertThat(LogSanitiser.sanitise(null)).isNull();
    }

    @Test
    @DisplayName("should return empty string when input is empty")
    void shouldReturnEmptyStringWhenInputIsEmpty() {
      assertThat(LogSanitiser.sanitise("")).isEmpty();
    }

    @ParameterizedTest
    @MethodSource("controlCharacterCases")
    @DisplayName("should replace CR and LF with underscores (tabs unchanged)")
    void shouldReplaceControlCharacters(String input, String expected) {
      assertThat(LogSanitiser.sanitise(input)).isEqualTo(expected);
    }

    static Stream<Arguments> controlCharacterCases() {
      return Stream.of(
          Arguments.of("hello\rworld", "hello_world"),
          Arguments.of("hello\nworld", "hello_world"),
          Arguments.of("hello\tworld", "hello\tworld"),
          Arguments.of("\r\n", "__"),
          Arguments.of("a\rb\nc\td", "a_b_c\td"),
          Arguments.of("\r\n\t", "__\t"),
          Arguments.of("multiple\nlines\rwith\ttabs", "multiple_lines_with\ttabs"));
    }

    @Test
    @DisplayName("should preserve non-control characters unchanged")
    void shouldPreserveRegularCharactersUnchanged() {
      String input = "Hello World 123!@#$%^&*()_+-=[]{};:'\",.<>?/\\|`~";
      assertThat(LogSanitiser.sanitise(input)).isEqualTo(input);
    }

    @Test
    @DisplayName("should handle long strings safely")
    void shouldHandleLongStrings() {
      String input = "This is a long string ".repeat(1000) + "\r\n";

      String result = LogSanitiser.sanitise(input);

      assertThat(result).doesNotContain("\r", "\n").endsWith("__");
    }
  }

  @Nested
  @DisplayName("sanitiseForFilename")
  class SanitiseForFilenameTests {

    @Test
    @DisplayName("should throw exception when input is null")
    void shouldThrowWhenInputIsNull() {
      assertThatThrownBy(() -> LogSanitiser.sanitiseForFilename(null))
          .isInstanceOf(IllegalArgumentException.class)
          .hasMessage("Filename component cannot be null");
    }

    @Test
    @DisplayName("should remove invalid filename characters")
    void shouldRemoveInvalidCharacters() {
      String input = "file name @#$%.txt";
      assertThat(LogSanitiser.sanitiseForFilename(input)).isEqualTo("filename.txt");
    }

    @Test
    @DisplayName("should preserve valid filename characters")
    void shouldPreserveValidCharacters() {
      String input = "file_name-123.txt";
      assertThat(LogSanitiser.sanitiseForFilename(input)).isEqualTo("file_name-123.txt");
    }

    @Test
    @DisplayName("should throw exception when result is empty after sanitisation")
    void shouldThrowWhenResultEmpty() {
      String input = "@#$%^&*";

      assertThatThrownBy(() -> LogSanitiser.sanitiseForFilename(input))
          .isInstanceOf(IllegalArgumentException.class)
          .hasMessage("Filename component invalid after sanitisation");
    }
  }

  @Nested
  @DisplayName("constructor")
  class ConstructorTests {

    @Test
    @DisplayName("should prevent instantiation via reflection")
    void shouldHavePrivateConstructor() throws ReflectiveOperationException {
      Constructor<LogSanitiser> constructor = LogSanitiser.class.getDeclaredConstructor();

      assertThat(constructor.canAccess(null)).isFalse();

      constructor.setAccessible(true);

      LogSanitiser instance = constructor.newInstance();
      assertThat(instance).isNotNull();
    }
  }
}
