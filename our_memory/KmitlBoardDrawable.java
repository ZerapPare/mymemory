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

    /** ขนาดป้ายที่ scale 1  */
    private static final int BOARD_W = 500;
    private static final int BOARD_H = 360;
    private static final int BOARD_ARC = 30;

    /** จำนวนรอบที่หมุนตอนเข้า */
    private static final int SPIN_TURNS = 10;

    private final List<Element> elements = new ArrayList<>();

    // ช่วงเวลาของแต่ละเฟส (วินาที) นับต่อกันไปเรื่อยๆ จากตอนเริ่มซีน
    private final double startDelay;   // รอ - ยังไม่วาด
    private final double spinIn;       // หมุนเข้า scale 0 -> 1
    private final double hold;         // หยุดให้อ่านทัน
    private final double zoomIn;       // พุ่งเข้าจนขาวเต็มจอ

    /**
     * ป้ายหยุดกลาง panel
     */
    private double targetX = 300.0;
    private double targetY = 300.0;

    /** App เรียกก่อน draw ทุกเฟรม  */
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

    /** ออกตัวเร็วแล้วค่อยๆ ช้า */
    private static double easeOut(double p) {
        return 1.0 - Math.pow(1.0 - p, 3);
    }

    /** ออกตัวช้าแล้วพุ่ง*/
    private static double easeIn(double p) {
        return p * p * p;
    }

    /**
     * สเกลที่ทำให้ป้ายขยายใหญ่
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
            // เฟส 1 หมุนเข้า 
            double p = easeOut(t / spinIn);
            boardScale = p;
            boardRotation = Math.toRadians(360.0 * SPIN_TURNS * p);
        } else if (t < spinIn + hold) {
            // เฟส 2 หยุดนิ่ง
            boardScale = 1.0;
        } else {
            // เฟส 3 พุ่งเข้าจนขาวเต็มจอ 
            zoomProgress = Math.min(1.0, (t - spinIn - hold) / zoomIn);
            boardScale = 1.0 + (coverScale() - 1.0) * easeIn(zoomProgress);
        }

        // พุ่งเปลี่ยนฉากเป็นสีทึบ
        int boardAlpha = (int) Math.round(240 + 15 * zoomProgress);

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

        // ตอนพุ่งตรากับข้อความขยายตามป้าย
        double inkAlpha = Math.max(0.0, 1.0 - zoomProgress * 1.5);
        if (inkAlpha <= 0) return;

        // วาด SVG แต่ละชิ้นโดยคำนวณจุดศูนย์กลางภาพให้อัตโนมัติ
        for (Element el : elements) {
            if (el.local == null) continue;

            // เลื่อนไปพิกัดเป้าหมาย ใส่สเกล/หมุน ดึงจุดศูนย์กลาง SVG มาทับพิกัด
            AffineTransform tx = new AffineTransform(board);
            tx.translate(el.x, el.y);
            tx.scale(el.scale, el.scale);
            tx.rotate(Math.toRadians(el.rotationDeg));
            tx.translate(-el.cx, -el.cy);

            // คูณเมทริกซ์ลงอาเรย์ที่ใช้ซ้ำ
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

    /** คูณ alpha เข้าไปในสี */
    private static int fade(Color c, double alpha) {
        int a = (int) Math.round(Math.max(0, Math.min(1, alpha)) * (c.getAlpha()));
        return (a << 24) | (c.getRGB() & 0xFFFFFF);
    }
}