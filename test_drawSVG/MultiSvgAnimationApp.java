import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.geom.AffineTransform;
import java.awt.geom.Path2D;
import java.awt.image.BufferedImage;
import java.io.File;
import java.util.ArrayList;
import java.util.List;

/**
 * Swing UI / Presentation
 *
 * ประกอบสามส่วนเข้าด้วยกัน: SvgLoader อ่านไฟล์ -> FillEngine ลงสี -> แปะลงจอ
 * แล้ววาด Drawable ทับ พร้อมปุ่มควบคุมและเครื่องมือเก็บพิกัด
 *
 * สี พิกัด และค่าคงที่ทั้งหมดอยู่ใน ArtConfig ไม่ได้อยู่ในไฟล์นี้
 */
public class MultiSvgAnimationApp extends JPanel {

    /** ข้อมูลของแต่ละเฟรม (SVG + Drawable objects) */
    private static class FrameData {
        Path2D svgPath;
        List<Drawable> drawables = new ArrayList<>();
        FrameData(Path2D path) {
            this.svgPath = path;
        }
    }

    private final List<FrameData> frames = new ArrayList<>();
    private final List<String> names = new ArrayList<>();

    /** พื้นหลังที่เรนเดอร์แล้ว (Cache) หนึ่งใบต่อเฟรม */
    private final List<BufferedImage> backgrounds = new ArrayList<>();
    private int lastW = -1, lastH = -1;
    private boolean colorOn = true;

    private int currentIndex = 0;
    private Timer timer;
    private boolean paused = false;
    private int frameCounter = 0;        // นับจำนวน tick

    private String pickText = "click to pick a point";

    // =================== Constructor ===================
    public MultiSvgAnimationApp(String[] filePaths) {
        for (String filePath : filePaths) {
            Path2D path = SvgLoader.loadSvg(filePath);
            if (path != null) {
                FrameData fd = new FrameData(path);
                // ***** เพิ่ม Drawable ต่าง ๆ ลงในเฟรมนี้ *****
                fd.drawables.add(new WallClockDrawable(92, 70, 0.25));

				fd.drawables.add(new DeskDecorDrawable());
				fd.drawables.add(new WindowBackgroundDrawable());
                // ชั้นวางต้นไม้ใต้นาฬิกา (กึ่งกลาง x, ระดับแผ่นชั้น, สเกล)
                fd.drawables.add(new PlantShelfDrawable(92, 180, 1.0));

                // สามารถเพิ่ม object อื่น ๆ ได้อีก เช่น
                // fd.drawables.add(new AnotherDrawable(...));
                // ------------------------------------------
                frames.add(fd);
                String n = new File(filePath).getName();
                names.add(n.endsWith(".svg") ? n.substring(0, n.length() - 4) : n);
            }
        }

        setOpaque(true);
        setBackground(Color.WHITE);
        installKeys();
        installPicker();

        if (!frames.isEmpty()) {
            timer = new Timer(ArtConfig.TICK_INTERVAL, e -> {
                // เปลี่ยนเฟรมตามเวลา
                if (!paused) {
                    frameCounter += ArtConfig.TICK_INTERVAL;
                    if (frameCounter >= ArtConfig.FRAME_INTERVAL) {
                        frameCounter = 0;
                        currentIndex = (currentIndex + 1) % frames.size();
                    }
                }
                repaint();
            });
            timer.start();
        }
    }

    // =================== paintComponent ===================
    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        if (frames.isEmpty()) return;

        Graphics2D g2 = (Graphics2D) g;

        // 1. สร้างพื้นหลัง (Cache) ถ้าขนาดหน้าต่างเปลี่ยน
        ensureBackgrounds(getWidth(), getHeight());

        // 2. วาดพื้นหลัง (SVG ที่ใส่สีแล้ว)
        if (currentIndex < backgrounds.size()) {
            g.drawImage(backgrounds.get(currentIndex), 0, 0, null);
        }

        // 3. วาด Drawable Objects ของเฟรมปัจจุบัน
        FrameData fd = frames.get(currentIndex);
        double time = System.currentTimeMillis() / 1000.0; // เวลาจริงเป็นวินาที
        for (Drawable d : fd.drawables) {
            d.draw(g2, time);
        }

        // 4. วาด HUD
        drawHud(g2);
    }

    private void drawHud(Graphics2D g) {
        g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING,
                RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
        g.setFont(new Font(Font.SANS_SERIF, Font.PLAIN, 12));
        g.setColor(new Color(0x66000000, true));
        g.drawString((currentIndex < names.size() ? names.get(currentIndex) : "?")
                + (paused ? "  [stop]" : "")
                + "  |  " + (colorOn ? "color" : "line")
                + "  |  SPACE=stop  LEFT/RIGHT=change frame  C=switch color", 10, 18);
        g.setColor(new Color(0xCC0033AA, true));
        g.drawString(pickText, 10, 34);
    }

    // =================== สร้างพื้นหลัง (Cache) ===================
    private void ensureBackgrounds(int w, int h) {
        if (w <= 0 || h <= 0 || (w == lastW && h == lastH && !backgrounds.isEmpty())) return;

        double s = Math.min(Math.max(1, w - 2 * ArtConfig.PAD) / ArtConfig.VBW,
                Math.max(1, h - 2 * ArtConfig.PAD) / ArtConfig.VBH);
        AffineTransform at = new AffineTransform();
        at.translate(w / 2.0, h / 2.0);
        at.scale(s, s);
        at.translate(-ArtConfig.VBW / 2, -ArtConfig.VBH / 2);

        backgrounds.clear();
        for (int i = 0; i < frames.size(); i++) {
            backgrounds.add(FillEngine.rasteriseBackground(frames.get(i).svgPath, i, at, w, h, colorOn));
        }
        lastW = w;
        lastH = h;
    }

    // =================== Picker (คลิกหาพิกัด) ===================
    private void installPicker() {
        addMouseListener(new MouseAdapter() {
            @Override
            public void mousePressed(MouseEvent e) {
                pick(e.getX(), e.getY());
            }
        });
    }

    private void pick(int px, int py) {
        int w = getWidth(), h = getHeight();
        if (w <= 0 || h <= 0 || frames.isEmpty()) return;

        double s = Math.min(Math.max(1, w - 2 * ArtConfig.PAD) / ArtConfig.VBW,
                Math.max(1, h - 2 * ArtConfig.PAD) / ArtConfig.VBH);
        double ux = (px - w / 2.0) / s + ArtConfig.VBW / 2;
        double uy = (py - h / 2.0) / s + ArtConfig.VBH / 2;

        String frame = currentIndex < names.size() ? names.get(currentIndex) : "?";
        String what = "out";
        if (currentIndex < backgrounds.size() && px >= 0 && px < w && py >= 0 && py < h) {
            int rgb = backgrounds.get(currentIndex).getRGB(px, py) & 0xFFFFFF;
            what = ArtConfig.describe(rgb);
        }

        pickText = String.format("%s  (%.1f, %.1f)  %s", frame, ux, uy, what);
        System.out.printf("[pick] %s%n        new Seed(%.1f, %.1f, SKIN),   // <- fix SKIN %n",
                pickText, ux, uy);
        repaint();
    }

    // =================== Key bindings ===================
    private void installKeys() {
        bind("SPACE", () -> {
            paused = !paused;
            if (timer != null) {
                if (paused) timer.stop(); else timer.start();
            }
        });
        bind("LEFT", () -> step(-1));
        bind("RIGHT", () -> step(1));
        bind("C", () -> {
            colorOn = !colorOn;
            lastW = -1; // บังคับสร้างพื้นหลังใหม่
        });
    }

    private void step(int d) {
        if (frames.isEmpty()) return;
        if (!paused) {
            paused = true;
            if (timer != null) timer.stop();
        }
        currentIndex = (currentIndex + d + frames.size()) % frames.size();
        frameCounter = 0; // รีเซ็ตตัวนับเวลา
        repaint();
    }

    private void bind(String key, Runnable action) {
        getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW).put(KeyStroke.getKeyStroke(key), key);
        getActionMap().put(key, new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                action.run();
                repaint();
            }
        });
    }

    // =================== main ===================
    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            JFrame frame = new JFrame("Multi-Frame SVG with Drawable Objects");
            frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
            frame.add(new MultiSvgAnimationApp(ArtConfig.FILES));
            frame.setSize(600, 600);
            frame.setLocationRelativeTo(null);
            frame.setVisible(true);
        });
    }
}
