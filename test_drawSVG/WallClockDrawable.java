import java.awt.*;
import java.awt.geom.Line2D;

public class WallClockDrawable implements Drawable {
    private final int cx, cy;
    private final double speed;  // รอบต่อวินาที (ปรับได้)

    public WallClockDrawable(int cx, int cy, double speed) {
        this.cx = cx;
        this.cy = cy;
        this.speed = speed;
    }

    @Override
    public void draw(Graphics2D g, double time) {
        double t = time * speed;
        drawWallClock(g, cx, cy, t);
    }

    private void drawWallClock(Graphics2D g, int cx, int cy, double t) {
        g.setColor(Art.lerp(Art.S1_WALL, Color.WHITE, 0.7));
        Midpoint.fillCircle(g, cx, cy, 34);
        g.setColor(Art.S1_INK);
        Midpoint.circle(g, cx, cy, 36);
        Midpoint.circle(g, cx, cy, 35);
        Midpoint.circle(g, cx, cy, 30);

        g.setStroke(new BasicStroke(2f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
        for (int i = 0; i < 12; i++) {
            double a = i * Math.PI / 6.0;
            double s = (i % 3 == 0) ? 21 : 24;
            g.draw(new Line2D.Double(
                    cx + Math.sin(a) * s, cy - Math.cos(a) * s,
                    cx + Math.sin(a) * 27, cy - Math.cos(a) * 27));
        }

        g.setStroke(new BasicStroke(3f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
        drawHand(g, cx, cy, Math.toRadians(150), 13);
        g.setStroke(new BasicStroke(2.4f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
        drawHand(g, cx, cy, Math.toRadians(348), 21);

        double revs = t / 2.5 * 3.0;
        double stepped = Math.floor(revs * 20.0) / 20.0;
        g.setColor(Art.rgb("#E24B4B"));
        g.setStroke(new BasicStroke(1.8f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
        drawHand(g, cx, cy, stepped * 2 * Math.PI, 26);

        g.setColor(Art.S1_INK);
        Midpoint.fillCircle(g, cx, cy, 2);
    }

    private void drawHand(Graphics2D g, int cx, int cy, double angle, double length) {
        double ex = cx + Math.sin(angle) * length;
        double ey = cy - Math.cos(angle) * length;
        g.draw(new Line2D.Double(cx, cy, ex, ey));
    }
}
