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
    public static final Color MOUTH = new Color(0xFF788D);
    public static final Color TEAR = new Color(0x30AFFF);
    public static final Color STAR = new Color(0xFFDA62);
    public static final Color HAT = new Color(0x1D2128);
    public static final Color MONITOR = new Color(0x2B3442);
    public static final Color DEVICE = new Color(0xC9D1DA);
    public static final Color PANTS = new Color(0x4A5568);
    public static final Color CHAIR = new Color(0x4A4A4A);
    public static final Color DARKCIRCLE = new Color(0x2C2C2C);


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
            new Seed(342.4, 133.4, BACKDROP),
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
            new Seed(324.5, 155.8, SKIN),
            new Seed(326.8, 178.1, SKIN),
            new Seed(355.8, 208.2, SHIRT),
            new Seed(298.9, 272.9, SHIRT),
            new Seed(333.5, 152.4, SHIRT),
            new Seed(306.7, 110.0, SKIN),
            new Seed(267.7, 106.7, HAIR),
            new Seed(267.7, 105.6, HAIR),
            new Seed(297.8, 131.2, SKIN),
            new Seed(281.0, 100.0, SKIN),
            new Seed(330.1, 136.8, SKIN),
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
            new Seed(30.0, 30.0, BACKDROP),      // ผนัง
            new Seed(341.3, 129.0, BACKDROP),
            new Seed(298.9, 451.8, TABLE),       // โต๊ะ - แยกจากผนังได้เพราะเส้นอุด
            new Seed(190.2, 192.0, SHIRT),       // ลำตัว
            // ดีเทลเสื้อ
            new Seed(150.6, 136.8, SHIRT),
            new Seed(185.1, 136.8, SHIRT),
            new Seed(165.1, 142.4, SHIRT),
            new Seed(371.7, 145.3, SHIRT),       // แขนขวาที่ยกขึ้น
            new Seed(297.8, 72.4, HAIR),         // ผม
            new Seed(264.3, 105.6, HAIR),
            new Seed(265.2, 123.5, SKIN),        // ใบหน้า
            new Seed(275.5, 98.9, SKIN),
            new Seed(306.5, 114.8, SKIN),        // ตา
            new Seed(81.5, 75.7, SKIN),          // กำปั้นซ้าย
            new Seed(458.7, 52.9, SKIN),         // กำปั้นขวา
            //นิ้วแขนซ้าย
            new Seed(89.2, 24.2, SKIN),
            new Seed(101.5, 36.4, SKIN),
            //นิ้วแขนขวา
            new Seed(459.5, 11.9, SKIN),
            new Seed(470.6, 20.8, SKIN),
            new Seed(469.5, 34.2, SKIN),
            new Seed(480.7, 38.7, SKIN),
            new Seed(462.8, 34.2, SKIN),
            //แว่น
            new Seed(298.9, 125.6, SKIN),
            new Seed(331.2, 126.8, SKIN),
            new Seed(292.2, 120.1, SKIN),
            new Seed(293.3, 158.0, MOUTH),        // ปาก
            new Seed(290.0, 131.2, TEAR),
            new Seed(335.7, 135.7, TEAR),
            new Seed(229.7, 163.6, INSIDESHIRT), // ข้างในเสื้อ
            new Seed(401.1, 293.1, SCREEN),      // จอแท็บเล็ต
            new Seed(400.0, 312.7, SCREEN),      // ไอคอนบนจอ
            new Seed(360.2, 301.9, STAR),
            new Seed(382.5, 310.8, HAT),
    };

    /** ซีนนี้มีเฟรมเดียว ไม่มีอะไรขยับระหว่างเฟรม */
    private static final Seed[] SHOCKED_FISTS = {};

    public static final Scene SCENE_2 = new Scene("shock",
            new String[] {
                    "test_drawSVG/b1.svg",
            }, SHOCKED, SHOCKED_FISTS, ROOM_DAMS);

    // ==================== ซีน 3 : ดีใจ ====================
    // พิกัดชุดนี้ต้องวัดจากภาพ c1/c2 เอง ใช้ของซีน 1 ไม่ได้เพราะท่าคนละท่า
    // หาเพิ่มได้ด้วยการรันแล้วคลิกบนรูป มันพิมพ์บรรทัด Seed ให้

    private static final Seed[] CELEBRATING = {
            new Seed(30.0, 30.0, BACKDROP), // ผนัง
            new Seed(344.6, 131.2, BACKDROP),
            new Seed(298.9, 451.8, TABLE), // โต๊ะ - แยกจากผนังได้เพราะเส้นอุด
            new Seed(203.3, 302.9, SHIRT), // ลำตัว
            new Seed(302.2, 275.1, SHIRT),
            new Seed(346.5, 247.0, SHIRT), // แขนเสื้อ
            new Seed(315.2, 262.7, SHIRT), // ข้อศอก
            new Seed(301.1, 75.7, HAIR), // ผม
            new Seed(293.5, 142.0, SKIN), // ใบหน้า
            new Seed(306.7, 107.8, SKIN),
            new Seed(330.1, 127.9, SKIN),
            new Seed(278.8, 101.1, SKIN),
            new Seed(297.8, 117.0, SKIN), // หน้าผาก
            new Seed(303.8, 124.6, SKIN), // ตา
            new Seed(279.3, 176.8, SKIN), // มือที่คาง
            //บริเวณแขน
            new Seed(392.6, 174.7, SKIN),
            new Seed(371.4, 122.3, SKIN),
            new Seed(330.1, 136.8, SKIN),
            new Seed(389.2, 127.9, SKIN),
            new Seed(296.7, 156.9, MOUTH),
            new Seed(238.6, 177.9, INSIDESHIRT), // ในเสื้อ
            new Seed(307.8, 172.5, INSIDESHIRT),
            new Seed(400.5, 292.5, SCREEN), // จอแท็บเล็ต
            new Seed(400.5, 311.0, SCREEN), // ไอคอนบนจอ
            new Seed(360.2, 301.9, STAR),
            new Seed(292.2, 130.1, TEAR),
            new Seed(284.4, 136.8, TEAR),
            new Seed(382.5, 310.8, HAT),

    };

    /** กำปั้นที่ชูขึ้นขยับระหว่างเฟรม จึงต้องมี seed ต่อเฟรม */
    private static final Seed[] CELEBRATING_FISTS = {};

    public static final Scene SCENE_3 = new Scene("happy",
            new String[] {
                    "test_drawSVG/c1.svg",
                    "test_drawSVG/c2.svg",
            },
            CELEBRATING, CELEBRATING_FISTS, ROOM_DAMS);
	
    private static final double[][] SAD_DAMS = {
            { 6.7, 630.5, -70.9, 660.7 },       
            { 461.5, 425, 461.5, -240 },    
            // { 564.3, 253.5, 577.7, 246.8 }, 
            {1197, 336, 1197, 385}, 
            { 550, 764.3, 1249.1, 768.8}     
            
    };

    private static final Seed[] SAD = {
            new Seed(1063.8, 135.9, BACKDROP),   // ผนัง - ต่อถึงกันทั้งซ้ายขวาผ่าน margin ด้านบน
            new Seed(107.0, 681.8, TABLE),       // โต๊ะ + เก้าอี้ (ต่อกันผ่าน margin ขวา)
            new Seed(421.2, 507.8, TABLE),       // แผงโต๊ะที่ยกขึ้นหลังคีย์บอร์ด
            new Seed(659.0, 135.0, HAIR),        // ผม
            new Seed(651.9, 295.7, SKIN),        // ใบหน้า
            new Seed(615.5, 245.1, DARKCIRCLE),        // เลนส์แว่น
            new Seed(757.5, 239.8, SHIRT),        // หู + ข้างคอ
            new Seed(546.5, 251.3, DARKCIRCLE),        // หู + ข้างคอ
            new Seed(559.9, 273.6, DARKCIRCLE),        // หู + ข้างคอ
            new Seed(700.4, 166.5, SKIN),        // หู + ข้างคอ
            new Seed(702.6, 177.7, SKIN),        // หู + ข้างคอ
            new Seed(702.6, 200.0, SKIN),        // หู + ข้างคอ
            new Seed(695.9, 168.8, SKIN),        // หู + ข้างคอ
            new Seed(811.7, 458.1, SHIRT),       // ลำตัว
            new Seed(910.2, 333.9, SHIRT),       // แขนขวาที่เท้าหลัง
            new Seed(695.4, 547.8, SHIRT),       // แขนซ้ายที่พิมพ์
            new Seed(991.0, 471.4, SHIRT),       // รอยจีบข้างลำตัว
            new Seed(1027.4, 433.3, SHIRT),      // รักแร้
            new Seed(913.8, 680.0, SHIRT),       // ชายเสื้อ
            new Seed(738.9, 523.8, INSIDESHIRT), // สาบเสื้อ
            new Seed(752.2, 645.4, BACKDROP),   // ขอบโต๊ะด้านซ้าย
            new Seed(710.5, 666.7, INSIDESHIRT),
            new Seed(682.8, 361.6, INSIDESHIRT), // ปกเสื้อใต้คาง
            new Seed(699.5, 376.2, INSIDESHIRT),
            new Seed(596.0, 581.5, SKIN),        // มือบนคีย์บอร์ด
            new Seed(494.8, 561.1, SKIN),        // นิ้ว
            new Seed(1010.5, 621.4, SKIN),       // มือที่เท้าหลัง
            new Seed(1038.9, 705.7, SHIRT),  
            new Seed(702.6, 474.3, SHIRT),
            new Seed(700.4, 496.6, SHIRT),
            new Seed(698.1, 505.6, SHIRT),
            new Seed(700.4, 719.7, PANTS),       // ขา  
            new Seed(1026.0, 440.9, CHAIR),       // เก้าอี้
            new Seed(1121.9, 597.0, CHAIR),       // เก้าอี้
            new Seed(1041.6, 755.4, CHAIR),

            // ---- จอมอนิเตอร์ ----
            new Seed(239.2, 402.2, MONITOR),     // แผงซ้ายของหน้าจอ
            new Seed(367.9, 232.7, MONITOR),     // แผงโค้ดขวา
            new Seed(107.8, 168.8, MONITOR),     // แถบเครื่องมือ
            new Seed(120.3, 146.6, MONITOR),     // แถบหัวหน้าต่าง
            new Seed(128.3, 191.8, MONITOR),     // แถบที่อยู่
            new Seed(287.1, 162.6, MONITOR),     // หัวแผงโค้ด
            new Seed(209.0, 485.6, MONITOR),     // แถบสถานะล่าง
            new Seed(122.9, 496.3, MONITOR),
            new Seed(182.8, 473.1, MONITOR),     // เศษพื้นจอที่เหลือ - ถ้าไม่เท จะเป็นรอยขาวบนจอมืด
            new Seed(282.8, 182.4, MONITOR),
            new Seed(413.0, 423.1, MONITOR),
            new Seed(192.2, 521.0, MONITOR),

            // แท่งกราฟบนจอ
            new Seed(130.0, 253.1, STAR),
            new Seed(154.0, 258.4, TEAR),
            new Seed(178.0, 368.5, MOUTH),
            new Seed(191.3, 353.4, STAR),
            new Seed(204.6, 345.4, TEAR),

            // ---- ตัวเครื่อง ----
            new Seed(120.3, 526.5, DEVICE),      // กรอบจอ
            new Seed(74.1, 149.2, DEVICE),       // ขอบจอด้านซ้าย
            new Seed(227.7, 546.0, DEVICE),      // คอขาตั้ง
            new Seed(177.1, 550.4, DEVICE),
            new Seed(272.9, 577.9, DEVICE),      // ฐานจอ
            new Seed(370.6, 575.3, DEVICE),
            new Seed(244.3, 611.6, DEVICE),
            new Seed(143.2, 561.6, DEVICE),      // มุมล่างซ้ายของกรอบจอ
            new Seed(288.0, 135.0, DEVICE),      // ตัวนาฬิกา
            new Seed(363.5, 649.8, DEVICE),      // คีย์บอร์ด
            new Seed(514.3, 652.5, DEVICE),
            new Seed(611.1, 526.5, DEVICE),      // เมาส์
            new Seed(580.0, 533.6, DEVICE),
    };

    private static final Seed[] SAD_PERFRAME = {};

    public static final Scene SCENE_4 = new Scene("sad",
            new String[] { "test_drawSVG/d.svg" },
            SAD, SAD_PERFRAME, SAD_DAMS);

    /** ลำดับการเล่น - จบซีนหนึ่งแล้วไปซีนถัดไป วนกลับมาซีนแรก */
    public static final Scene[] SCENES = { SCENE_1, SCENE_2, SCENE_3 , SCENE_4};

	public static final int[] SCENE_DURATION = {
		5000, // Scene 1 = 5 วินาที
		2000, // Scene 2 = 2 วินาที
		6000,  // Scene 3 = 6 วินาที - ต้องเท่ากับ KmitlBoardDrawable.totalTime() ของซีนนี้
		       //                      ซีนจะได้ตัดไป Scene 4 ตอนป้ายขาวเต็มจอพอดี
		4000,  // Scene 4 = 4 วินาที
	};

    /** เทสีลงเฉพาะพิกเซลขาวล้วน เส้นหมึกและสีที่เทไปแล้วกั้นอยู่ */
    public static final int BLANK = 0xFFFFFF;

    /** ทุกเฟรมใช้กรอบนี้ร่วมกัน ไม่ใช้ bounds ของแต่ละเฟรม */
    public static final double VBW = 600, VBH = 384, PAD = 24;
	public static final double VBW_SAD = 1200, VBH_SAD = 768;

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
        if (rgb == (TABLE.getRGB() & 0xFFFFFF))
            return "table";
        if (rgb == (INSIDESHIRT.getRGB() & 0xFFFFFF))
            return "insideshirt";
        if (rgb == (MOUTH.getRGB() & 0xFFFFFF))
            return "mouth";
        if (rgb == (TEAR.getRGB() & 0xFFFFFF))
            return "tear";
        if (rgb == (STAR.getRGB() & 0xFFFFFF))
            return "star";
        if (rgb == (HAT.getRGB() & 0xFFFFFF))
            return "hat";
        if (rgb == (MONITOR.getRGB() & 0xFFFFFF))
            return "monitor";
        if (rgb == (DEVICE.getRGB() & 0xFFFFFF))
            return "device";
        if (rgb == (PANTS.getRGB() & 0xFFFFFF))
            return "pants";
        return String.format("#%06X", rgb);
    }

}