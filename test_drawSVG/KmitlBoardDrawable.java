import java.awt.*;
import java.awt.geom.AffineTransform;
import java.awt.geom.Path2D;
import java.awt.geom.Rectangle2D;
import java.util.ArrayList;
import java.util.List;

public class KmitlBoardDrawable implements Drawable {

    private static class Element {
        double x, y, scale, rotationDeg;
        Color fillColor, strokeColor;

        /**
         * คอนทัวร์ในพิกัดของตัว SVG เอง แกะไว้ครั้งเดียวตอนโหลด
         *
         * ป้ายนี้วาดทุก 30ms ถ้าเรียก contours() ใหม่ทุกเฟรมจะเสียเวลาเดิน
         * PathIterator กับก็อป path ซ้ำ ๆ (ตราอย่างเดียวมีจุดแสนสามหมื่น)
         * เก็บไว้แล้วแค่คูณเมทริกซ์ลงอาเรย์ที่ใช้ซ้ำก็พอ
         *
         * ไม่เก็บ Path2D ต้นทางไว้ ใช้เสร็จในคอนสตรัคเตอร์แล้วปล่อยเลย
         */
        Gfx.Contours local;
        double cx, cy;
        double[] screen;

        Element(String svgPath, double x, double y, double scale, double rotationDeg, Color fill, Color stroke) {
            this.x = x;
            this.y = y;
            this.scale = scale;
            this.rotationDeg = rotationDeg;
            this.fillColor = fill;
            this.strokeColor = stroke;

            Path2D path = SvgLoader.loadSvg(svgPath);
            if (path != null) {
                Rectangle2D b = path.getBounds2D();
                this.cx = b.getCenterX();
                this.cy = b.getCenterY();
                this.local = Gfx.contours(path);
                this.screen = new double[local.pts.length];
            }
        }
    }

    /** ขนาดป้ายที่ scale 1 (พิกเซลจอ) วัดจากกึ่งกลางป้าย -> -250..250 x -180..180 */
    private static final int BOARD_W = 500;
    private static final int BOARD_H = 360;
    private static final int BOARD_ARC = 30;

    /** จำนวนรอบที่หมุนตอนเข้า - ต้องเป็นจำนวนเต็ม ป้ายจะได้จบที่ 0 องศาพอดี */
    private static final int SPIN_TURNS = 10;

    private final List<Element> elements = new ArrayList<>();

    // ช่วงเวลาของแต่ละเฟส (วินาที) นับต่อกันไปเรื่อยๆ จากตอนเริ่มซีน
    private final double startDelay;   // รอ - ยังไม่วาด
    private final double spinIn;       // หมุนเข้า scale 0 -> 1
    private final double hold;         // หยุดนิ่งให้อ่านทัน
    private final double zoomIn;       // พุ่งเข้าหาคนดูจนขาวเต็มจอ

    /**
     * จุดที่ป้ายจะไปหยุด = กึ่งกลาง panel จริง (พิกเซลจอ ไม่ใช่หน่วย viewBox)
     * ค่าตั้งต้นเป็นกึ่งกลางหน้าต่างขนาดออกแบบ 600x600 เผื่อกรณียังไม่ได้เรียก setPanelSize
     */
    private double targetX = 300.0;
    private double targetY = 300.0;

    /** App เรียกก่อน draw ทุกเฟรม ป้ายจะได้อยู่กลางจอไม่ว่าย่อ/ขยายหน้าต่าง */
    public void setPanelSize(int w, int h) {
        if (w > 0) this.targetX = w / 2.0;
        if (h > 0) this.targetY = h / 2.0;
    }

    public KmitlBoardDrawable(double startDelay, double spinIn, double hold, double zoomIn) {
        this.startDelay = startDelay;
        this.spinIn = Math.max(0.001, spinIn);
        this.hold = Math.max(0.0, hold);
        this.zoomIn = Math.max(0.001, zoomIn);

        // 1. ตราสัญลักษณ์ KMITL
        elements.add(new Element(
                ArtConfig.SVG_DIR + "kmitl.svg",
                100, 30,
                0.40,
                0,
                ArtConfig.SEAL,
                ArtConfig.SEAL_EDGE
        ));

        // 2. ข้อความ 1
        elements.add(new Element(
                ArtConfig.SVG_DIR + "text1.svg",
                0, -125,
                0.2,
                0,
                ArtConfig.BOARD_TEXT,
                ArtConfig.BOARD_TEXT
        ));

        // 3. ข้อความ 2
        elements.add(new Element(
                ArtConfig.SVG_DIR + "text2.svg",
                -110, 20,
                0.45,
                0,
                ArtConfig.BOARD_TEXT,
                ArtConfig.BOARD_TEXT
        ));
    }

    /** ออกตัวเร็วแล้วค่อยๆ นิ่ง - ใช้ตอนหมุนเข้า จะได้รู้สึกว่า "หยุด" จริง */
    private static double easeOut(double p) {
        return 1.0 - Math.pow(1.0 - p, 3);
    }

    /** ออกตัวช้าแล้วพุ่ง - ใช้ตอนซูมเข้าหาคนดู */
    private static double easeIn(double p) {
        return p * p * p;
    }

    /**
     * สเกลที่ทำให้ป้ายกินพื้นที่เกิน panel ทุกด้าน (คูณ 2 เผื่อให้มุมโค้งหลุดออกนอกจอ)
     * ที่สเกลนี้จอจะเป็นสีขาวล้วน ใช้กลบรอยตัดไปซีนถัดไป
     */
    private double coverScale() {
        double panelW = targetX * 2.0;
        double panelH = targetY * 2.0;
        return 2.0 * Math.max(panelW / BOARD_W, panelH / BOARD_H);
    }

    @Override
    public void draw(Raster r, double relativeTime) {
        double t = relativeTime - startDelay;
        if (t < 0) return;

        double boardScale;
        double boardRotation = 0;
        double zoomProgress = 0;

        if (t < spinIn) {
            // เฟส 1 หมุนเข้า - จบที่ scale 1 และ 0 องศาพอดี (SPIN_TURNS เป็นจำนวนเต็ม)
            double p = easeOut(t / spinIn);
            boardScale = p;
            boardRotation = Math.toRadians(360.0 * SPIN_TURNS * p);
        } else if (t < spinIn + hold) {
            // เฟส 2 หยุดนิ่ง
            boardScale = 1.0;
        } else {
            // เฟส 3 พุ่งเข้าหาคนดูจนขาวเต็มจอ แล้วค้างไว้ให้ซีนตัดทับ
            zoomProgress = Math.min(1.0, (t - spinIn - hold) / zoomIn);
            boardScale = 1.0 + (coverScale() - 1.0) * easeIn(zoomProgress);
        }

        // ปกติป้ายโปร่งนิดๆ (240) แต่ตอนพุ่งต้องทึบสนิทถึงจะกลบรอยตัดซีนได้
        int boardAlpha = (int) Math.round(240 + 15 * zoomProgress);

        // ไม่มีสแตกทรานส์ฟอร์มของ Graphics2D ให้ใช้แล้ว - คูณเมทริกซ์เองแล้วแปลง
        // พิกัดจุดก่อนส่งเข้า Gfx (AffineTransform ใช้ได้ ถือเป็นคณิตศาสตร์ล้วน)
        AffineTransform board = new AffineTransform();
        board.translate(targetX, targetY);
        board.scale(boardScale, boardScale);
        board.rotate(boardRotation);

        double[] frame = Gfx.roundRect(-BOARD_W / 2.0, -BOARD_H / 2.0,
                BOARD_W, BOARD_H, BOARD_ARC, BOARD_ARC);
        double[] screenFrame = apply(board, frame);

        Gfx.scanlineFill(r, screenFrame, null,
                (boardAlpha << 24) | (ArtConfig.BOARD_FACE.getRGB() & 0xFFFFFF));
        Gfx.polyline(r, screenFrame, true, 5.0 * boardScale,
                Raster.argb(ArtConfig.BOARD_BORDER));

        // ตอนพุ่ง ตรากับข้อความขยายตามป้ายไปด้วย ถ้าไม่จางหายมันจะบังจนเต็มจอ
        // แทนที่จะเหลือสีขาว - จางหมดตอนพุ่งไปได้ราวสองในสามของเฟส
        double inkAlpha = Math.max(0.0, 1.0 - zoomProgress * 1.5);
        if (inkAlpha <= 0) return;

        // ===================================================
        // วาด SVG แต่ละชิ้นโดยคำนวณจุดศูนย์กลางภาพให้อัตโนมัติ
        // ===================================================
        for (Element el : elements) {
            if (el.local == null) continue;

            // เลื่อนไปพิกัดเป้าหมาย -> ใส่สเกล/หมุน -> ดึงจุดศูนย์กลาง SVG มาทับพิกัด
            AffineTransform tx = new AffineTransform(board);
            tx.translate(el.x, el.y);
            tx.scale(el.scale, el.scale);
            tx.rotate(Math.toRadians(el.rotationDeg));
            tx.translate(-el.cx, -el.cy);

            // คูณเมทริกซ์ลงอาเรย์ที่ใช้ซ้ำ ไม่ก็อป path ใหม่ทุกเฟรม
            tx.transform(el.local.pts, 0, el.screen, 0, el.local.pts.length / 2);
            Gfx.Contours c = new Gfx.Contours(el.screen, el.local.ends);

            if (el.fillColor != null) {
                Gfx.scanlineFill(r, c.pts, c.ends, fade(el.fillColor, inkAlpha));
            }
            if (el.strokeColor != null) {
                Gfx.strokeContours(r, c, 1.5 * boardScale * el.scale,
                        fade(el.strokeColor, inkAlpha));
            }
        }

    }

    /** แปลงลิสต์จุดทั้งชุดด้วยเมทริกซ์เดียว */
    private static double[] apply(AffineTransform tx, double[] pts) {
        double[] out = new double[pts.length];
        tx.transform(pts, 0, out, 0, pts.length / 2);
        return out;
    }

    /** คูณ alpha เข้าไปในสี - มาแทน AlphaComposite ตอนตรากับข้อความจางหาย */
    private static int fade(Color c, double alpha) {
        int a = (int) Math.round(Math.max(0, Math.min(1, alpha)) * (c.getAlpha()));
        return (a << 24) | (c.getRGB() & 0xFFFFFF);
    }
}