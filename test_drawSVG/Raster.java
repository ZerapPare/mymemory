import java.awt.image.BufferedImage;
import java.awt.image.DataBufferInt;

/**
 * Framebuffer - ที่เดียวในโปรเจกต์ที่แตะพิกเซล
 *
 * ทุกอย่างที่ปรากฏบนจอผ่าน plot() ตัวนี้ตัวเดียว ไม่มีคำสั่งวาดรูปทรงของ Java2D
 * เหลืออยู่เลย Gfx สร้างเส้นและรูปทรงจาก plot() ส่วน FillEngine ทำ flood fill
 * บน px[] ตรงๆ
 *
 * ที่ต้องมี buffer เก็บผลไว้ ไม่ใช่วาดลงจอตรงๆ เพราะ flood fill ต้อง "อ่าน" ค่า
 * พิกเซลข้างเคียงกลับมาเพื่อรู้ว่าชนขอบหมึกหรือยัง ซึ่ง Graphics2D ทำไม่ได้
 */
public final class Raster {

    /** ARGB หนึ่งช่องต่อพิกเซล เรียงทีละแถว - FillEngine เข้าถึงตรงๆ ตอน flood fill */
    public final int[] px;
    public final int w, h;

    /** ขอบเขตที่ยอมให้เขียน - มาแทน g.setClip() ปลายทั้งสองข้างรวมอยู่ในช่วง */
    private int clipX0, clipY0, clipX1, clipY1;

    private final BufferedImage image;

    public Raster(int w, int h) {
        this.w = Math.max(1, w);
        this.h = Math.max(1, h);
        this.image = new BufferedImage(this.w, this.h, BufferedImage.TYPE_INT_ARGB);
        this.px = ((DataBufferInt) image.getRaster().getDataBuffer()).getData();
        clipAll();
    }

    /** ภาพที่เอาไป blit ขึ้นจอ - พิกเซลทุกช่องในนี้มาจาก plot() ทั้งหมด */
    public BufferedImage image() {
        return image;
    }

    // =================== clip ===================

    public void clip(int x, int y, int cw, int ch) {
        clipX0 = Math.max(0, x);
        clipY0 = Math.max(0, y);
        clipX1 = Math.min(w - 1, x + cw - 1);
        clipY1 = Math.min(h - 1, y + ch - 1);
    }

    public void clipAll() {
        clipX0 = 0;
        clipY0 = 0;
        clipX1 = w - 1;
        clipY1 = h - 1;
    }

    /** เก็บ clip ปัจจุบันไว้คืนทีหลัง - คืนค่าเป็น {x0,y0,x1,y1} */
    public int[] saveClip() {
        return new int[] { clipX0, clipY0, clipX1, clipY1 };
    }

    public void restoreClip(int[] saved) {
        clipX0 = saved[0];
        clipY0 = saved[1];
        clipX1 = saved[2];
        clipY1 = saved[3];
    }

    // =================== เขียนพิกเซล ===================

    /**
     * พล็อตหนึ่งจุด ผสม alpha แบบ src-over ด้วยมือ
     *
     * ต้องผสมเองเพราะหลายที่ใช้สีโปร่ง - ลำแสงแดด alpha 25, ป้าย KMITL alpha 240,
     * ควันกาแฟ alpha 120 และตอนป้ายจางหายก่อนพุ่งเข้าหาคนดู
     */
    public void plot(int x, int y, int argb) {
        if (x < clipX0 || x > clipX1 || y < clipY0 || y > clipY1) return;

        int sa = argb >>> 24;
        if (sa == 0) return;

        int i = y * w + x;
        if (sa == 255) {
            px[i] = argb;
            return;
        }

        int dst = px[i];
        int da = dst >>> 24;

        // out = src + dst * (1 - sa)   คิดบนช่วง 0..255 ปัดด้วยการบวก 127
        int inv = 255 - sa;
        int oa = sa + (da * inv + 127) / 255;
        if (oa == 0) {
            px[i] = 0;
            return;
        }

        int sr = (argb >> 16) & 0xFF, sg = (argb >> 8) & 0xFF, sb = argb & 0xFF;
        int dr = (dst >> 16) & 0xFF, dg = (dst >> 8) & 0xFF, db = dst & 0xFF;

        // สีที่เก็บไว้ไม่ได้คูณ alpha ล่วงหน้า จึงต้องถ่วงน้ำหนักด้วย alpha ของแต่ละฝั่ง
        int ws = sa * 255;
        int wd = da * inv;
        int tot = ws + wd;
        int r = (sr * ws + dr * wd) / tot;
        int g = (sg * ws + dg * wd) / tot;
        int b = (sb * ws + db * wd) / tot;

        px[i] = (oa << 24) | (r << 16) | (g << 8) | b;
    }

    /** ถมหนึ่งแถวจาก xa ถึง xb - หัวใจของ scanline fill */
    public void span(int y, int xa, int xb, int argb) {
        if (y < clipY0 || y > clipY1) return;
        if (xa > xb) {
            int t = xa;
            xa = xb;
            xb = t;
        }
        xa = Math.max(xa, clipX0);
        xb = Math.min(xb, clipX1);
        if (xa > xb) return;

        int sa = argb >>> 24;
        if (sa == 255) {
            // ทึบสนิท เขียนตรงๆ ไม่ต้องผสม
            int row = y * w;
            java.util.Arrays.fill(px, row + xa, row + xb + 1, argb);
        } else {
            for (int x = xa; x <= xb; x++) plot(x, y, argb);
        }
    }

    /** ล้างทั้งผืน - ไม่ผสม alpha เขียนทับตรงๆ */
    public void clear(int argb) {
        java.util.Arrays.fill(px, argb);
    }

    public int get(int x, int y) {
        if (x < 0 || x >= w || y < 0 || y >= h) return 0;
        return px[y * w + x];
    }

    /** ARGB จาก java.awt.Color - ใช้สีเดิมใน ArtConfig/Art ต่อได้ */
    public static int argb(java.awt.Color c) {
        return c.getRGB();
    }

    public static int argb(java.awt.Color c, double alpha) {
        int a = (int) Math.round(Math.max(0, Math.min(1, alpha)) * 255);
        return (a << 24) | (c.getRGB() & 0xFFFFFF);
    }
}
