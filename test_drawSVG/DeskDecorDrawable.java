/**
 * ของบนโต๊ะ - ลำแสงแดดเฉียง กับแก้วกาแฟที่มีควันลอย
 *
 * ลำแสงกับควันใช้สีโปร่ง ซึ่ง Raster.plot() ผสม alpha ให้เอง
 * สีมาจาก ArtConfig ที่เดียว
 */
public class DeskDecorDrawable implements Drawable {

    @Override
    public void draw(Raster r, double time) {
        drawSunlight(r, r.w, r.h);
        drawCoffeeCup(r, r.w, r.h, time);
    }

    private void drawSunlight(Raster r, int w, int h) {
        // ตำแหน่งหน้าต่างขวาบน ต้องตรงกับ WindowBackgroundDrawable
        int winX = w - (int) (w * 0.28) - 15;
        int winY = 15;
        int winW = (int) (w * 0.28);
        int winH = (int) (h * 0.32);

        // ลำแสงแดดสีขาวโปร่งแสง (alpha 25)
        double[] beam = {
                winX, winY + (winH * 0.3),          // จุดเริ่มบนซ้ายหน้าต่าง
                winX + winW, winY + winH,           // มุมขวาล่างหน้าต่าง
                winX - (w * 0.2), h,                // พาดเฉียงลงมาพื้น/โต๊ะซ้าย
                winX - (w * 0.4), h * 0.65,         // ปลายแสงด้านซ้าย
        };
        Gfx.scanlineFill(r, beam, null, Raster.argb(ArtConfig.SUNBEAM));
    }

    private void drawCoffeeCup(Raster r, int w, int h, double time) {
        int cupX = (int) (w * 0.40);
        int cupY = (int) (h * 0.75);
        int cupW = 42;
        int cupH = 50;

        // เงาใต้แก้ว
        Gfx.fillEllipse(r, cupX - 5 + (cupW + 10) / 2, cupY + cupH - 5 + 7,
                (cupW + 10) / 2, 7, Raster.argb(ArtConfig.CUP_SHADOW));

        double[] cup = Gfx.roundRect(cupX, cupY, cupW, cupH, 10, 10);
        Gfx.scanlineFill(r, cup, null, Raster.argb(ArtConfig.CUP));

        int rim = Raster.argb(ArtConfig.PROP_INK);
        Gfx.polyline(r, cup, true, 2, rim);

        // หูจับแก้ว - ส่วนโค้งครึ่งวงทางขวา ตรงกับ drawArc(-90, 180) เดิม
        Gfx.polyline(r, Gfx.arcPoints(cupX + cupW - 4 + 7, cupY + 10 + 12, 7, 12, -90, 180),
                false, 2, rim);

        // ควันกาแฟ - Bézier กำลังสามที่ส่ายตามเวลา
        int steamColor = Raster.argb(ArtConfig.STEAM);
        for (int i = 0; i < 2; i++) {
            double offsetX = (i == 0) ? 12 : 26;
            double wave = Math.sin(time * 2.5 + i * 1.5) * 6;

            double[] steam = Gfx.bezier3(
                    cupX + offsetX, cupY - 6,
                    cupX + offsetX + wave, cupY - 18,
                    cupX + offsetX - wave, cupY - 30,
                    cupX + offsetX + (wave / 2), cupY - 42, 20);
            Gfx.polyline(r, steam, false, 2.5, steamColor);
        }
    }
}
