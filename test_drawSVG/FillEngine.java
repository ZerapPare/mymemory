import java.awt.geom.AffineTransform;
import java.awt.geom.Path2D;
import java.awt.geom.Point2D;
import java.awt.image.BufferedImage;
import java.util.Arrays;

/**
 * Flood Fill / Rasterization Engine
 *
 * รับ Path2D แล้วคืนภาพลายเส้นที่ลงสีเสร็จแล้ว (ยังไม่รวม Drawable)
 *
 * ทุกพิกเซลในภาพมาจาก Raster.plot() ผ่าน Gfx ไม่มีคำสั่งวาดของ Java2D เหลืออยู่
 * ที่ต้องเรนเดอร์ลง Raster ก่อน เพราะ flood fill ต้อง "อ่าน" พิกเซลกลับมา
 */
public final class FillEngine {

    private FillEngine() {
    }

    /**
     * วาดเฉพาะพื้นหลัง (SVG + flood fill) ไม่รวม Drawable
     *
     * รับ seed กับเส้นอุดเข้ามาเป็นพารามิเตอร์ ไม่ไปหยิบจาก ArtConfig เอง
     * แต่ละซีนจึงส่งชุดของตัวเองมาได้ และไม่มีการอ้าง seed ด้วย index อีก
     */
    public static BufferedImage rasteriseBackground(Path2D path, ArtConfig.Seed[] seeds,
            double[][] dams, AffineTransform at, int w, int h, boolean colorOn) {

        // ARGB ไม่ใช่ RGB เพราะตอนท้ายจะทำบริเวณผนังให้โปร่ง
        // props ที่วาดไว้ก่อนหน้าจะได้ทะลุขึ้นมาได้ ตัวละครจึงอยู่หน้า props
        Raster r = new Raster(w, h);
        r.clear(0xFF000000 | ArtConfig.BLANK);

        int ink = 0xFF000000;

        // ---- ลายเส้นตัวละคร ----
        // ต้องทั้งถมและลากขอบ - เส้น potrace เป็นสลิ่วบางที่แค่แตะกัน ถมอย่างเดียว
        // เหลือรูจิ๋วให้สีลอดทะลุ (ดู docs/setup.md หัวข้อ "สามข้อที่ห้ามแตะ")
        Gfx.Contours c = Gfx.contours(at.createTransformedShape(path));
        Gfx.scanlineFill(r, c.pts, c.ends, ink);
        Gfx.strokeContours(r, c, 1, ink);

        // ---- เส้นอุด (DAMS) ----
        double damW = Math.max(3, ArtConfig.DAM_WIDTH * at.getScaleX());
        Point2D.Double p1 = new Point2D.Double();
        Point2D.Double p2 = new Point2D.Double();
        for (double[] d : dams) {
            p1.setLocation(d[0], d[1]);
            p2.setLocation(d[2], d[3]);
            at.transform(p1, p1);
            at.transform(p2, p2);
            Gfx.thickLine(r, p1.x, p1.y, p2.x, p2.y, damW, ink);
        }

        if (!colorOn) return r.image();

        // ---- ลงสีทีละ seed ----
        int[] px = r.px;
        int radius = Math.max(2, (int) Math.round(1.5 * at.getScaleX()));

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

        makeWallTransparent(px);
        return r.image();
    }

    /**
     * เปลี่ยนพิกเซลสีผนังให้โปร่งใส
     *
     * ต้องทำ "หลัง" flood fill ครบทุก seed แล้วเท่านั้น - พิกเซลโปร่งมีค่า RGB
     * เป็น 0 ซึ่งชนกับสีหมึกดำ ถ้าทำระหว่างทางจะไปกวน seed ตัวถัดไป
     *
     * ผลคือ props ที่ panel วาดไว้ก่อนหน้าทะลุขึ้นมาตรงผนังได้ ตัวละครจึงอยู่หน้า
     * props แทนที่จะถูกทับ
     */
    private static void makeWallTransparent(int[] px) {
        int wall = ArtConfig.BACKDROP.getRGB() & 0xFFFFFF;
        for (int i = 0; i < px.length; i++) {
            if ((px[i] & 0xFFFFFF) == wall) {
                px[i] = 0x00000000;
            }
        }
        // ไม่เตือนตรงนี้ - ภาพทึบทั้งใบเป็นปัญหาก็ต่อเมื่อซีนนั้นมี props อยู่ข้างหลัง
        // ซึ่ง FillEngine ไม่รู้ คนเรียกเป็นคนเช็ค
    }

    /** ภาพนี้มีพิกเซลโปร่งบ้างไหม - ใช้เช็คว่าซีนลืม seed BACKDROP หรือเปล่า */
    public static boolean hasTransparent(BufferedImage img) {
        for (int y = 0; y < img.getHeight(); y++) {
            for (int x = 0; x < img.getWidth(); x++) {
                if ((img.getRGB(x, y) >>> 24) == 0) return true;
            }
        }
        return false;
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
