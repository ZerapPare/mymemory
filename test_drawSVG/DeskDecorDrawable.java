import java.awt.*;
import java.awt.geom.Path2D;

public class DeskDecorDrawable implements Drawable {

    @Override
    public void draw(Graphics2D g, double time) {
        Rectangle bounds = g.getClipBounds();
        int width = (bounds != null) ? bounds.width : 600;
        int height = (bounds != null) ? bounds.height : 600;

        // เปิด Anti-aliasing เพื่อความเนียน
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

        // 1. วาดแสงแดดเฉียงส่องจากหน้าต่างลงมาที่โต๊ะ
        drawSunlight(g, width, height);

        // 2. วาดแก้วกาแฟพร้อมควนลอย (ขยับตามเวลา time)
        drawCoffeeCup(g, width, height, time);
    }

    private void drawSunlight(Graphics2D g, int w, int h) {
        // ตำแหน่งหน้าต่างขวาบน
        int winX = w - (int)(w * 0.28) - 15;
        int winY = 15;
        int winW = (int)(w * 0.28);
        int winH = (int)(h * 0.32);

        // ลำแสงแดดสีขาวโปร่งแสง (Alpha = 25)
        g.setColor(new Color(255, 255, 200, 25));

        Path2D lightBeam = new Path2D.Double();
        lightBeam.moveTo(winX, winY + (winH * 0.3));             // จุดเริ่มบนซ้ายหน้าต่าง
        lightBeam.lineTo(winX + winW, winY + winH);              // มุมขวาล่างหน้าต่าง
        lightBeam.lineTo(winX - (w * 0.2), h);                   // พาดเฉียงลงมาพื้น/โต๊ะซ้าย
        lightBeam.lineTo(winX - (w * 0.4), h * 0.65);            // ปลายแสงด้านซ้าย
        lightBeam.closePath();

        g.fill(lightBeam);
    }

    private void drawCoffeeCup(Graphics2D g, int w, int h, double time) {
		int cupX = (int) (w * 0.40); 
		int cupY = (int) (h * 0.75); 

		int cupW = 42; 
		int cupH = 50;

		g.setColor(new Color(0x40000000, true));
		g.fillOval(cupX - 5, cupY + cupH - 5, cupW + 10, 14);

		g.setColor(new Color(0xF0F0F0));
		g.fillRoundRect(cupX, cupY, cupW, cupH, 10, 10);

		// เส้นขอบแก้ว (เพิ่มความหนาเส้นตามขนาด)
		g.setColor(new Color(0x332B25));
		g.setStroke(new BasicStroke(2.0f));
		g.drawRoundRect(cupX, cupY, cupW, cupH, 10, 10);

		// หูจับแก้ว (ปรับขนาดให้รับกับตัวแก้ว)
		g.drawArc(cupX + cupW - 4, cupY + 10, 14, 24, -90, 180);

		// C. ควันกาแฟ (ปรับระยะความสูงควันให้ลอยสูงขึ้น)
		g.setColor(new Color(200, 200, 200, 120));
		g.setStroke(new BasicStroke(2.5f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));

		for (int i = 0; i < 2; i++) {
			double offsetX = (i == 0) ? 12 : 26; // ขยับระยะห่างควัน
			double wave = Math.sin(time * 2.5 + i * 1.5) * 6; 

			Path2D steam = new Path2D.Double();
			steam.moveTo(cupX + offsetX, cupY - 6);
			steam.curveTo(
				cupX + offsetX + wave, cupY - 18,
				cupX + offsetX - wave, cupY - 30,
				cupX + offsetX + (wave / 2), cupY - 42 // ปรับให้ควันลอยสูงขึ้น
			);
			g.draw(steam);
		}
	}
}