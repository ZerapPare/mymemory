import java.awt.BasicStroke;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.Stroke;
import java.awt.geom.Line2D;

public class Midpoint {

    public static void circle(Graphics2D g, int cx, int cy, int r) {
        g.drawOval(cx - r, cy - r, 2 * r, 2 * r);
    }

    public static void fillCircle(Graphics2D g, int cx, int cy, int r) {
        g.fillOval(cx - r, cy - r, 2 * r, 2 * r);
    }

    // ---- ยกมาจาก Midpoint.java ที่ root ตรงๆ ----
    // ปิด antialiasing ในทุกตัวเพื่อให้พิกเซลที่พล็อตออกมาตรงเป๊ะ และตัวที่ถม
    // จะเก็บ x ที่กว้างที่สุดของแต่ละ scan line ไว้ก่อน แล้วค่อยวาดทีละแถวครั้งเดียว
    // ไม่งั้นสีโปร่งแสงจะทับตัวเองซ้ำตรงที่สองวงซ้อนกัน

    public static void plot(Graphics2D g, int x, int y) {
        g.fillRect(x, y, 1, 1);
    }

    public static void plot4(Graphics2D g, int xc, int yc, int x, int y) {
        plot(g, xc + x, yc + y);
        plot(g, xc - x, yc + y);
        plot(g, xc + x, yc - y);
        plot(g, xc - x, yc - y);
    }

    /** Midpoint ellipse algorithm, region 1 then region 2. */
    public static void ellipse(Graphics2D g, int xc, int yc, int rx, int ry) {
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

    /** Solid version of the midpoint ellipse, one row at a time. */
    public static void fillEllipse(Graphics2D g, int xc, int yc, int rx, int ry) {
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
