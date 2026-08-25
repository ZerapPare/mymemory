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
 * รับ Path2D กับรายการ seed แล้วคืนภาพที่ลงสีเสร็จแล้ว
 *
 * ที่ต้องเรนเดอร์ลง BufferedImage ไม่ใช่วาดลงจอตรงๆ เพราะ flood fill ต้อง
 * "อ่าน" พิกเซลกลับมา ซึ่ง Graphics2D ของ panel ทำไม่ได้
 *
 * ตัวนี้ไม่รู้จัก Swing ไม่รู้จักไฟล์ SVG รู้แค่พิกเซล
 */
public final class FillEngine {

    private FillEngine() {
    }

    /** วาดลายเส้นลงภาพ ลากเส้นอุด แล้วเทสีทีละ seed */
    public static BufferedImage rasterise(Path2D path, ArtConfig.Seed[] seeds,
            AffineTransform at, int w, int h, boolean colorOn) {

        BufferedImage img = new BufferedImage(w, h, BufferedImage.TYPE_INT_RGB);
        Graphics2D g = img.createGraphics();
        g.setColor(Color.WHITE);
        g.fillRect(0, 0, w, h);

        // ปิด AA ไม่ใช่เรื่องความสวย แต่เพราะ flood fill ต้องการขอบคมชัด
        // ถ้าเปิด ขอบหมึกจะเป็นเทาไล่เฉด สีจะหยุดก่อนถึงเส้นแล้วเหลือขอบขาวซีด
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_OFF);
        g.setStroke(new BasicStroke(1f));
        g.setColor(Color.BLACK);

        Shape sh = at.createTransformedShape(path);
        g.fill(sh);
        // ลากขอบทับไม่ใช่การตกแต่ง แต่เป็นการอุดรู เส้น potrace เป็นสลิ่วบางที่
        // แค่แตะกัน ถมอย่างเดียวเหลือรูจิ๋วให้สีลอดทะลุไปบริเวณข้างเคียง
        g.draw(sh);

        drawDams(g, at);
        g.dispose();

        if (!colorOn) return img;

        int[] px = ((DataBufferInt) img.getRaster().getDataBuffer()).getData();

        // seed ที่วัดไว้ที่ขนาดหนึ่ง อาจไปตกใต้เส้นที่อีกขนาดเพราะเส้นขยับตาม
        // สเกล ให้ขยับหาพิกเซลว่างใกล้สุดได้ 1.5 หน่วย viewBox ซึ่งแคบกว่า
        // ระยะระหว่างบริเวณมาก จึงข้ามเส้นไปผิดฝั่งไม่ได้
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
                // ไม่มีที่ให้เท และสีที่อยู่ตรงนั้นก็ผิด แปลว่าบริเวณนี้รวมเข้ากับ
                // เพื่อนบ้านคนละสี ถ้าสีตรงกันถือว่าไม่เป็นไร จึงเตือนเฉพาะกรณีผิด
                System.err.println("[seed] (" + seed.x + ", " + seed.y + ") ที่ " + w + "x" + h
                        + " - บริเวณรวมกับเพื่อนบ้านคนละสี");
            }
        }
        return img;
    }

    /** เส้นอุดต้องมาก่อน flood fill ถึงจะทำหน้าที่เป็นกำแพงได้ */
    private static void drawDams(Graphics2D g, AffineTransform at) {
        // คูณสเกลด้วย เส้นจะได้หนาเท่ากันเมื่อเทียบกับรูป ไม่ว่าหน้าต่างขนาดไหน
        float width = (float) Math.max(ArtConfig.MIN_DAM_PX, ArtConfig.DAM_WIDTH * at.getScaleX());
        g.setStroke(new BasicStroke(width, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));

        Point2D.Double p1 = new Point2D.Double();
        Point2D.Double p2 = new Point2D.Double();
        for (double[] d : ArtConfig.DAMS) {
            p1.setLocation(d[0], d[1]);
            p2.setLocation(d[2], d[3]);
            at.transform(p1, p1);
            at.transform(p2, p2);
            g.drawLine((int) Math.round(p1.x), (int) Math.round(p1.y),
                       (int) Math.round(p2.x), (int) Math.round(p2.y));
        }
    }

    /**
     * flood fill แบบ span 4 ทิศ
     *
     * ใช้ 4 ทิศไม่ใช่ 8 เพื่อไม่ให้สีเล็ดลอดตามแนวทแยงระหว่างพิกเซลหมึกสองตัว
     * ที่แตะกันแค่มุมเดียว เติมทั้งแถวรวดเดียวแล้ว push แค่จุดเดียวต่อแถวใหม่
     * สแต็กเลยอยู่ระดับร้อย ไม่ใช่หนึ่งช่องต่อพิกเซล - บริเวณเดียวที่นี่อาจ
     * ใหญ่เกือบเต็มจอ ถ้าใช้ recursion จะ StackOverflow
     */
    public static void floodFill(int[] px, int w, int h, int sx, int sy, int rgb) {
        int fill = rgb & 0xFFFFFF;
        if (fill == ArtConfig.BLANK || (px[sy * w + sx] & 0xFFFFFF) != ArtConfig.BLANK) return;

        int[] stack = new int[1024];
        int sp = 0;
        stack[sp++] = sy * w + sx;

        while (sp > 0) {
            int p = stack[--sp];
            if ((px[p] & 0xFFFFFF) != ArtConfig.BLANK) continue; // แถวก่อนหน้าเติมไปแล้ว
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

    /**
     * พิกเซลว่างที่ใกล้ seed ที่สุด ไล่หาเป็นวงทีละชั้น บริเวณที่เทสีไปแล้ว
     * ไม่นับว่าว่าง การขยับจึงไปแย่งสีของเพื่อนบ้านไม่ได้ อย่างแย่ที่สุดคือ
     * หาไม่เจอแล้วฟ้อง
     */
    public static int findBlankNear(int[] px, int w, int h, int sx, int sy, int maxR) {
        for (int r = 0; r <= maxR; r++) {
            for (int dy = -r; dy <= r; dy++) {
                for (int dx = -r; dx <= r; dx++) {
                    if (r > 0 && Math.abs(dx) != r && Math.abs(dy) != r) continue; // เอาเฉพาะขอบวง
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

    /** มีพิกเซลในรัศมีที่เป็นสีที่ seed นี้ต้องการอยู่แล้วหรือไม่ */
    public static boolean hasColourNear(int[] px, int w, int h, int sx, int sy, int maxR, int rgb) {
        for (int y = Math.max(0, sy - maxR); y <= Math.min(h - 1, sy + maxR); y++) {
            for (int x = Math.max(0, sx - maxR); x <= Math.min(w - 1, sx + maxR); x++) {
                if ((px[y * w + x] & 0xFFFFFF) == rgb) return true;
            }
        }
        return false;
    }
}
