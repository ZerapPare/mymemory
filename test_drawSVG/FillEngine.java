import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.Shape;
import java.awt.geom.AffineTransform;
import java.awt.geom.Path2D;
import java.awt.geom.Point2D;
import java.awt.image.BufferedImage;
import java.awt.image.DataBufferInt;
import java.util.Arrays;

/**
 * Flood Fill / Rasterization Engine
 *
 * รับ Path2D แล้วคืนภาพลายเส้นที่ลงสีเสร็จแล้ว (ยังไม่รวม Drawable)
 *
 * ที่ต้องเรนเดอร์ลง BufferedImage เพราะ flood fill ต้อง "อ่าน" พิกเซลกลับมา
 * ซึ่ง Graphics2D ของ panel ทำไม่ได้ - ตัวนี้ไม่รู้จัก Swing รู้แค่พิกเซล
 */
public final class FillEngine {

    private FillEngine() {
    }

    /**
     * วาดเฉพาะพื้นหลัง (SVG + flood fill) ไม่รวม Drawable
     *
     * รับ seed กับเส้นอุดเข้ามาเป็นพารามิเตอร์ ไม่ไปหยิบจาก ArtConfig เอง
     * แต่ละซีนจึงส่งชุดของตัวเองมาได้ และไม่มีการอ้าง seed ด้วย index อีก
     * (เดิมเช็ค frameIndex < SCREEN_PER_FRAME.length ซึ่งถ้าลืมเพิ่มจะเงียบ)
     */
    public static BufferedImage rasteriseBackground(Path2D path, ArtConfig.Seed[] seeds,
            double[][] dams, AffineTransform at, int w, int h, boolean colorOn) {

        BufferedImage img = new BufferedImage(w, h, BufferedImage.TYPE_INT_RGB);
        Graphics2D g = img.createGraphics();
        g.setColor(Color.WHITE);
        g.fillRect(0, 0, w, h);

        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_OFF);
        g.setStroke(new BasicStroke(1f));
        g.setColor(Color.BLACK);

        Shape sh = at.createTransformedShape(path);
        g.fill(sh);
        g.draw(sh);

        // เส้นอุด (DAMS)
        g.setStroke(new BasicStroke((float) Math.max(3, ArtConfig.DAM_WIDTH * s(at)),
                BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
        Point2D.Double p1 = new Point2D.Double();
        Point2D.Double p2 = new Point2D.Double();
        for (double[] d : dams) {
            p1.setLocation(d[0], d[1]);
            p2.setLocation(d[2], d[3]);
            at.transform(p1, p1);
            at.transform(p2, p2);
            g.drawLine((int) Math.round(p1.x), (int) Math.round(p1.y),
                       (int) Math.round(p2.x), (int) Math.round(p2.y));
        }
        g.dispose();

        if (!colorOn) return img;

        int[] px = ((DataBufferInt) img.getRaster().getDataBuffer()).getData();
        int radius = Math.max(2, (int) Math.round(1.5 * s(at)));

        Point2D.Double p = new Point2D.Double();
        for (ArtConfig.Seed seed : seeds) {
            p.setLocation(seed.x, seed.y);
            at.transform(p, p);
            int sx = (int) Math.round(p.x);
            int sy = (int) Math.round(p.y);
            int want = seed.color.getRGB() & 0xFFFFFF;

            int start = findBlankNear(px, w, h, sx, sy, radius);
            if (start >= 0) {
                floodFill(px, w, h, start % w, start / w, want);
            } else if (!hasColourNear(px, w, h, sx, sy, radius, want)) {
                System.err.println("[seed] (" + seed.x + ", " + seed.y + ") ที่ " + w + "x" + h
                        + " - บริเวณรวมกับเพื่อนบ้านคนละสี");
            }
        }
        return img;
    }

    private static double s(AffineTransform at) {
        return at.getScaleX();
    }

    // =================== Flood Fill ===================
    public static void floodFill(int[] px, int w, int h, int sx, int sy, int rgb) {
        int fill = rgb & 0xFFFFFF;
        if (fill == ArtConfig.BLANK || (px[sy * w + sx] & 0xFFFFFF) != ArtConfig.BLANK) return;

        int[] stack = new int[1024];
        int sp = 0;
        stack[sp++] = sy * w + sx;

        while (sp > 0) {
            int p = stack[--sp];
            if ((px[p] & 0xFFFFFF) != ArtConfig.BLANK) continue;
            int y = p / w, row = y * w;

            int left = p - row;
            while (left > 0 && (px[row + left - 1] & 0xFFFFFF) == ArtConfig.BLANK) left--;
            int right = p - row;
            while (right < w - 1 && (px[row + right + 1] & 0xFFFFFF) == ArtConfig.BLANK) right++;
            for (int i = left; i <= right; i++) px[row + i] = 0xFF000000 | fill;

            for (int dy = -1; dy <= 1; dy += 2) {
                int ny = y + dy;
                if (ny < 0 || ny >= h) continue;
                int nrow = ny * w;
                boolean inRun = false;
                for (int i = left; i <= right; i++) {
                    if ((px[nrow + i] & 0xFFFFFF) == ArtConfig.BLANK) {
                        if (!inRun) {
                            if (sp == stack.length) stack = Arrays.copyOf(stack, stack.length * 2);
                            stack[sp++] = nrow + i;
                            inRun = true;
                        }
                    } else {
                        inRun = false;
                    }
                }
            }
        }
    }

    public static int findBlankNear(int[] px, int w, int h, int sx, int sy, int maxR) {
        for (int r = 0; r <= maxR; r++) {
            for (int dy = -r; dy <= r; dy++) {
                for (int dx = -r; dx <= r; dx++) {
                    if (r > 0 && Math.abs(dx) != r && Math.abs(dy) != r) continue;
                    int x = sx + dx, y = sy + dy;
                    if (x >= 0 && x < w && y >= 0 && y < h
                            && (px[y * w + x] & 0xFFFFFF) == ArtConfig.BLANK) {
                        return y * w + x;
                    }
                }
            }
        }
        return -1;
    }

    public static boolean hasColourNear(int[] px, int w, int h, int sx, int sy, int maxR, int rgb) {
        for (int y = Math.max(0, sy - maxR); y <= Math.min(h - 1, sy + maxR); y++) {
            for (int x = Math.max(0, sx - maxR); x <= Math.min(w - 1, sx + maxR); x++) {
                if ((px[y * w + x] & 0xFFFFFF) == rgb) return true;
            }
        }
        return false;
    }
}
