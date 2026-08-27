/**
 * หน้าต่าง
 */
public class WindowBackgroundDrawable implements Drawable {

    @Override
    public void draw(Raster r, double time) {
        drawWindowBackground(r, r.w, r.h);
    }

    private void drawWindowBackground(Raster r, int canvasW, int canvasH) {
        // 1. กำหนดขนาดและตำแหน่งของหน้าต่าง 
        int winW = (int) (canvasW * 0.28);
        int winH = (int) (canvasH * 0.32);
        int winX = canvasW - winW - 15;
        int winY = 15;

        int sky      = Raster.argb(ArtConfig.SKY);
        int frame    = Raster.argb(ArtConfig.WINDOW_FRAME);
        int sun      = Raster.argb(ArtConfig.SUN);
        int sunRay   = Raster.argb(ArtConfig.PROP_INK);
        int cloud    = Raster.argb(ArtConfig.CLOUD);

        // 2. ฉากหลังในหน้าต่าง 
        int[] saved = r.saveClip();
        r.clip(winX, winY, winW, winH);

        // A. ท้องฟ้า
        Gfx.scanlineFill(r, Gfx.rect(winX, winY, winW, winH), null, sky);

        // B. เมฆ 
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

        // 3. กรอบไม้หน้าต่าง
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

        Gfx.polyline(r, Gfx.rect(winX, winY, winW, winH), true, 1.5, sunRay);
    }

    private static void oval(Raster r, int x, int y, int w, int h, int argb) {
        Gfx.fillEllipse(r, x + w / 2, y + h / 2, w / 2, h / 2, argb);
    }
}
