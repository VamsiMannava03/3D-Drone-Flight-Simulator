package dronesimulation.view.scene;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Collects many shapes into one mesh per colour, so a whole town becomes a handful of meshes
 * instead of thousands of scene nodes.
 */
public final class ColourBatch {

  private final Map<Integer, MeshBuilder> byColour = new LinkedHashMap<>();

  /** Creates an empty batch. */
  public ColourBatch() {
    // nothing collected yet
  }

  /**
   * Adds a shape in a colour.
   *
   * @param rgb the colour as 0xRRGGBB
   * @param vertices the shape's vertices, as {@link MeshBuilder#toArray()} gives
   * @param transform where to put it
   * @return this batch
   */
  public ColourBatch add(int rgb, float[] vertices, float[] transform) {
    builder(rgb).append(vertices, transform);
    return this;
  }

  /**
   * Returns the builder that collects one colour, to add shapes to it directly.
   *
   * @param rgb the colour as 0xRRGGBB
   * @return the builder
   */
  public MeshBuilder builder(int rgb) {
    return byColour.computeIfAbsent(rgb, c -> new MeshBuilder());
  }

  /**
   * Returns the collected meshes.
   *
   * @return a builder per colour, in the order colours were first used
   */
  public Map<Integer, MeshBuilder> meshes() {
    return Collections.unmodifiableMap(byColour);
  }
}
