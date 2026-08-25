import java.awt.*;

public class WindowBackgroundDrawable implements Drawable {

    @Override
    public void draw(Graphics2D g, double time) {
        Rectangle bounds = g.getClipBounds();
        int canvasWidth = (bounds != null) ? bounds.width : 600;
        int canvasHeight = (bounds != null) ? bounds.height : 600;

        drawWindowBackground(g, canvasWidth, canvasHeight);
    }

    private void drawWindowBackground(Graphics2D g, int canvasW, int canvasH) {
        // 1. กำหนดขนาดและตำแหน่งของหน้าต่าง (มุมขวาบน)
        int winW = (int) (canvasW * 0.28); // ขนาดความกว้างหน้าต่าง (45% ของหน้าจอ)
        int winH = (int) (canvasH * 0.32); // ขนาดความสูงหน้าต่าง (50% ของหน้าจอ)
        int winX = canvasW - winW - 15;     // ชิดขวา (เว้นระยะขอบ 15px)
        int winY = 15;                      // ชิดบน (เว้นระยะขอบ 15px)

        // กำหนดสี
        Color skyColor     = new Color(0xD8E8E2);
        Color frameColor   = new Color(0xDDAA66);
        Color sunColor     = new Color(0xF4A229);
        Color sunRayColor  = new Color(0x332B25);
        Color cloudColor   = new Color(0xF2FAF6);

        // Save State เดิมไว้ก่อนตัดพื้นที่
        Shape oldClip = g.getClip();

        // 2. วาดฉากหลังเฉพาะพื้นที่ภายในหน้าต่าง (ท้องฟ้า, เมฆ, พระอาทิตย์)
        g.setClip(winX, winY, winW, winH);

        // A. ท้องฟ้า
        g.setColor(skyColor);
        g.fillRect(winX, winY, winW, winH);

        // B. เมฆ
        g.setColor(cloudColor);
        g.fillOval(winX - (int)(winW * 0.1), winY + (int)(winH * 0.2), (int)(winW * 0.6), (int)(winH * 0.3));
        g.fillOval(winX + (int)(winW * 0.5), winY + (int)(winH * 0.6), (int)(winW * 0.6), (int)(winH * 0.3));

        // C. พระอาทิตย์และรัศมี
        int sunCx = winX + (int) (winW * 0.70);
        int sunCy = winY + (int) (winH * 0.30);
        int sunRadius = (int) (Math.min(winW, winH) * 0.18);

        // รัศมี
        g.setColor(sunRayColor);
        g.setStroke(new BasicStroke(2.0f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
        int rayCount = 10;
        int innerRay = sunRadius + 5;
        int outerRay = sunRadius + 18;

        for (int i = 0; i < rayCount; i++) {
            double angle = i * (2 * Math.PI / rayCount);
            int x1 = sunCx + (int) (Math.cos(angle) * innerRay);
            int y1 = sunCy + (int) (Math.sin(angle) * innerRay);
            int x2 = sunCx + (int) (Math.cos(angle) * outerRay);
            int y2 = sunCy + (int) (Math.sin(angle) * outerRay);
            g.drawLine(x1, y1, x2, y2);
        }

        // ดวงอาทิตย์
        g.setColor(sunColor);
        g.fillOval(sunCx - sunRadius, sunCy - sunRadius, sunRadius * 2, sunRadius * 2);

        // คืนค่า Clip ปกติ
        g.setClip(oldClip);

        // 3. วาดกรอบไม้หน้าต่าง (ขอบนอก + คานกากบาทกลาง)
        g.setColor(frameColor);
        int borderThickness = 8; // ความหนาขอบ
        int barThickness = 5;     // ความหนาซี่ไม้ตรงกลาง

        // กรอบ 4 ด้าน
        g.fillRect(winX, winY, winW, borderThickness);
        g.fillRect(winX, winY + winH - borderThickness, winW, borderThickness);
        g.fillRect(winX, winY, borderThickness, winH);
        g.fillRect(winX + winW - borderThickness, winY, borderThickness, winH);

        // คานกากบาทตรงกลาง
        int midX = winX + (winW / 2);
        int midY = winY + (winH / 2);
        g.fillRect(midX - (barThickness / 2), winY, barThickness, winH);
        g.fillRect(winX, midY - (barThickness / 2), winW, barThickness);

        // เส้นขอบสีเข้มสไตล์งานวาด
        g.setColor(sunRayColor);
        g.setStroke(new BasicStroke(1.5f));
        g.drawRect(winX, winY, winW, winH);
    }
}