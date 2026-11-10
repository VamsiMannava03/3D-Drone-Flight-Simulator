package dronesimulation.view.panel;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

class DronePaletteTest {

  @Test
  void givesColoursInTheOrderDronesAreSeen() {
    DronePalette palette = new DronePalette();
    assertEquals(DronePalette.COLOURS.get(0), palette.colourOf("q1"));
    assertEquals(DronePalette.COLOURS.get(1), palette.colourOf("h1"));
    assertEquals(DronePalette.COLOURS.get(0), palette.colourOf("q1"), "keeps its colour");
    for (int i = 0; i < DronePalette.COLOURS.size() - 2; i++) {
      palette.colourOf("d" + i);
    }
    assertEquals(DronePalette.COLOURS.get(0), palette.colourOf("wraps"));
    palette.clear();
    assertEquals(DronePalette.COLOURS.get(0), palette.colourOf("h1"));
  }
}
