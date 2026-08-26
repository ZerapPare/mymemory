import java.awt.Color;
import java.util.Arrays;

/**
 * Data / Configuration
 *
 * ทุกอย่างที่ "ปรับแต่งได้" อยู่ในไฟล์นี้ไฟล์เดียว - สี, จุดเทสี, เส้นอุด,
 * กรอบภาพ, รายชื่อไฟล์, จังหวะเวลา ไฟล์อื่นเป็นกลไกล้วนๆ
 *
 * จุดสำคัญ: seed ผูกอยู่กับ "ซีน" ไม่ใช่ค่ากลางที่ทุกซีนใช้ร่วมกัน เพราะแต่ละ
 * ซีนท่าทางคนละอย่าง พิกัดที่วัดจากท่านั่งเอาไปใช้กับท่าดีใจไม่ได้
 */
public final class ArtConfig {

    private ArtConfig() {
    }

    public static final int FRAME_INTERVAL = 360; // ms ต่อเฟรม
    public static final int TICK_INTERVAL = 30; // ms ต่อการวาด

    // ------ สีที่ใช้ใน SVG ------
    public static final Color BACKDROP = new Color(0xDCEBFF);
    public static final Color TABLE = new Color(0xF2E4CB);
    public static final Color HAIR = new Color(0x6B4A2E);
    public static final Color SKIN = new Color(0xFFDCB8);
    public static final Color SHIRT = new Color(0x7FA8D4);
    public static final Color INSIDESHIRT = new Color(0xF2EFE7);
    public static final Color SCREEN = new Color(0xDCEBFF);

    /** จุดเริ่มเทสีหนึ่งจุด พิกัดเป็นหน่วย viewBox ไม่ใช่พิกเซลจอ */
    public static class Seed {
        public final double x, y;
        public final Color color;

        public Seed(double x, double y, Color color) {
            this.x = x;
            this.y = y;
            this.color = color;
        }
    }

    /**
     * หนึ่งซีน = ไฟล์ภาพ + seed ของตัวเอง + เส้นอุดของตัวเอง
     *
     * ที่ต้องแยกต่อซีนเพราะพอเปลี่ยนท่า ตำแหน่งผม/หน้า/เสื้อ ก็ย้ายตาม
     * seed ชุดเดิมจะไปตกบนเส้นหรือตกผิดบริเวณ
     */
    public static final class Scene {
        public final String name;
        public final String[] files;
        /** seed ที่ทุกเฟรมในซีนนี้ใช้ร่วมกัน (ส่วนที่ไม่ขยับระหว่างเฟรม) */
        public final Seed[] common;
        /** seed เฉพาะเฟรม เรียงตรงกับ files - สั้นกว่า files ได้ */
        public final Seed[] perFrame;
        /** เส้นอุดของซีนนี้ {x1,y1,x2,y2} */
        public final double[][] dams;

        public Scene(String name, String[] files, Seed[] common, Seed[] perFrame, double[][] dams) {
            this.name = name;
            this.files = files;
            this.common = common;
            this.perFrame = perFrame;
            this.dams = dams;
        }

        /**
         * seed ทั้งหมดของเฟรมที่ i
         *
         * ต่อ perFrame ไว้ "ท้ายสุด" เสมอ - flood fill เทลงเฉพาะพิกเซลขาว
         * ถ้ามี seed สองจุดตกบริเวณเดียวกัน ตัวแรกชนะ ลำดับจึงมีผลต่อภาพ
         */
        public Seed[] seedsFor(int i) {
            if (i < 0 || i >= perFrame.length) {
                return common;
            }
            Seed[] out = Arrays.copyOf(common, common.length + 1);
            out[common.length] = perFrame[i];
            return out;
        }
    }

    /** เส้นอุดของห้องเรียน - ขอบโต๊ะใน SVG จบกลางอากาศ ไม่ลากถึงขอบภาพ */
    private static final double[][] ROOM_DAMS = {
            { 149.4, 355.4, -37.4, 456.5 },
            { 485.1, 238.3, 659.5, 262.5 },
    };

    // ==================== ซีน 1 : นั่งรอหน้าจอ ====================

    private static final Seed[] SITTING = {
            new Seed(30.0, 30.0, BACKDROP),
            new Seed(317.8, 257.2, BACKDROP),
            new Seed(218.6, 413.4, TABLE),
            new Seed(229.8, 207.6, SHIRT),
            new Seed(301.0, 75.5, HAIR),
            new Seed(325.2, 111.1, SKIN),
            new Seed(294.4, 142.0, SKIN),
            new Seed(350.3, 243.5, SHIRT),
            new Seed(306.4, 200.0, SKIN),
            new Seed(333.5, 210.4, SKIN),
            new Seed(327.9, 175.8, SKIN),
            new Seed(302.2, 162.4, SKIN),
            new Seed(315.6, 168.0, SKIN),
            new Seed(326.8, 178.1, SKIN),
            new Seed(355.8, 208.2, SHIRT),
            new Seed(298.9, 272.9, SHIRT),
            new Seed(333.5, 152.4, SHIRT),
            new Seed(306.7, 110.0, SKIN),
            new Seed(267.7, 106.7, HAIR),
            new Seed(267.7, 105.6, HAIR),
            new Seed(281.0, 100.0, SKIN),
            new Seed(227.5, 166.9, INSIDESHIRT),
    };

    /** จอแท็บเล็ตขยับตามเฟรม จึงต้องมี seed ต่อเฟรม */
    private static final Seed[] SITTING_SCREENS = {
            new Seed(411.7, 289.3, SCREEN),
            new Seed(365.4, 323.6, SCREEN),
            new Seed(404.5, 294.9, SCREEN),
            new Seed(411.7, 289.3, SCREEN),
            new Seed(365.4, 323.6, SCREEN),
            new Seed(404.5, 294.9, SCREEN),
    };

    public static final Scene SCENE_1 = new Scene("sitting",
            new String[] {
                    "test_drawSVG/a1.svg",
                    "test_drawSVG/a2.svg",
                    "test_drawSVG/a3.svg",
                    "test_drawSVG/a4.svg",
                    "test_drawSVG/a5.svg",
                    "test_drawSVG/a6.svg",
            },
            SITTING, SITTING_SCREENS, ROOM_DAMS);

    // ==================== ซีน 2 : ตกใจ ====================
    // พิกัดชุดนี้ต้องวัดจากภาพ b1 เอง ใช้ของซีน 1 ไม่ได้เพราะท่าคนละท่า
    // หาเพิ่มได้ด้วยการรันแล้วคลิกบนรูป มันพิมพ์บรรทัด Seed ให้

    private static final Seed[] SHOCKED = {
            // new Seed( 30.0, 30.0, BACKDROP), // ผนัง
            // new Seed(298.9, 451.8, TABLE), // โต๊ะ - แยกจากผนังได้เพราะเส้นอุด
            // new Seed(203.3, 302.9, SHIRT), // ลำตัว
            // new Seed(346.5, 247.0, SHIRT), // แขนเสื้อ
            // new Seed(315.2, 262.7, SHIRT), // ข้อศอก
            // new Seed(301.1, 75.7, HAIR), // ผม
            // new Seed(293.5, 142.0, SKIN), // ใบหน้า
            // new Seed(297.8, 117.0, SKIN), // หน้าผาก
            // new Seed(303.8, 124.6, SKIN), // ตา
            // new Seed(279.3, 176.8, SKIN), // มือที่คาง
            // new Seed(252.0, 160.0, SKIN), // คอ
            // new Seed(210.0, 146.0, INSIDESHIRT), // สายบ่า
            // new Seed(238.6, 177.9, INSIDESHIRT), // ปกเสื้อ
            // new Seed(400.5, 292.5, SCREEN), // จอแท็บเล็ต
            // new Seed(400.5, 311.0, SCREEN), // ไอคอนบนจอ
    };

    private static final Seed[] SHOCKED_FISTS = {
            new Seed(387.0, 132.2, SKIN), // c1
            new Seed(402.2, 142.0, SKIN), // c2
    };

    public static final Scene SCENE_2 = new Scene("shock",
            new String[] {
                    "test_drawSVG/b1.svg",
            }, SHOCKED, SHOCKED_FISTS, ROOM_DAMS);

    // ==================== ซีน 3 : ดีใจ ====================
    // พิกัดชุดนี้ต้องวัดจากภาพ c1/c2 เอง ใช้ของซีน 1 ไม่ได้เพราะท่าคนละท่า
    // หาเพิ่มได้ด้วยการรันแล้วคลิกบนรูป มันพิมพ์บรรทัด Seed ให้

    private static final Seed[] CELEBRATING = {
            new Seed(30.0, 30.0, BACKDROP), // ผนัง
            new Seed(298.9, 451.8, TABLE), // โต๊ะ - แยกจากผนังได้เพราะเส้นอุด
            new Seed(203.3, 302.9, SHIRT), // ลำตัว
            new Seed(346.5, 247.0, SHIRT), // แขนเสื้อ
            new Seed(315.2, 262.7, SHIRT), // ข้อศอก
            new Seed(301.1, 75.7, HAIR), // ผม
            new Seed(293.5, 142.0, SKIN), // ใบหน้า
            new Seed(297.8, 117.0, SKIN), // หน้าผาก
            new Seed(303.8, 124.6, SKIN), // ตา
            new Seed(279.3, 176.8, SKIN), // มือที่คาง
            new Seed(252.0, 160.0, SKIN), // คอ
            new Seed(210.0, 146.0, INSIDESHIRT), // สายบ่า
            new Seed(238.6, 177.9, INSIDESHIRT), // ปกเสื้อ
            new Seed(400.5, 292.5, SCREEN), // จอแท็บเล็ต
            new Seed(400.5, 311.0, SCREEN), // ไอคอนบนจอ
    };

    /** กำปั้นที่ชูขึ้นขยับระหว่างเฟรม จึงต้องมี seed ต่อเฟรม */
    private static final Seed[] CELEBRATING_FISTS = {
            new Seed(387.0, 132.2, SKIN), // c1
            new Seed(402.2, 142.0, SKIN), // c2
    };

    public static final Scene SCENE_3 = new Scene("happy",
            new String[] {
                    "test_drawSVG/c1.svg",
                    "test_drawSVG/c2.svg",
            },
            CELEBRATING, CELEBRATING_FISTS, ROOM_DAMS);

    /** ลำดับการเล่น - จบซีนหนึ่งแล้วไปซีนถัดไป วนกลับมาซีนแรก */
    public static final Scene[] SCENES = { SCENE_1, SCENE_2, SCENE_3 };

    /** เทสีลงเฉพาะพิกเซลขาวล้วน เส้นหมึกและสีที่เทไปแล้วกั้นอยู่ */
    public static final int BLANK = 0xFFFFFF;

    /** ทุกเฟรมใช้กรอบนี้ร่วมกัน ไม่ใช้ bounds ของแต่ละเฟรม */
    public static final double VBW = 600, VBH = 384, PAD = 24;

    public static final double DAM_WIDTH = 1.0;

    /** บอกว่าพิกเซลนั้นคืออะไร จะได้รู้ว่าคลิกโดนเส้นหรือโดนช่องว่าง */
    public static String describe(int rgb) {
        if (rgb == 0x000000)
            return "black";
        if (rgb == BLANK)
            return "blank";
        if (rgb == (BACKDROP.getRGB() & 0xFFFFFF))
            return "background";
        if (rgb == (HAIR.getRGB() & 0xFFFFFF))
            return "hair";
        if (rgb == (SKIN.getRGB() & 0xFFFFFF))
            return "skin";
        if (rgb == (SHIRT.getRGB() & 0xFFFFFF))
            return "shirt";
        if (rgb == (SCREEN.getRGB() & 0xFFFFFF))
            return "screen";
        return String.format("#%06X", rgb);
    }
}