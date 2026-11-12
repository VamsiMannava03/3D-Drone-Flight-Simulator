package dronesimulation.view.scene;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import javafx.scene.image.WritableImage;
import javafx.scene.paint.Color;
import javafx.scene.paint.PhongMaterial;
import javafx.scene.shape.CullFace;
import javafx.scene.shape.MeshView;
import javafx.scene.shape.TriangleMesh;
import javafx.scene.shape.VertexFormat;
import javafx.scene.transform.Affine;

/**
 * Turns the plain mesh data of {@link MeshBuilder} and the matrices of {@link Mat4} into
 * JavaFX shapes and transforms.
 */
final class FxMeshes {

  private FxMeshes() {
    // static helpers only
  }

  /**
   * Converts vertex data into a JavaFX mesh with per-vertex normals.
   *
   * @param data interleaved position and normal floats
   * @return the mesh
   */
  static TriangleMesh mesh(float[] data) {
    TriangleMesh mesh = new TriangleMesh(VertexFormat.POINT_NORMAL_TEXCOORD);
    int n = data.length / 6;
    float[] points = new float[n * 3];
    float[] normals = new float[n * 3];
    int[] faces = new int[n * 3];
    for (int v = 0; v < n; v++) {
      System.arraycopy(data, v * 6, points, v * 3, 3);
      System.arraycopy(data, v * 6 + 3, normals, v * 3, 3);
      faces[v * 3] = v;
      faces[v * 3 + 1] = v;
      faces[v * 3 + 2] = 0;
    }
    mesh.getPoints().setAll(points);
    mesh.getNormals().setAll(normals);
    mesh.getTexCoords().setAll(0, 0);
    mesh.getFaces().setAll(faces);
    return mesh;
  }

  /**
   * Replaces a mesh's contents, for shapes rebuilt every frame.
   *
   * @param mesh the mesh to change
   * @param data interleaved position and normal floats
   */
  static void update(TriangleMesh mesh, float[] data) {
    TriangleMesh fresh = mesh(data);
    mesh.getPoints().setAll(fresh.getPoints());
    mesh.getNormals().setAll(fresh.getNormals());
    mesh.getTexCoords().setAll(fresh.getTexCoords());
    mesh.getFaces().setAll(fresh.getFaces());
  }

  /**
   * Makes a lit shape.
   *
   * @param data interleaved position and normal floats
   * @param colour its colour
   * @return the shape
   */
  static MeshView view(float[] data, Color colour) {
    MeshView view = new MeshView(mesh(data));
    view.setMaterial(material(colour));
    view.setCullFace(CullFace.NONE);
    return view;
  }

  /**
   * Makes one shape per colour of a batch.
   *
   * @param batch the batch
   * @return the shapes
   */
  static List<MeshView> views(ColourBatch batch) {
    List<MeshView> views = new ArrayList<>();
    for (Map.Entry<Integer, MeshBuilder> e : batch.meshes().entrySet()) {
      if (e.getValue().vertexCount() > 0) {
        views.add(view(e.getValue().toArray(), rgb(e.getKey())));
      }
    }
    return views;
  }

  /**
   * Makes a matt material.
   *
   * @param colour the colour, which may be see-through
   * @return the material
   */
  static PhongMaterial material(Color colour) {
    PhongMaterial m = new PhongMaterial(colour);
    m.setSpecularColor(Color.gray(0.12));
    return m;
  }

  /**
   * Makes a material that glows in its own colour, unaffected by light, for lines and
   * markers.
   *
   * @param colour the colour, which may be see-through
   * @return the material
   */
  static PhongMaterial glow(Color colour) {
    PhongMaterial m = new PhongMaterial(colour);
    WritableImage pixel = new WritableImage(1, 1);
    pixel.getPixelWriter().setColor(0, 0, colour);
    m.setSelfIlluminationMap(pixel);
    m.setSpecularColor(Color.TRANSPARENT);
    return m;
  }

  /**
   * Converts a {@link Mat4} matrix into a JavaFX transform.
   *
   * @param m the column-major matrix
   * @return the transform
   */
  static Affine affine(float[] m) {
    return new Affine(m[0], m[4], m[8], m[12], m[1], m[5], m[9], m[13], m[2], m[6], m[10],
        m[14]);
  }

  /**
   * Sets a transform to a {@link Mat4} matrix.
   *
   * @param target the transform to change
   * @param m the column-major matrix
   */
  static void set(Affine target, float[] m) {
    target.setToTransform(m[0], m[4], m[8], m[12], m[1], m[5], m[9], m[13], m[2], m[6], m[10],
        m[14]);
  }

  /**
   * Converts 0xRRGGBB into a colour.
   *
   * @param rgb the colour
   * @return the colour
   */
  static Color rgb(int rgb) {
    return Color.rgb((rgb >> 16) & 0xFF, (rgb >> 8) & 0xFF, rgb & 0xFF);
  }

  /**
   * Converts 0xRRGGBB and an opacity into a colour.
   *
   * @param rgb the colour
   * @param alpha the opacity, 0 to 1
   * @return the colour
   */
  static Color rgb(int rgb, double alpha) {
    return Color.rgb((rgb >> 16) & 0xFF, (rgb >> 8) & 0xFF, rgb & 0xFF, alpha);
  }
}
