import java.awt.Color;

/**
 * Palette and colour maths for MY MEMORIES.
 *
 * Everything here is static. Classes in the default package cannot be
 * imported, so call sites read Art.S1_WALL, Art.lerp(...) and so on - the
 * class name is deliberately short to keep those readable.
 */
final class Art {

    private Art() {
    }

    /** Canvas size. Every shot draws into this box. */
    static final int W = 600;
    static final int H = 600;

    // Shot 1: a school common room in warm afternoon light. The values stay
    // soft and creamy on purpose, so the saturated orange of shot 2 is still
    // a jump even though shot 1 is no longer cold.
    static final Color S1_WALL = rgb("#F3E7D5");
    static final Color S1_WALL_D = rgb("#E2D0B4");
    static final Color S1_FLOOR = rgb("#E7D6BC");
    static final Color S1_TABLE = rgb("#FBF5EA");
    static final Color S1_TABLE_D = rgb("#E4D6C0");
    static final Color S1_WOOD = rgb("#C89A63");
    static final Color S1_WOOD_D = rgb("#A87B46");
    static final Color S1_LEAF = rgb("#84A96A");
    static final Color S1_LEAF_D = rgb("#5F8250");
    static final Color S1_INK = rgb("#6B5B4F");
    static final Color S1_SKIN = rgb("#F6DBC2");
    static final Color S1_HAIR = rgb("#4A3B33");
    static final Color S1_SHIRT = rgb("#FFFFFF");
    static final Color S1_NAVY = rgb("#3C4A6B");
    static final Color S1_CARDI = rgb("#BCD5E8");
    /** Screen light. Shot 6 uses the same value on purpose - the story rhymes. */
    static final Color S1_GLOW = rgb("#7FD4E8");

    static final Color S2_BG = rgb("#FF8C42");
    static final Color S3_BG = rgb("#EAF6FF");
    static final Color S4_BG = rgb("#E8E4D9");
    static final Color S5_BG = rgb("#BFB9AA");
    static final Color S6_BG = rgb("#6E7076");

    static final Color RED = rgb("#E24B4B");

    static Color rgb(String hex) {
        return new Color(Integer.parseInt(hex.substring(1), 16));
    }

    static double clamp(double v, double lo, double hi) {
        return v < lo ? lo : (v > hi ? hi : v);
    }

    /** smoothstep */
    static double ease(double u) {
        u = clamp(u, 0, 1);
        return u * u * (3 - 2 * u);
    }

    static Color lerp(Color a, Color b, double u) {
        u = clamp(u, 0, 1);
        return new Color(
                (int) (a.getRed() + (b.getRed() - a.getRed()) * u),
                (int) (a.getGreen() + (b.getGreen() - a.getGreen()) * u),
                (int) (a.getBlue() + (b.getBlue() - a.getBlue()) * u));
    }

    static Color desat(Color c, double u) {
        int g = (int) (0.299 * c.getRed() + 0.587 * c.getGreen() + 0.114 * c.getBlue());
        return lerp(c, new Color(g, g, g), u);
    }

    /** Deterministic pseudo random in [0,1) - the same every run. */
    static double rnd(int i, int salt) {
        double v = Math.sin(i * 12.9898 + salt * 78.233) * 43758.5453;
        return v - Math.floor(v);
    }
}
