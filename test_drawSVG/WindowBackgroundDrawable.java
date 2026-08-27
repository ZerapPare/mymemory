import java.awt.Color;

/**
 * หน้าต่างพร้อมท้องฟ้า เมฆ ดวงอาทิตย์ และกรอบไม้
 *
 * ท้องฟ้าข้างในถูกตัดด้วย clip ของ Raster แทน g.setClip() เดิม
 */
public class WindowBackgroundDrawable implements Drawable {

    @Override
    public void draw(Raster r, double time) {
        // time ไม่ใช้ - หน้าต่างไม่ขยับ แต่ต้องรับตาม interface
        drawWindowBackground(r, r.w, r.h);
    }

    private void drawWindowBackground(Raster r, int canvasW, int canvasH) {
        // 1. กำหนดขนาดและตำแหน่งของหน้าต่าง (มุมขวาบน)
        int winW = (int) (canvasW * 0.28);
        int winH = (int) (canvasH * 0.32);
        int winX = canvasW - winW - 15;
        int winY = 15;

        int sky      = Raster.argb(new Color(0xD8E8E2));
        int frame    = Raster.argb(new Color(0xDDAA66));
        int sun      = Raster.argb(new Color(0xF4A229));
        int sunRay   = Raster.argb(new Color(0x332B25));
        int cloud    = Raster.argb(new Color(0xF2FAF6));

        // 2. ฉากหลังในหน้าต่าง - จำกัดพื้นที่เขียนด้วย clip ของ Raster
        int[] saved = r.saveClip();
        r.clip(winX, winY, winW, winH);

        // A. ท้องฟ้า
        Gfx.scanlineFill(r, Gfx.rect(winX, winY, winW, winH), null, sky);

        // B. เมฆ - วงรีสองก้อน (พิกัดเดิมเป็นมุมซ้ายบน ต้องแปลงเป็นจุดศูนย์กลาง)
        oval(r, winX - (int) (winW * 0.1), winY + (int) (winH * 0.2),
                (int) (winW * 0.6), (int) (winH * 0.3), cloud);
        oval(r, winX + (int) (winW * 0.5), winY + (int) (winH * 0.6),
                (int) (winW * 0.6), (int) (winH * 0.3), cloud);

        // C. ดวงอาทิตย์และรัศมี
        int sunCx = winX + (int) (winW * 0.70);
        int sunCy = winY + (int) (winH * 0.30);
        int sunRadius = (int) (Math.min(winW, winH) * 0.18);

        int rayCount = 10;
        int innerRay = sunRadius + 5;
        int outerRay = sunRadius + 18;
        for (int i = 0; i < rayCount; i++) {
            double angle = i * (2 * Math.PI / rayCount);
            Gfx.thickLine(r,
                    sunCx + Math.cos(angle) * innerRay, sunCy + Math.sin(angle) * innerRay,
                    sunCx + Math.cos(angle) * outerRay, sunCy + Math.sin(angle) * outerRay,
                    2, sunRay);
        }

        Gfx.fillEllipse(r, sunCx, sunCy, sunRadius, sunRadius, sun);

        r.restoreClip(saved);

        // 3. กรอบไม้หน้าต่าง (ขอบนอก + คานกากบาทกลาง)
        int borderThickness = 8;
        int barThickness = 5;

        Gfx.scanlineFill(r, Gfx.rect(winX, winY, winW, borderThickness), null, frame);
        Gfx.scanlineFill(r, Gfx.rect(winX, winY + winH - borderThickness, winW, borderThickness), null, frame);
        Gfx.scanlineFill(r, Gfx.rect(winX, winY, borderThickness, winH), null, frame);
        Gfx.scanlineFill(r, Gfx.rect(winX + winW - borderThickness, winY, borderThickness, winH), null, frame);

        int midX = winX + (winW / 2);
        int midY = winY + (winH / 2);
        Gfx.scanlineFill(r, Gfx.rect(midX - (barThickness / 2), winY, barThickness, winH), null, frame);
        Gfx.scanlineFill(r, Gfx.rect(winX, midY - (barThickness / 2), winW, barThickness), null, frame);

        // เส้นขอบสีเข้มสไตล์งานวาด
        Gfx.polyline(r, Gfx.rect(winX, winY, winW, winH), true, 1.5, sunRay);
    }

    /** วงรีที่ระบุด้วยกรอบสี่เหลี่ยมแบบ fillOval เดิม */
    private static void oval(Raster r, int x, int y, int w, int h, int argb) {
        Gfx.fillEllipse(r, x + w / 2, y + h / 2, w / 2, h / 2, argb);
    }
}
