import java.awt.AlphaComposite;
import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.geom.GeneralPath;
import java.awt.geom.Line2D;
import java.awt.geom.Rectangle2D;

/**
 * The school common room of shots 1 and 2: walls, plants, bench, stool,
 * clock, the big foreground table and everything sitting on it.
 *
 * All static - the room holds no state, it is redrawn from t every frame.
 */
final class Scene {

    private Scene() {
    }

    static final java.awt.Color[] CONFETTI = {
        Art.rgb("#FF8C42"), Art.rgb("#FFB347"), Art.rgb("#FFD97D"),
        Art.rgb("#FFF4D6"), Art.rgb("#7FD4E8")
    };

    /** Where the far edge of the big foreground table sits at a given x. */
    static double tableEdgeY(double x) {
        return 472 - x * 58.0 / Art.W;
    }

    /** The whole room behind the table. Shots 1 and 2 share this camera. */
    static void drawRoomScene(Graphics2D g, double clockT) {
        drawRoom(g);
        drawPlantShelf(g);
        drawWallClock(g, 300, 92, clockT);
        drawBench(g);
        drawStool(g);
        drawSunlight(g);
    }

    /** Warm wall, floor and skirting board. */
    static void drawRoom(Graphics2D g) {
        g.setColor(Art.S1_WALL);
        g.fillRect(0, 0, Art.W, 300);
        g.setColor(Art.S1_FLOOR);
        g.fillRect(0, 300, Art.W, Art.H - 300);

        g.setColor(Art.S1_WALL_D);
        g.fillRect(0, 292, Art.W, 10);
        g.setColor(Art.lerp(Art.S1_WALL_D, Art.S1_INK, 0.35));
        g.setStroke(new BasicStroke(2f));
        g.draw(new Line2D.Double(0, 292, Art.W, 292));

        // floorboards
        g.setColor(Art.lerp(Art.S1_FLOOR, Art.S1_WOOD, 0.3));
        g.setStroke(new BasicStroke(1.6f));
        for (int i = 1; i < 4; i++) {
            double y = 300 + i * 26;
            g.draw(new Line2D.Double(0, y, Art.W, y - 8));
        }
    }

    /** Shelf of potted plants on the left wall. The pot rims are ellipses. */
    static void drawPlantShelf(Graphics2D g) {
        g.setColor(Art.S1_WOOD);
        g.fill(new Rectangle2D.Double(12, 150, 180, 12));
        g.setColor(Art.S1_WOOD_D);
        g.setStroke(new BasicStroke(2f));
        g.draw(new Rectangle2D.Double(12, 150, 180, 12));
        g.draw(new Line2D.Double(40, 162, 44, 186));   // brackets
        g.draw(new Line2D.Double(164, 162, 160, 186));

        drawPottedPlant(g, 52, 150, 20);
        drawPottedPlant(g, 106, 150, 15);
        drawPottedPlant(g, 154, 150, 18);
    }

    static void drawPottedPlant(Graphics2D g, int cx, int baseY, int rx) {
        // leaves first, so the pot overlaps their stems
        g.setStroke(new BasicStroke(2.2f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
        for (int i = -1; i <= 1; i++) {
            double tipX = cx + i * rx * 1.5;
            double tipY = baseY - 34 - rx - Math.abs(i) * -8;
            GeneralPath leaf = new GeneralPath();
            leaf.moveTo(cx, baseY - 22);
            leaf.quadTo(cx + i * rx * 1.8, baseY - 34, tipX, tipY);
            leaf.quadTo(cx + i * rx * 0.4, baseY - 32, cx, baseY - 22);
            leaf.closePath();
            g.setColor(i == 0 ? Art.S1_LEAF : Art.lerp(Art.S1_LEAF, Art.S1_LEAF_D, 0.45));
            g.fill(leaf);
            g.setColor(Art.S1_LEAF_D);
            g.draw(leaf);
        }

        GeneralPath pot = new GeneralPath();
        pot.moveTo(cx - rx, baseY - 22);
        pot.lineTo(cx + rx, baseY - 22);
        pot.lineTo(cx + rx * 0.72, baseY);
        pot.lineTo(cx - rx * 0.72, baseY);
        pot.closePath();
        g.setColor(Art.S1_WOOD);
        g.fill(pot);
        g.setColor(Art.S1_WOOD_D);
        g.setStroke(new BasicStroke(2f));
        g.draw(pot);
        g.setColor(Art.lerp(Art.S1_WOOD, Color.WHITE, 0.3));
        Midpoint.fillEllipse(g, cx, baseY - 22, rx, 5);
        g.setColor(Art.S1_WOOD_D);
        Midpoint.ellipse(g, cx, baseY - 22, rx, 5);
    }

    /** Wooden bench against the right wall. */
    static void drawBench(Graphics2D g) {
        g.setColor(Art.S1_WOOD);
        g.fill(new Rectangle2D.Double(388, 226, 208, 16));
        g.setColor(Art.S1_WOOD_D);
        g.setStroke(new BasicStroke(2.2f));
        g.draw(new Rectangle2D.Double(388, 226, 208, 16));
        g.draw(new Line2D.Double(408, 242, 412, 300));
        g.draw(new Line2D.Double(572, 242, 568, 300));
        g.draw(new Line2D.Double(412, 274, 568, 274));
    }

    /** Round stool. The seat is a filled midpoint ellipse. */
    static void drawStool(Graphics2D g) {
        int cx = 520;
        int cy = 336;
        g.setColor(Art.S1_WOOD_D);
        g.setStroke(new BasicStroke(3f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
        g.draw(new Line2D.Double(cx - 26, cy + 6, cx - 34, cy + 64));
        g.draw(new Line2D.Double(cx + 26, cy + 6, cx + 34, cy + 64));
        g.draw(new Line2D.Double(cx, cy + 8, cx + 2, cy + 60));

        g.setColor(Art.S1_WOOD);
        Midpoint.fillEllipse(g, cx, cy, 42, 15);
        g.setColor(Art.S1_WOOD_D);
        Midpoint.ellipse(g, cx, cy, 42, 15);
    }

    /** Two soft shafts of afternoon light falling in from the right. */
    static void drawSunlight(Graphics2D g) {
        Graphics2D gg = (Graphics2D) g.create();
        gg.setColor(new Color(255, 246, 214));
        gg.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 0.42f));
        GeneralPath a = new GeneralPath();
        a.moveTo(600, 0);
        a.lineTo(600, 150);
        a.lineTo(300, 470);
        a.lineTo(180, 470);
        a.closePath();
        gg.fill(a);
        gg.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 0.28f));
        GeneralPath b = new GeneralPath();
        b.moveTo(600, 168);
        b.lineTo(600, 236);
        b.lineTo(392, 470);
        b.lineTo(318, 470);
        b.closePath();
        gg.fill(b);
        gg.dispose();
    }

    /** Wall clock. The rim is a hand written midpoint circle. */
    static void drawWallClock(Graphics2D g, int cx, int cy, double t) {
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

        // hour and minute hands sit still - only the seconds run
        g.setStroke(new BasicStroke(3f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
        drawHand(g, cx, cy, Math.toRadians(150), 13);
        g.setStroke(new BasicStroke(2.4f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
        drawHand(g, cx, cy, Math.toRadians(348), 21);

        // the second hand runs far too fast, and ticks in steps - pressure
        double revs = t / 2.5 * 3.0;
        double stepped = Math.floor(revs * 20.0) / 20.0;
        g.setColor(Art.rgb("#E24B4B"));
        g.setStroke(new BasicStroke(1.8f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
        drawHand(g, cx, cy, stepped * 2 * Math.PI, 26);

        g.setColor(Art.S1_INK);
        Midpoint.fillCircle(g, cx, cy, 2);
    }

    static void drawHand(Graphics2D g, int cx, int cy, double angle, double len) {
        g.draw(new Line2D.Double(
                cx - Math.sin(angle) * 5, cy + Math.cos(angle) * 5,
                cx + Math.sin(angle) * len, cy - Math.cos(angle) * len));
    }

    /** The big pale table across the foreground, seen slightly from above. */
    static void drawBigTable(Graphics2D g) {
        GeneralPath side = new GeneralPath();
        side.moveTo(0, tableEdgeY(0) - 13);
        side.lineTo(Art.W, tableEdgeY(Art.W) - 13);
        side.lineTo(Art.W, tableEdgeY(Art.W));
        side.lineTo(0, tableEdgeY(0));
        side.closePath();

        GeneralPath top = new GeneralPath();
        top.moveTo(0, tableEdgeY(0));
        top.lineTo(Art.W, tableEdgeY(Art.W));
        top.lineTo(Art.W, Art.H);
        top.lineTo(0, Art.H);
        top.closePath();

        g.setColor(Art.S1_TABLE_D);
        g.fill(side);
        g.setColor(Art.S1_TABLE);
        g.fill(top);
        g.setColor(Art.lerp(Art.S1_TABLE_D, Art.S1_INK, 0.3));
        g.setStroke(new BasicStroke(2.2f));
        g.draw(new Line2D.Double(0, tableEdgeY(0) - 13, Art.W, tableEdgeY(Art.W) - 13));
        g.draw(new Line2D.Double(0, tableEdgeY(0), Art.W, tableEdgeY(Art.W)));
    }

    /**
     * The right hand friend's hands, resting on top of the table. They are
     * drawn after the table so they land exactly where her forearms vanish
     * behind the edge.
     */
    static void drawTableHands(Graphics2D g, Color skin, Color ink) {
        drawTableHand(g, 450, 442, skin, ink);
        drawTableHand(g, 414, 438, skin, ink);
    }

    static void drawTableHand(Graphics2D g, double x, double y, Color skin, Color ink) {
        GeneralPath hand = new GeneralPath();
        hand.moveTo(x - 13, y - 11);
        hand.quadTo(x - 17, y + 6, x - 4, y + 10);
        hand.quadTo(x + 13, y + 12, x + 14, y - 1);
        hand.quadTo(x + 12, y - 13, x - 13, y - 11);
        hand.closePath();
        g.setColor(skin);
        g.fill(hand);
        g.setColor(ink);
        g.setStroke(new BasicStroke(2.2f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
        g.draw(hand);
        g.setStroke(new BasicStroke(1.3f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
        g.draw(new Line2D.Double(x - 2, y - 10, x - 1, y + 9));
        g.draw(new Line2D.Double(x + 6, y - 9, x + 7, y + 9));
    }

    /** Photo frame and pencil case sharing the table. */
    static void drawTableProps(Graphics2D g) {
        g.setColor(Art.lerp(Art.S1_WOOD, Color.WHITE, 0.25));
        g.fill(new Rectangle2D.Double(492, 470, 74, 58));
        g.setColor(Art.S1_WOOD_D);
        g.setStroke(new BasicStroke(2.4f));
        g.draw(new Rectangle2D.Double(492, 470, 74, 58));
        g.setColor(Art.lerp(Art.S1_CARDI, Color.WHITE, 0.4));
        g.fill(new Rectangle2D.Double(501, 479, 56, 40));
        g.setColor(Art.S1_WOOD_D);
        g.setStroke(new BasicStroke(1.6f));
        g.draw(new Rectangle2D.Double(501, 479, 56, 40));

        GeneralPath caseBody = new GeneralPath();
        caseBody.moveTo(70, 534);
        caseBody.quadTo(62, 560, 90, 564);
        caseBody.lineTo(160, 564);
        caseBody.quadTo(186, 558, 178, 534);
        caseBody.closePath();
        g.setColor(Art.lerp(Art.S1_CARDI, Art.S1_NAVY, 0.45));
        g.fill(caseBody);
        g.setColor(Art.S1_INK);
        g.setStroke(new BasicStroke(2.4f));
        g.draw(caseBody);
        g.setStroke(new BasicStroke(2.6f));
        g.draw(new Line2D.Double(70, 538, 178, 538));   // zip
        g.setStroke(new BasicStroke(2f));
        g.draw(new Line2D.Double(178, 538, 188, 544));  // zip pull
    }

    /** The tablet lying tilted on the table. */
    static void drawTablet(Graphics2D g, double flash) {
        GeneralPath body = new GeneralPath();
        body.moveTo(237, 481);
        body.lineTo(381, 462);
        body.lineTo(408, 532);
        body.lineTo(257, 557);
        body.closePath();

        GeneralPath screen = new GeneralPath();
        screen.moveTo(248, 489);
        screen.lineTo(371, 472);
        screen.lineTo(395, 525);
        screen.lineTo(267, 546);
        screen.closePath();

        g.setColor(Art.lerp(Art.S1_INK, Color.WHITE, 0.15));
        g.fill(body);
        g.setColor(Art.S1_INK);
        g.setStroke(new BasicStroke(2.6f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
        g.draw(body);

        Color dim = Art.lerp(Art.S1_GLOW, Color.WHITE, 0.55);
        Color bright = Art.lerp(Art.S1_GLOW, Color.WHITE, 0.05);
        g.setColor(Art.lerp(dim, bright, flash));
        g.fill(screen);

        // a couple of text lines on the result page, brightening on the beat
        g.setColor(Art.lerp(Art.lerp(Art.S1_GLOW, Art.S1_INK, 0.35), Color.WHITE, flash * 0.75));
        g.setStroke(new BasicStroke(5f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
        g.draw(new Line2D.Double(266, 501, 357, 488));
        g.draw(new Line2D.Double(266, 517, 335, 507));
        g.setStroke(new BasicStroke(3f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
        g.draw(new Line2D.Double(266, 532, 364, 519));
    }

    /** Soft halo on the table around the tablet, from filled ellipses. */
    static void drawScreenGlow(Graphics2D g, double flash) {
        Graphics2D gg = (Graphics2D) g.create();
        gg.setColor(Art.lerp(Art.S1_GLOW, Color.WHITE, 0.35));
        float base = (float) (0.05 + 0.10 * flash);
        for (int i = 4; i >= 1; i--) {
            gg.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, base));
            Midpoint.fillEllipse(gg, 322, 509, 74 + i * 22, 34 + i * 12);
        }
        gg.dispose();
    }

    /** Short rays around the tablet, thrown out the moment the result lands. */
    static void drawSparks(Graphics2D g, double t) {
        double u = Art.clamp((t - 2.0) / 0.25, 0, 1);
        if (u <= 0) {
            return;
        }
        double grow = Art.ease(u);
        g.setColor(Art.lerp(Art.S1_GLOW, Art.S1_INK, 0.25));
        g.setStroke(new BasicStroke(3.4f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
        for (int i = 0; i < 12; i++) {
            double a = Math.toRadians(i * 30 + 12);
            double r0 = 92 + 16 * grow;
            double r1 = r0 + 14 + 20 * grow;
            double sx = Math.cos(a);
            double sy = Math.sin(a) * 0.45; // squashed, the table is in perspective
            g.draw(new Line2D.Double(322 + sx * r0, 509 + sy * r0,
                    322 + sx * r1, 509 + sy * r1));
        }
    }

    /** Flat colour laid over the frame. */
    static void wash(Graphics2D g, Color c, float alpha) {
        Graphics2D gg = (Graphics2D) g.create();
        gg.setColor(c);
        gg.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, alpha));
        gg.fillRect(0, 0, Art.W, Art.H);
        gg.dispose();
    }

    /** A bounce that loses height, in pixels. */
    static double jump(double t, double delay) {
        double u = Math.max(0, t - delay);
        return Math.abs(Math.sin(u * Math.PI * 1.7)) * 34 * Math.exp(-u * 0.75);
    }

    /** Short radial dashes bursting out of a point. */
    static void joyLines(Graphics2D g, double cx, double cy, double t) {
        double u = (t % 0.7) / 0.7;
        g.setColor(Art.rgb("#FFF4D6"));
        g.setStroke(new BasicStroke(4.4f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
        for (int i = 0; i < 8; i++) {
            double a = Math.toRadians(i * 45 + 20);
            double r0 = 34 + 40 * u;
            double r1 = r0 + 18 * (1 - u);
            g.draw(new Line2D.Double(cx + Math.cos(a) * r0, cy + Math.sin(a) * r0,
                    cx + Math.cos(a) * r1, cy + Math.sin(a) * r1));
        }
    }

    /**
     * 25 pieces falling and spinning. Each piece is a filled midpoint ellipse
     * whose rx follows |cos| of its spin angle, so it flattens edge on and
     * fills out again exactly the way a tumbling disc does.
     */
    static void confetti(Graphics2D g, double t) {
        for (int i = 0; i < 25; i++) {
            double x = Art.rnd(i, 1) * Art.W;
            double fall = (Art.rnd(i, 2) * 0.6 + 0.7);
            double y = ((Art.rnd(i, 3) * Art.H) + t * 150 * fall) % (Art.H + 80) - 40;
            double spin = t * (3 + Art.rnd(i, 4) * 5) + Art.rnd(i, 5) * 6.3;
            int r = (int) (5 + Art.rnd(i, 6) * 4);
            int rx = (int) Math.max(1, Math.abs(Math.cos(spin)) * r);

            g.setColor(CONFETTI[i % CONFETTI.length]);
            Midpoint.fillEllipse(g, (int) x, (int) y, rx, r);
            g.setColor(Art.lerp(CONFETTI[i % CONFETTI.length], Art.S1_INK, 0.35));
            Midpoint.ellipse(g, (int) x, (int) y, rx, r);
        }
    }
}
