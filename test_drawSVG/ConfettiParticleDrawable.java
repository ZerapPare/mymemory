import java.awt.*;
import java.awt.geom.AffineTransform;
import java.awt.geom.Path2D;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public class ConfettiParticleDrawable implements Drawable {

    private final List<Particle> particles = new ArrayList<>();
    private static final Color[] COLORS = {
        new Color(0xFF595E), new Color(0xFFCA3A), new Color(0x8AC926),
        new Color(0x1982C4), new Color(0x6A4C93), new Color(0xFF924C)
    };

    public ConfettiParticleDrawable() {
        Random rand = new Random(42); // ล็อก Seed ให้สุ่มได้รูปแบบสวยคงที่
        int count = 60; // จำนวนเม็ดพลุ

        for (int i = 0; i < count; i++) {
            particles.add(new Particle(
                rand.nextDouble(),                  // x ratio (0.0 - 1.0)
                rand.nextDouble() * 0.8,            // y offset
                rand.nextDouble() * 8 + 6,          // size
                COLORS[rand.nextInt(COLORS.length)],// color
                rand.nextDouble() * 1.5 + 0.8,      // speed
                rand.nextDouble() * Math.PI * 2,    // rotation speed
                rand.nextInt(2)                      // type: 0=rect, 1=ribbon
            ));
        }
    }

    @Override
    public void draw(Graphics2D g, double time) {
        Rectangle bounds = g.getClipBounds();
        int width = (bounds != null) ? bounds.width : 600;
        int height = (bounds != null) ? bounds.height : 600;

        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

        AffineTransform oldTx = g.getTransform();

        for (Particle p : particles) {
            // คำนวณตำแหน่งร่วงลงมาตามเวลา (วนลูปกลับไปข้างบนเมื่อพ้นขอบล่าง)
            double posY = ((p.yOffset + time * p.speed * 0.2) % 1.2) * height - (height * 0.1);
            double posX = (p.xRatio * width) + Math.sin(time * 3 + p.rotationSpeed) * 15; // ส่ายไปมา

            double angle = time * p.rotationSpeed;
            double flipScale = Math.cos(time * 4 + p.rotationSpeed); // เอฟเฟกต์หมุนพลิกใบ

            g.setColor(p.color);
            g.translate(posX, posY);
            g.rotate(angle);
            g.scale(1.0, flipScale); // พลิกมิติกระดาษ

            if (p.type == 0) {
                // เม็ดกระดาษสี่เหลี่ยม
                int s = (int) p.size;
                g.fillRect(-s / 2, -s / 2, s, s);
            } else {
                // ริบบิ้นโค้ง (ใช้ curveTo ตามเงื่อนไข)
                Path2D ribbon = new Path2D.Double();
                ribbon.moveTo(-p.size, -p.size / 2);
                ribbon.curveTo(-p.size / 2, p.size, p.size / 2, -p.size, p.size, p.size / 2);
                g.setStroke(new BasicStroke(3.0f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
                g.draw(ribbon);
            }

            g.setTransform(oldTx);
        }
    }

    // คลาสเก็บข้อมูลพลุแต่ละเม็ด
    private static class Particle {
        double xRatio, yOffset, size, speed, rotationSpeed;
        Color color;
        int type;

        Particle(double xRatio, double yOffset, double size, Color color, double speed, double rotationSpeed, int type) {
            this.xRatio = xRatio;
            this.yOffset = yOffset;
            this.size = size;
            this.color = color;
            this.speed = speed;
            this.rotationSpeed = rotationSpeed;
            this.type = type;
        }
    }
}