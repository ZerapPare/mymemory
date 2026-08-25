import java.awt.Color;

public class Art {
    public static final Color S1_WALL = new Color(0x8B7355); // สีน้ำตาล
    public static final Color S1_INK  = Color.BLACK;

    public static Color lerp(Color c1, Color c2, double t) {
        int r = (int)(c1.getRed()   + (c2.getRed()   - c1.getRed())   * t);
        int g = (int)(c1.getGreen() + (c2.getGreen() - c1.getGreen()) * t);
        int b = (int)(c1.getBlue()  + (c2.getBlue()  - c1.getBlue())  * t);
        return new Color(r, g, b);
    }

    public static Color rgb(String hex) {
        return Color.decode(hex);
    }
}
