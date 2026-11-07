import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

/**
 * Tests for {@link Main}.
 */
public class MainTest {

  @Test
  public void testGreeting() {
    assertEquals("Hello, World!", Main.greeting());
  }
}
