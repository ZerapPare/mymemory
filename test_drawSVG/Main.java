import javax.swing.*;
import java.awt.*;
import java.awt.geom.AffineTransform;
import java.awt.geom.Path2D;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public class Main {

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            JFrame frame = new JFrame("Test Confetti Particle Only");
            frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
            frame.setSize(800, 800);
            frame.setLocationRelativeTo(null);

            // สร้าง Panel ทดสอบเฉพาะพลุ
            ConfettiTestPanel testPanel = new ConfettiTestPanel();
            frame.add(testPanel);
            frame.setVisible(true);

            // Timer ลูป 60 FPS ขยับเม็ดพลุ
            Timer timer = new Timer(16, e -> testPanel.repaint());
            timer.start();
        });
    }
}

// Panel สำหรับทดสอบพลุเพียวๆ
class ConfettiTestPanel extends JPanel {
    private final List<Particle> particles = new ArrayList<>();
    private static final Color[] COLORS = {
        new Color(0xFF595E), new Color(0xFFCA3A), new Color(0x8AC926),
        new Color(0x1982C4), new Color(0x6A4C93), new Color(0xFF924C)
    };

    public ConfettiTestPanel() {
        setBackground(new Color(0x222222)); // พื้นหลังสีเทาดำเพื่อให้เห็นพลุชัดเจน
        Random rand = new Random();
        
        // สร้างเม็ดพลุ 80 เม็ด
        for (int i = 0; i < 80; i++) {
            particles.add(new Particle(
                rand.nextDouble(),                  // x Ratio (0.0 - 1.0)
                rand.nextDouble() * 0.8,            // y Offset
                rand.nextDouble() * 8 + 6,          // ขนาดเม็ด
                COLORS[rand.nextInt(COLORS.length)],// สุ่มสี
                rand.nextDouble() * 1.5 + 0.8,      // ความเร็วร่วง
                rand.nextDouble() * Math.PI * 2,    // ความเร็วหมุน
                rand.nextInt(2)                      // 0=สี่เหลี่ยม, 1=ริบบิ้น curveTo
            ));
        }
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        Graphics2D g2 = (Graphics2D) g;

        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

        int width = getWidth();
        int height = getHeight();
        double time = System.currentTimeMillis() / 1000.0;
        AffineTransform oldTx = g2.getTransform();

        // วาดเม็ดพลุทีละเม็ด
        for (Particle p : particles) {
            double posY = ((p.yOffset + time * p.speed * 0.2) % 1.2) * height - (height * 0.1);
            double posX = (p.xRatio * width) + Math.sin(time * 3 + p.rotationSpeed) * 15;

            double angle = time * p.rotationSpeed;
            double flipScale = Math.cos(time * 4 + p.rotationSpeed); // เอฟเฟกต์หมุนพลิกใบ

            g2.setColor(p.color);
            g2.translate(posX, posY);
            g2.rotate(angle);
            g2.scale(1.0, flipScale);

            if (p.type == 0) {
                // สี่เหลี่ยม
                int s = (int) p.size;
                g2.fillRect(-s / 2, -s / 2, s, s);
            } else {
                // ริบบิ้น (ใช้ curveTo)
                Path2D ribbon = new Path2D.Double();
                ribbon.moveTo(-p.size, -p.size / 2);
                ribbon.curveTo(-p.size / 2, p.size, p.size / 2, -p.size, p.size, p.size / 2);
                g2.setStroke(new BasicStroke(3.0f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
                g2.draw(ribbon);
            }

            g2.setTransform(oldTx);
        }

        // ตัวอักษรบอกสถานะ
        g2.setColor(Color.WHITE);
        g2.setFont(new Font("SansSerif", Font.BOLD, 18));
        g2.drawString("Confetti Particle Test (Running...)", 20, 40);
    }

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