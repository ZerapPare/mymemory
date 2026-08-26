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

    private final List<Element> elements = new ArrayList<>();
    private final double startDelay;
    private final double duration;
    
    // พิกัดจุดศูนย์กลางหน้าจอ ( viewBox คือ 600x384 -> ศูนย์กลางคือ 300, 192 )
    private final double targetX = 300.0;
    private final double targetY = 192.0;

    public KmitlBoardDrawable(double startDelay, double duration) {
        this.startDelay = startDelay;
        this.duration = duration;

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
                -100, -20,
                0.45,
                0,
                Color.BLACK,
                Color.BLACK
        ));
    }

    @Override
    public void draw(Graphics2D g2, double relativeTime) {
        if (relativeTime < startDelay) return;

        double progress = Math.min(1.0, (relativeTime - startDelay) / duration);
        
        // หมุน 4 รอบ พร้อมขยายขนาด
        double boardScale = progress; 
        double boardRotation = Math.toRadians(360 * 4 * progress);

        Graphics2D g = (Graphics2D) g2.create();
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

        // เลื่อนแกนไปที่จุดศูนย์กลางจอ
        g.translate(targetX, targetY);
        g.scale(boardScale, boardScale);
        g.rotate(boardRotation);

        g.setColor(new Color(255, 255, 255, 240));
        g.fillRoundRect(-250, -180, 500, 360, 30, 30);
        g.setColor(new Color(0xFF6600));
        g.setStroke(new BasicStroke(5.0f));
        g.drawRoundRect(-250, -180, 500, 360, 30, 30);
        

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