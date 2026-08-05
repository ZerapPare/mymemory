import java.awt.AlphaComposite;
import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics2D;
import java.awt.geom.GeneralPath;
import java.awt.geom.Line2D;
import java.awt.geom.Rectangle2D;

/**
 * Everywhere that is not the common room: the result page of shot 3, the
 * lecture hall of shot 4, the stage of shot 5, and the rain cloud and laptop
 * of shot 6.
 */
final class Hall {

    private Hall() {
    }

    /** What the tablet says, once the camera is inside it. */
    static void drawResultPage(Graphics2D g, double t) {
        g.setColor(Art.S3_BG);
        g.fillRect(0, 0, Art.W, Art.H);
        g.setColor(Color.WHITE);
        g.fill(new Rectangle2D.Double(46, 60, 508, 480));
        g.setColor(Art.lerp(Art.S3_BG, Art.S1_INK, 0.35));
        g.setStroke(new BasicStroke(3f));
        g.draw(new Rectangle2D.Double(46, 60, 508, 480));

        // the tick is traced on, dot by dot, rather than just appearing
        double ring = Art.clamp((t - 0.95) / 0.45, 0, 1);
        double mark = Art.clamp((t - 1.30) / 0.3, 0, 1);
        g.setColor(Art.rgb("#3DDC84"));
        for (int r = 62; r <= 66; r++) {
            Midpoint.arc(g, 300, 196, r, ring);
        }
        if (mark > 0) {
            g.setStroke(new BasicStroke(11f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
            double m1 = Math.min(mark / 0.4, 1);
            g.draw(new Line2D.Double(272, 196, 272 + 20 * m1, 196 + 22 * m1));
            if (mark > 0.4) {
                double m2 = (mark - 0.4) / 0.6;
                g.draw(new Line2D.Double(292, 218, 292 + 40 * m2, 218 - 48 * m2));
            }
        }

        g.setColor(Art.rgb("#1E2433"));
        g.setFont(new Font("Tahoma", Font.BOLD, 44));
        centred(g, "ผ่านการคัดเลือก", 358);
        g.setFont(new Font("Tahoma", Font.BOLD, 40));
        g.setColor(Art.rgb("#3C4A6B"));
        centred(g, "Computer Science", 430);
    }

    static void centred(Graphics2D g, String s, int y) {
        int w = g.getFontMetrics().stringWidth(s);
        g.drawString(s, (Art.W - w) / 2, y);
    }

    /** One row of the audience. The heads are midpoint circles. */
    static void audienceRow(Graphics2D g, double y, double hr, double gap, int row) {
        double seatH = hr * 2.6;
        for (int i = -3; i <= 3; i++) {
            double x = 300 + i * gap;
            if (x < -60 || x > Art.W + 60) {
                continue;
            }
            boolean hero = (row == 1 && i == 0);

            g.setColor(Art.rgb("#6B5B4F"));
            g.fill(new Rectangle2D.Double(x - hr * 1.5, y, hr * 3, seatH));
            g.setColor(Art.lerp(Art.rgb("#6B5B4F"), Art.S1_INK, 0.6));
            g.setStroke(new BasicStroke(2f));
            g.draw(new Rectangle2D.Double(x - hr * 1.5, y, hr * 3, seatH));

            // shoulders, then the back of a head
            g.setColor(hero ? Color.WHITE : Art.rgb("#DAD4C6"));
            GeneralPath sh = new GeneralPath();
            sh.moveTo(x - hr * 1.6, y + 4);
            sh.quadTo(x, y - hr * 0.9, x + hr * 1.6, y + 4);
            sh.lineTo(x + hr * 1.6, y + 12);
            sh.lineTo(x - hr * 1.6, y + 12);
            sh.closePath();
            g.fill(sh);
            g.setColor(Art.lerp(Art.S4_BG, Art.S1_INK, 0.8));
            g.draw(sh);

            g.setColor(Art.lerp(Art.S4_BG, Art.S1_INK, 0.85));
            Midpoint.fillCircle(g, (int) x, (int) (y - hr * 0.7), (int) hr + 1);
            g.setColor(hero ? Art.rgb("#4A3B33") : Art.rgb("#5E5145"));
            Midpoint.fillCircle(g, (int) x, (int) (y - hr * 0.7), (int) hr);
        }
    }

    /** Translucent cone of light from the ceiling. */
    static void spotlight(Graphics2D g, double x, double bottomY) {
        Graphics2D gg = (Graphics2D) g.create();
        gg.setColor(Art.rgb("#FFF3D0"));
        gg.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 0.5f));
        GeneralPath cone = new GeneralPath();
        cone.moveTo(x - 14, 0);
        cone.lineTo(x + 14, 0);
        cone.lineTo(x + 92, bottomY);
        cone.lineTo(x - 92, bottomY);
        cone.closePath();
        gg.fill(cone);
        gg.dispose();
    }

    static void drawStage(Graphics2D g, double t) {
        g.setColor(Art.rgb("#8A8578"));
        g.fillRect(-200, 430, Art.W + 400, 200);
        g.setColor(Art.rgb("#3D3A34"));
        g.setStroke(new BasicStroke(3f));
        g.draw(new Line2D.Double(-200, 430, Art.W + 400, 430));

        // projector screen, kept to the right so the bubble has its own space
        g.setColor(Art.rgb("#EFEADF"));
        g.fill(new Rectangle2D.Double(272, 196, 302, 206));
        g.setColor(Art.rgb("#3D3A34"));
        g.setStroke(new BasicStroke(4f));
        g.draw(new Rectangle2D.Double(272, 196, 302, 206));
        g.setStroke(new BasicStroke(2.5f));
        g.draw(new Line2D.Double(288, 382, 558, 382));

        // five bars climbing, the last one red and tallest
        double grow = Art.ease(Art.clamp(t / 1.4, 0, 1));
        double[] h = { 38, 58, 80, 104, 156 };
        for (int i = 0; i < h.length; i++) {
            double bh = h[i] * grow;
            double x = 296 + i * 52;
            g.setColor(i == 4 ? Art.rgb("#E24B4B") : Art.rgb("#8A8578"));
            g.fill(new Rectangle2D.Double(x, 382 - bh, 36, bh));
            g.setColor(Art.rgb("#3D3A34"));
            g.setStroke(new BasicStroke(2f));
            g.draw(new Rectangle2D.Double(x, 382 - bh, 36, bh));
        }

        // the lecturer, standing to the left of the screen
        GeneralPath body = new GeneralPath();
        body.moveTo(96, 430);
        body.lineTo(104, 372);
        body.quadTo(132, 356, 160, 372);
        body.lineTo(168, 430);
        body.closePath();
        g.setColor(Art.rgb("#EFEADF"));
        g.fill(body);
        g.setColor(Art.rgb("#3D3A34"));
        g.setStroke(new BasicStroke(2.5f));
        g.draw(body);
        // an arm pointing across at the chart
        g.setStroke(new BasicStroke(9f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
        g.draw(new Line2D.Double(156, 382, 232, 350));

        g.setColor(Art.rgb("#3D3A34"));
        Midpoint.fillCircle(g, 132, 330, 29);
        g.setColor(Art.rgb("#BFB9AA"));
        Midpoint.fillCircle(g, 132, 330, 26);
    }

    /** Rounded box with a tail, filling in a line at a time. */
    static void speechBubble(Graphics2D g, double t) {
        double show = Art.clamp((t - 0.4) / 0.2, 0, 1);
        if (show <= 0) {
            return;
        }
        Graphics2D gg = (Graphics2D) g.create();
        gg.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, (float) show));

        // tail reaching down to the lecturer's head
        GeneralPath tail = new GeneralPath();
        tail.moveTo(148, 186);
        tail.lineTo(120, 250);
        tail.lineTo(196, 188);
        tail.closePath();
        gg.setColor(Color.WHITE);
        gg.fill(new Rectangle2D.Double(52, 34, 428, 154));
        gg.fill(tail);
        gg.setColor(Art.rgb("#3D3A34"));
        gg.setStroke(new BasicStroke(3f));
        gg.draw(new Rectangle2D.Double(52, 34, 428, 154));
        gg.draw(tail);

        gg.setColor(Art.rgb("#3D3A34"));
        gg.setFont(new Font("Tahoma", Font.PLAIN, 27));
        if (t > 0.7) {
            gg.drawString("สาขาที่บัณฑิต", 76, 82);
            gg.drawString("ตกงานมากที่สุด...", 76, 120);
        }
        if (t > 1.6) {
            gg.setColor(Art.rgb("#E24B4B"));
            gg.setFont(new Font("Tahoma", Font.BOLD, 31));
            gg.drawString("Computer Science", 76, 166);
        }
        gg.dispose();
    }

    /** Three midpoint ellipses and a curtain of short rain strokes. */
    static void rainCloud(Graphics2D g, double t, double alive) {
        if (alive <= 0.01) {
            return;
        }
        Graphics2D gg = (Graphics2D) g.create();
        gg.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, (float) alive));
        int cy = 118;
        gg.setColor(Art.rgb("#8A8F99"));
        Midpoint.fillEllipse(gg, 246, cy + 8, 52, 30);
        Midpoint.fillEllipse(gg, 316, cy - 8, 64, 40);
        Midpoint.fillEllipse(gg, 384, cy + 6, 50, 28);
        gg.setColor(Art.lerp(Art.rgb("#8A8F99"), Art.S1_INK, 0.4));
        Midpoint.ellipse(gg, 246, cy + 8, 52, 30);
        Midpoint.ellipse(gg, 316, cy - 8, 64, 40);
        Midpoint.ellipse(gg, 384, cy + 6, 50, 28);

        gg.setStroke(new BasicStroke(2.6f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
        gg.setColor(Art.rgb("#8A8F99"));
        for (int i = 0; i < 16; i++) {
            double x = 214 + Art.rnd(i, 11) * 190;
            double y = 160 + ((Art.rnd(i, 12) * 120) + t * 230) % 130;
            gg.draw(new Line2D.Double(x, y, x - 3, y + 17));
        }
        gg.dispose();
    }

    /** Light from the laptop he has just opened, on his face again. */
    static void laptopGlow(Graphics2D g, double lift) {
        Graphics2D gg = (Graphics2D) g.create();
        gg.setColor(Art.S1_GLOW);
        gg.setComposite(AlphaComposite.getInstance(
                AlphaComposite.SRC_OVER, (float) (0.3 * lift)));
        GeneralPath cone = new GeneralPath();
        cone.moveTo(232, 560);
        cone.lineTo(368, 560);
        cone.lineTo(430, 300);
        cone.lineTo(170, 300);
        cone.closePath();
        gg.fill(cone);
        gg.dispose();

        // the open lid, bottom of frame
        g.setColor(Art.lerp(Art.S6_BG, Art.rgb("#2A2D33"), 0.65));
        GeneralPath lid = new GeneralPath();
        lid.moveTo(196, 600);
        lid.lineTo(226, 512);
        lid.lineTo(374, 512);
        lid.lineTo(404, 600);
        lid.closePath();
        g.fill(lid);
        g.setColor(Art.lerp(Art.S1_GLOW, Color.WHITE, 0.4));
        g.fill(new Rectangle2D.Double(236, 522, 128, 78));
    }
}
