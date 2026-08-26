import java.awt.*;
import java.awt.geom.AffineTransform;
import java.awt.geom.Path2D;
import java.awt.geom.Rectangle2D;
import java.util.ArrayList;
import java.util.List;

public class KmitlBoardDrawable implements Drawable {

    private static class Element {
        Path2D path;
        double x, y, scale, rotationDeg;
        Color fillColor, strokeColor;

        Element(String svgPath, double x, double y, double scale, double rotationDeg, Color fill, Color stroke) {
            this.path = SvgLoader.loadSvg(svgPath);
            this.x = x;
            this.y = y;
            this.scale = scale;
            this.rotationDeg = rotationDeg;
            this.fillColor = fill;
            this.strokeColor = stroke;
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

    /**
     * เวลารวมทั้งอนิเมชัน (วินาที) - `ArtConfig.SCENE_DURATION` ของซีนนี้
     * ต้องยาวอย่างน้อยเท่านี้ ไม่งั้นซีนจะตัดหนีไปก่อนป้ายจะขาวเต็มจอ
     */
    public double totalTime() {
        return startDelay + spinIn + hold + zoomIn;
    }

    public KmitlBoardDrawable(double startDelay, double spinIn, double hold, double zoomIn) {
        this.startDelay = startDelay;
        this.spinIn = Math.max(0.001, spinIn);
        this.hold = Math.max(0.0, hold);
        this.zoomIn = Math.max(0.001, zoomIn);

        // 1. ตราสัญลักษณ์ KMITL
        elements.add(new Element(
                "test_drawSVG/kmitl.svg",
                100, 30,
                0.40,
                0,
                new Color(0xFF6600),
                Color.ORANGE
        ));

        // 2. ข้อความ 1
        elements.add(new Element(
                "test_drawSVG/text1.svg",
                0, -125,
                0.2,
                0,
                Color.BLACK,
                Color.BLACK
        ));

        // 3. ข้อความ 2
        elements.add(new Element(
                "test_drawSVG/text2.svg",
                -110, 20,
                0.45,
                0,
                Color.BLACK,
                Color.BLACK
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
    public void draw(Graphics2D g2, double relativeTime) {
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

        Graphics2D g = (Graphics2D) g2.create();
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

        // เลื่อนแกนไปที่จุดศูนย์กลางจอ
        g.translate(targetX, targetY);
        g.scale(boardScale, boardScale);
        g.rotate(boardRotation);

        g.setColor(new Color(255, 255, 255, boardAlpha));
        g.fillRoundRect(-BOARD_W / 2, -BOARD_H / 2, BOARD_W, BOARD_H, BOARD_ARC, BOARD_ARC);
        g.setColor(new Color(0xFF6600));
        g.setStroke(new BasicStroke(5.0f));
        g.drawRoundRect(-BOARD_W / 2, -BOARD_H / 2, BOARD_W, BOARD_H, BOARD_ARC, BOARD_ARC);

        // ตอนพุ่ง ตรากับข้อความขยายตามป้ายไปด้วย ถ้าไม่จางหายมันจะบังจนเต็มจอ
        // แทนที่จะเหลือสีขาว - จางหมดตอนพุ่งไปได้ราวสองในสามของเฟส
        float inkAlpha = (float) Math.max(0.0, 1.0 - zoomProgress * 1.5);
        if (inkAlpha <= 0f) {
            g.dispose();
            return;
        }
        g.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, inkAlpha));

        // ===================================================
        // วาด SVG แต่ละชิ้นโดยคำนวณจุดศูนย์กลางภาพให้อัตโนมัติ
        // ===================================================
        for (Element el : elements) {
            if (el.path == null) continue;

            AffineTransform oldTx = g.getTransform();

            // หาจุดศูนย์กลางของตัว SVG เอง (Bounds)
            Rectangle2D bounds = el.path.getBounds2D();
            double centerX = bounds.getCenterX();
            double centerY = bounds.getCenterY();

            // เลื่อนไปพิกัดเป้าหมาย -> ใส่สเกล/หมุน -> ดึงจุดศูนย์กลาง SVG มาทับพิกัด
            g.translate(el.x, el.y);
            g.scale(el.scale, el.scale);
            g.rotate(Math.toRadians(el.rotationDeg));
            g.translate(-centerX, -centerY);

            // เทสีพื้น
            if (el.fillColor != null) {
                g.setColor(el.fillColor);
                g.fill(el.path);
            }

            // วาดเส้นขอบ
            if (el.strokeColor != null) {
                g.setColor(el.strokeColor);
                g.setStroke(new BasicStroke(1.5f));
                g.draw(el.path);
            }

            g.setTransform(oldTx);
        }

        g.dispose();
    }
}