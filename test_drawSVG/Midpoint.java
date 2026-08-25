import java.awt.Graphics2D;

public class Midpoint {
    public static void circle(Graphics2D g, int cx, int cy, int r) {
        g.drawOval(cx - r, cy - r, 2 * r, 2 * r);
    }
    public static void fillCircle(Graphics2D g, int cx, int cy, int r) {
        g.fillOval(cx - r, cy - r, 2 * r, 2 * r);
    }
}