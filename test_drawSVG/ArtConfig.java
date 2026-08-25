import java.awt.Color;

/**
 * Data / Configuration
 *
 * ทุกอย่างที่ "ปรับแต่งได้" อยู่ในไฟล์นี้ไฟล์เดียว - สี, จุดเทสี, เส้นอุด,
 * กรอบภาพ, รายชื่อไฟล์, จังหวะเวลา ไฟล์อื่นเป็นกลไกล้วนๆ
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

	public static final String[] SCENE_2_FILES = {
		"test_drawSVG/c1.svg",
		"test_drawSVG/c2.svg",
	};

    public static final int FRAME_INTERVAL = 350; // ms ต่อเฟรม
    public static final int TICK_INTERVAL = 30;   // ms ต่อการวาด

    // ------ สีที่ใช้ใน SVG ------
    public static final Color BACKDROP = new Color(0xDCEBFF);
    public static final Color TABLE    = new Color(0xF2E4CB);
    public static final Color HAIR     = new Color(0x6B4A2E);
    public static final Color SKIN     = new Color(0xFFDCB8);
    public static final Color SHIRT    = new Color(0x7FA8D4);
    public static final Color SCREEN   = new Color(0xDCEBFF);

    /** จุดเริ่มเทสีหนึ่งจุด พิกัดเป็นหน่วย viewBox ไม่ใช่พิกเซลจอ */
    public static class Seed {
        public final double x, y;
        public final Color color;
        public Seed(double x, double y, Color color) { this.x = x; this.y = y; this.color = color; }
    }

    public static final Seed[] COMMON = {
        new Seed( 30.0,  30.0, BACKDROP),
        new Seed( 218.6, 413.4, TABLE),
        new Seed(229.8, 207.6, SHIRT),
        new Seed(301.0,  75.5, HAIR),
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
    };

    public static final Seed[] SCREEN_PER_FRAME = {
        new Seed(411.7, 289.3, SCREEN),
        new Seed(365.4, 323.6, SCREEN),
        new Seed(404.5, 294.9, SCREEN),
        new Seed(411.7, 289.3, SCREEN),
        new Seed(365.4, 323.6, SCREEN),
        new Seed(404.5, 294.9, SCREEN),
    };

    /** เส้นดำที่ลากเพิ่มเองเพื่ออุดช่องที่ลายเส้นต้นฉบับเปิดค้างไว้ {x1,y1,x2,y2} */
    public static final double[][] DAMS = {
        { 149.4, 355.4, -37.4, 456.5 },
        {485.1, 238.3, 659.5, 262.5}
    };

    public static final double DAM_WIDTH = 1.0;

    /** เทสีลงเฉพาะพิกเซลขาวล้วน เส้นหมึกและสีที่เทไปแล้วกั้นอยู่ */
    public static final int BLANK = 0xFFFFFF;

    /** ทุกเฟรมใช้กรอบนี้ร่วมกัน ไม่ใช้ bounds ของแต่ละเฟรม */
    public static final double VBW = 600, VBH = 384, PAD = 24;

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
