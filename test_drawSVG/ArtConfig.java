import java.awt.Color;
import java.awt.geom.AffineTransform;

/**
 * Data / Configuration
 *
 * ทุกอย่างที่ "ปรับแต่งได้" อยู่ในไฟล์นี้ไฟล์เดียว - สี ตำแหน่งที่จะเทสี
 * เส้นอุด และกรอบภาพ ไฟล์อีกสามไฟล์เป็นกลไกล้วนๆ ไม่มีค่าคงที่ให้ต้องแก้
 *
 * ถ้าจะเปลี่ยนหน้าตางาน แก้ที่นี่พอ
 */
public final class ArtConfig {

    private ArtConfig() {
    }

    /** ไฟล์ภาพเรียงตามลำดับที่จะเล่น */
    public static final String[] FILES = {
        "test_drawSVG/a1.svg",
        "test_drawSVG/a2.svg",
        "test_drawSVG/a3.svg",
        "test_drawSVG/b1.svg",
        "test_drawSVG/b2.svg",
        "test_drawSVG/b3.svg"
    };

    /** หน่วงระหว่างเฟรม (ms) */
    public static final int FRAME_DELAY = 350;

    // ============ THE PALETTE - เปลี่ยนสีที่นี่ที่เดียว ============
    // ตารางข้างล่างบอกแค่ว่าสี "ไปลงตรงไหน" ไม่ได้บอกว่าเป็นสีอะไร
    public static final Color BACKDROP = new Color(0xF2E4CB);
    public static final Color HAIR     = new Color(0x6B4A2E);
    public static final Color SKIN     = new Color(0xFFDCB8);
    public static final Color SHIRT    = new Color(0x7FA8D4);
    public static final Color SCREEN   = new Color(0xDCEBFF);
    // ============================================================

    /** จุดเริ่มเทสีหนึ่งจุด พิกัดเป็นหน่วย viewBox (0..600 x 0..384) ไม่ใช่พิกเซลจอ */
    public static final class Seed {
        public final double x;
        public final double y;
        public final Color color;

        public Seed(double x, double y, Color color) {
            this.x = x;
            this.y = y;
            this.color = color;
        }
    }

    /**
     * พิกัดพวกนี้วัดมาจากภาพที่เรนเดอร์จริง ไม่ได้เดา - label ช่องว่างทุกก้อนแล้ว
     * ใช้กึ่งกลางของแถวที่กว้างที่สุดของแต่ละก้อน (ไม่ใช่ centroid เพราะบริเวณ
     * เว้าอย่างแขนจะทำให้ centroid หลุดออกไปนอกรูป)
     *
     * ทั้ง 6 ไฟล์เป็นภาพต่างกันจริงแค่ 3 แบบ (a1=b1, a2=b2, a3=b3) และมีแต่
     * แท็บเล็ตที่ขยับ ส่วนอื่นอยู่ที่เดิมทุกเฟรม จึงแยกเป็นสองตาราง
     *
     * หาพิกัดเพิ่มได้ด้วยการรันแล้วคลิกบนรูป มันจะพิมพ์บรรทัด Seed ให้เลย
     */
    public static final Seed[] COMMON = {
        new Seed( 30.0,  30.0, BACKDROP), // พื้นหลัง
        new Seed(229.8, 207.6, SHIRT),    // ลำตัว
        new Seed(301.0,  75.5, HAIR),     // ผม
        new Seed(325.2, 111.1, SKIN),     // ใบหน้า
        new Seed(294.4, 142.0, SKIN),     // ใบหน้า
        new Seed(350.3, 243.5, SHIRT),    // ท่อนแขน
        new Seed(306.4, 200.0, SKIN),     // มือที่คาง
        new Seed(333.5, 210.4, SKIN),     // มือที่คาง
        new Seed(327.9, 175.8, SKIN),
    };

    /** แท็บเล็ตเป็นอย่างเดียวที่ขยับ เลยต้องมี seed ต่อเฟรม */
    public static final Seed[] SCREEN_PER_FRAME = {
        new Seed(411.7, 289.3, SCREEN), // a1
        new Seed(365.4, 323.6, SCREEN), // a2
        new Seed(404.5, 294.9, SCREEN), // a3
        new Seed(411.7, 289.3, SCREEN), // b1 (ภาพเดียวกับ a1)
        new Seed(365.4, 323.6, SCREEN), // b2 (ภาพเดียวกับ a2)
        new Seed(404.5, 294.9, SCREEN), // b3 (ภาพเดียวกับ a3)
    };

    /** รวม seed ที่ใช้ร่วมกันกับ seed เฉพาะเฟรมนั้น */
    public static Seed[] seedsFor(int frameIndex) {
        if (frameIndex < 0 || frameIndex >= SCREEN_PER_FRAME.length) {
            return COMMON;
        }
        Seed[] out = new Seed[COMMON.length + 1];
        System.arraycopy(COMMON, 0, out, 0, COMMON.length);
        out[COMMON.length] = SCREEN_PER_FRAME[frameIndex];
        return out;
    }

    /**
     * เส้นดำที่เราลากเพิ่มเองเพื่ออุดช่องที่ลายเส้นต้นฉบับเปิดค้างไว้
     * แต่ละแถวคือ {x1, y1, x2, y2} หน่วย viewBox เหมือน Seed
     *
     * ที่ต้องมีเพราะขอบโต๊ะใน SVG จบกลางอากาศ ไม่ได้ลากถึงขอบภาพ พื้นที่โต๊ะ
     * กับพื้นหลังจึงทะลุถึงกัน เทสีโต๊ะทีเดียวสีท่วมทั้งจอ ลากเส้นปิดปลายที่
     * เปิดอยู่ก็แยกสองบริเวณออกจากกันได้
     *
     * ปลายเส้นเลยขอบ viewBox ได้ (ค่าติดลบ หรือเกิน 600/384) Java2D ตัดให้เอง
     * ซึ่งดีกว่าหยุดพอดีขอบ เพราะจะได้ไม่เหลือรูที่มุม
     */
    public static final double[][] DAMS = {
        { 149.4, 355.4, -37.4, 456.5 },  // ปิดขอบโต๊ะด้านซ้ายล่าง
        { 485.1, 238.3, 623.4, 257.2 },  // ปิดขอบโต๊ะด้านขวา
    };

    /**
     * ความหนาเส้นอุด หน่วย viewBox (คูณสเกลตามขนาดหน้าต่างให้เอง)
     *
     * แยกจากความหนาเส้นขอบของภาพ เพราะถ้าไปเพิ่มตรงนั้นลายเส้นการ์ตูนจะหนาตาม
     * ไปด้วยทั้งรูป flood fill เป็นแบบ 4 ทิศ จึงผ่านเส้นทแยงหนา 1 พิกเซลไม่ได้
     * อยู่แล้ว ความหนาที่เกินมามีไว้กันกรณีปลายเส้นไม่ได้แตะเส้นหมึกเดิมพอดีเป๊ะ
     */
    public static final double DAM_WIDTH = 1.0;

    /** เส้นอุดต้องไม่บางกว่านี้ (พิกเซล) แม้หน้าต่างจะเล็กแค่ไหน */
    public static final float MIN_DAM_PX = 3f;

    /** เทสีลงเฉพาะพิกเซลขาวล้วน เส้นหมึกและสีที่เทไปแล้วกั้นอยู่ */
    public static final int BLANK = 0xFFFFFF;

    /**
     * ทุกเฟรมใช้กรอบนี้ร่วมกัน ไม่ใช้ bounds ของแต่ละเฟรม - ไม่งั้นตัวการ์ตูน
     * จะเต้นตอนสลับ และ seed ที่วัดไว้จะเลื่อนตามไปด้วย
     * (a1/b1 เป็น 600x383 ที่เหลือ 600x384 ต่างกัน 0.26% ใช้ค่าเดียวได้)
     */
    public static final double VBW = 600, VBH = 384, PAD = 24;

    /** สเกลที่ทำให้ viewBox พอดีหน้าต่าง โดยไม่บิดสัดส่วน */
    public static double scaleFor(int w, int h) {
        return Math.min(Math.max(1, w - 2 * PAD) / VBW, Math.max(1, h - 2 * PAD) / VBH);
    }

    /**
     * viewBox -> พิกเซลจอ อยู่ที่เดียวเพื่อให้การวาดกับการอ่านพิกัดจากเมาส์
     * ใช้สูตรเดียวกันแน่นอน ไม่งั้นคลิกแล้วได้พิกัดเพี้ยน
     */
    public static AffineTransform viewTransform(int w, int h) {
        double s = scaleFor(w, h);
        AffineTransform at = new AffineTransform();
        at.translate(w / 2.0, h / 2.0);
        at.scale(s, s);
        at.translate(-VBW / 2, -VBH / 2);
        return at;
    }

    /** บอกว่าพิกเซลนั้นคืออะไร จะได้รู้ว่าคลิกโดนเส้นหรือโดนช่องว่าง */
    public static String describe(int rgb) {
        if (rgb == 0x000000) return "black";
        if (rgb == BLANK) return "blank";
        if (rgb == (BACKDROP.getRGB() & 0xFFFFFF)) return "background";
        if (rgb == (HAIR.getRGB() & 0xFFFFFF)) return "hair";
        if (rgb == (SKIN.getRGB() & 0xFFFFFF)) return "skin";
        if (rgb == (SHIRT.getRGB() & 0xFFFFFF)) return "shirt";
        if (rgb == (SCREEN.getRGB() & 0xFFFFFF)) return "screen";
        return String.format("#%06X", rgb);
    }
}
