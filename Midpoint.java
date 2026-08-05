import java.awt.BasicStroke;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.Stroke;
import java.awt.geom.Line2D;

/**
 * Hand written midpoint rasterisers. The assignment requires these, and
 * drawOval / fillOval / drawArc appear nowhere in the project.
 *
 * Antialiasing is switched off inside every routine so the plotted pixels
 * stay exact, and the filled versions collect the widest x per scan line
 * first, then draw each row once - otherwise a translucent fill doubles up
 * on itself where the two ellipse regions meet.
 */
final class Midpoint {

    private Midpoint() {
    }


    static void plot(Graphics2D g, int x, int y) {
        g.fillRect(x, y, 1, 1);
    }

    /** Midpoint circle algorithm, eight way symmetry. */
    static void circle(Graphics2D g, int xc, int yc, int r) {
        if (r < 0) {
            return;
        }
        Object aa = g.getRenderingHint(RenderingHints.KEY_ANTIALIASING);
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_OFF);
        int x = 0;
        int y = r;
        int d = 1 - r;
        while (x <= y) {
            plot(g, xc + x, yc + y);
            plot(g, xc - x, yc + y);
            plot(g, xc + x, yc - y);
            plot(g, xc - x, yc - y);
            plot(g, xc + y, yc + x);
            plot(g, xc - y, yc + x);
            plot(g, xc + y, yc - x);
            plot(g, xc - y, yc - x);
            if (d < 0) {
                d += 2 * x + 3;
            } else {
                d += 2 * (x - y) + 5;
                y--;
            }
            x++;
        }
        if (aa != null) {
            g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, aa);
        }
    }

    /**
     * Midpoint circle drawn only part of the way round, starting at the top
     * and running clockwise. Used by shot 3 for the progressive tick mark.
     */
    static void arc(Graphics2D g, int xc, int yc, int r, double frac) {
        if (r < 0) {
            return;
        }
        Object aa = g.getRenderingHint(RenderingHints.KEY_ANTIALIASING);
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_OFF);

        int cap = 2 * r + 8;
        int[] px = new int[cap];
        int[] py = new int[cap];
        int n = 0;
        int x = 0;
        int y = r;
        int d = 1 - r;
        while (x <= y && n < cap) {
            px[n] = x;
            py[n] = y;
            n++;
            if (d < 0) {
                d += 2 * x + 3;
            } else {
                d += 2 * (x - y) + 5;
                y--;
            }
            x++;
        }

        int total = n * 8;
        int count = (int) Math.round(Art.clamp(frac, 0, 1) * total);
        for (int i = 0; i < count; i++) {
            int oct = i / n;
            int k = i % n;
            int j = n - 1 - k; // reversed order for every other octant
            switch (oct) {
                case 0: plot(g, xc + px[k], yc - py[k]); break;
                case 1: plot(g, xc + py[j], yc - px[j]); break;
                case 2: plot(g, xc + py[k], yc + px[k]); break;
                case 3: plot(g, xc + px[j], yc + py[j]); break;
                case 4: plot(g, xc - px[k], yc + py[k]); break;
                case 5: plot(g, xc - py[j], yc + px[j]); break;
                case 6: plot(g, xc - py[k], yc - px[k]); break;
                default: plot(g, xc - px[j], yc - py[j]); break;
            }
        }
        if (aa != null) {
            g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, aa);
        }
    }

    /** Midpoint ellipse algorithm, region 1 then region 2. */
    static void ellipse(Graphics2D g, int xc, int yc, int rx, int ry) {
        if (rx < 0 || ry < 0) {
            return;
        }
        Object aa = g.getRenderingHint(RenderingHints.KEY_ANTIALIASING);
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_OFF);

        double rx2 = (double) rx * rx;
        double ry2 = (double) ry * ry;
        double x = 0;
        double y = ry;
        double dx = 0;
        double dy = 2 * rx2 * y;
        double p = ry2 - rx2 * ry + 0.25 * rx2;

        while (dx < dy) {
            plot4(g, xc, yc, (int) x, (int) y);
            x++;
            dx += 2 * ry2;
            if (p < 0) {
                p += ry2 + dx;
            } else {
                y--;
                dy -= 2 * rx2;
                p += ry2 + dx - dy;
            }
        }

        p = ry2 * (x + 0.5) * (x + 0.5) + rx2 * (y - 1) * (y - 1) - rx2 * ry2;
        while (y >= 0) {
            plot4(g, xc, yc, (int) x, (int) y);
            y--;
            dy -= 2 * rx2;
            if (p > 0) {
                p += rx2 - dy;
            } else {
                x++;
                dx += 2 * ry2;
                p += rx2 - dy + dx;
            }
        }
        if (aa != null) {
            g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, aa);
        }
    }

    static void plot4(Graphics2D g, int xc, int yc, int x, int y) {
        plot(g, xc + x, yc + y);
        plot(g, xc - x, yc + y);
        plot(g, xc + x, yc - y);
        plot(g, xc - x, yc - y);
    }

    /**
     * Solid version of the midpoint circle: the widest x of every scan line is
     * collected first, then each row is drawn exactly once with drawLine so a
     * translucent fill never doubles up on itself.
     */
    static void fillCircle(Graphics2D g, int xc, int yc, int r) {
        if (r < 0) {
            return;
        }
        int[] maxX = newSpan(r);
        int x = 0;
        int y = r;
        int d = 1 - r;
        while (x <= y) {
            span(maxX, y, x);
            span(maxX, x, y);
            if (d < 0) {
                d += 2 * x + 3;
            } else {
                d += 2 * (x - y) + 5;
                y--;
            }
            x++;
        }
        drawSpans(g, xc, yc, maxX, r);
    }

    /** Solid version of the midpoint ellipse, same one row at a time approach. */
    static void fillEllipse(Graphics2D g, int xc, int yc, int rx, int ry) {
        if (rx < 0 || ry < 0) {
            return;
        }
        int[] maxX = newSpan(ry);
        double rx2 = (double) rx * rx;
        double ry2 = (double) ry * ry;
        double x = 0;
        double y = ry;
        double dx = 0;
        double dy = 2 * rx2 * y;
        double p = ry2 - rx2 * ry + 0.25 * rx2;

        while (dx < dy) {
            span(maxX, (int) y, (int) x);
            x++;
            dx += 2 * ry2;
            if (p < 0) {
                p += ry2 + dx;
            } else {
                y--;
                dy -= 2 * rx2;
                p += ry2 + dx - dy;
            }
        }
        p = ry2 * (x + 0.5) * (x + 0.5) + rx2 * (y - 1) * (y - 1) - rx2 * ry2;
        while (y >= 0) {
            span(maxX, (int) y, (int) x);
            y--;
            dy -= 2 * rx2;
            if (p > 0) {
                p += rx2 - dy;
            } else {
                x++;
                dx += 2 * ry2;
                p += rx2 - dy + dx;
            }
        }
        drawSpans(g, xc, yc, maxX, ry);
    }

    static int[] newSpan(int rows) {
        int[] a = new int[rows + 1];
        for (int i = 0; i <= rows; i++) {
            a[i] = -1;
        }
        return a;
    }

    static void span(int[] maxX, int row, int x) {
        if (row >= 0 && row < maxX.length && x > maxX[row]) {
            maxX[row] = x;
        }
    }

    static void drawSpans(Graphics2D g, int xc, int yc, int[] maxX, int rows) {
        Object aa = g.getRenderingHint(RenderingHints.KEY_ANTIALIASING);
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_OFF);
        Stroke old = g.getStroke();
        g.setStroke(new BasicStroke(1f));
        int carry = 0;
        for (int dy = rows; dy >= 0; dy--) {
            int x = maxX[dy] >= 0 ? maxX[dy] : carry;
            carry = x;
            g.draw(new Line2D.Double(xc - x, yc - dy, xc + x, yc - dy));
            if (dy > 0) {
                g.draw(new Line2D.Double(xc - x, yc + dy, xc + x, yc + dy));
            }
        }
        g.setStroke(old);
        if (aa != null) {
            g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, aa);
        }
    }

}
