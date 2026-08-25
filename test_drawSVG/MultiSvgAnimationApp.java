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
 * ประกอบสามส่วนที่เหลือเข้าด้วยกัน: ให้ SvgLoader อ่านไฟล์ ให้ FillEngine
 * ลงสี แล้วเอาภาพที่ได้มาแปะลงจอ พร้อมปุ่มควบคุมกับเครื่องมือเก็บพิกัด
 *
 * ตัวนี้ไม่มีอัลกอริทึมและไม่มีค่าคงที่ปรับแต่ง - สีกับพิกัดอยู่ใน ArtConfig
 */
public class MultiSvgAnimationApp extends JPanel {

    private final List<Path2D> frames = new ArrayList<>();
    /** ชื่อสั้นของแต่ละเฟรม (a1, a2, ...) ไว้โชว์บนจอตอนเก็บพิกัด */
    private final List<String> names = new ArrayList<>();
    /** ภาพที่ลงสีเสร็จแล้ว หนึ่งใบต่อเฟรม สร้างใหม่เมื่อขนาดหน้าต่างเปลี่ยน */
    private final List<BufferedImage> buffers = new ArrayList<>();

    private int currentIndex = 0;
    private int lastW = -1, lastH = -1;
    private boolean colorOn = true;
    private boolean paused = false;
    private Timer timer;

    /** ข้อความจากการคลิกครั้งล่าสุด โชว์บนจอเพื่อไม่ต้องคอยมองคอนโซล */
    private String pickText = "click to pick a point";

    public MultiSvgAnimationApp(String[] filePaths) {
        for (String filePath : filePaths) {
            Path2D path = SvgLoader.load(filePath);
            if (path != null) {
                frames.add(path);
                String n = new File(filePath).getName();
                names.add(n.endsWith(".svg") ? n.substring(0, n.length() - 4) : n);
            }
        }

        setOpaque(true);
        setBackground(Color.WHITE);
        installKeys();
        installPicker();

        if (!frames.isEmpty()) {
            timer = new Timer(ArtConfig.FRAME_DELAY, e -> {
                currentIndex = (currentIndex + 1) % frames.size();
                repaint();
            });
            timer.start();
        }
    }

    // ------------------------------------------------------------ rendering

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        if (frames.isEmpty()) return;

        ensureFrames(getWidth(), getHeight());
        if (currentIndex < buffers.size()) {
            g.drawImage(buffers.get(currentIndex), 0, 0, null);
        }
        drawHud((Graphics2D) g);
    }

    /**
     * สร้างภาพทุกเฟรมไว้ล่วงหน้าหนึ่งครั้งต่อขนาดหน้าต่าง
     * พอ cache แล้ว การสลับเฟรมเหลือแค่ blit ภาพเดียว
     */
    private void ensureFrames(int w, int h) {
        if (w <= 0 || h <= 0 || (w == lastW && h == lastH && !buffers.isEmpty())) return;

        AffineTransform at = ArtConfig.viewTransform(w, h);
        buffers.clear();
        for (int i = 0; i < frames.size(); i++) {
            buffers.add(FillEngine.rasterise(frames.get(i), ArtConfig.seedsFor(i), at, w, h, colorOn));
        }
        lastW = w;
        lastH = h;
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

    // -------------------------------------------------------- coordinate pick

    /**
     * คลิกแล้วบอกพิกัดในหน่วย viewBox พร้อมสีที่จุดนั้น และพิมพ์บรรทัด
     * new Seed(...) สำเร็จรูปให้คัดลอกไปวางใน ArtConfig ได้เลย
     *
     * เก็บพิกัดในหน่วย viewBox ไม่ใช่พิกเซลจอ เพราะพิกเซลจอเปลี่ยนตามขนาด
     * หน้าต่าง แต่ viewBox คงที่เสมอ
     */
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

        // สูตรเดียวกับที่ใช้วาด (ArtConfig.viewTransform) แค่กลับทาง
        double s = ArtConfig.scaleFor(w, h);
        double ux = (px - w / 2.0) / s + ArtConfig.VBW / 2;
        double uy = (py - h / 2.0) / s + ArtConfig.VBH / 2;

        String frame = currentIndex < names.size() ? names.get(currentIndex) : "?";
        String what = "out";
        if (currentIndex < buffers.size() && px >= 0 && px < w && py >= 0 && py < h) {
            what = ArtConfig.describe(buffers.get(currentIndex).getRGB(px, py) & 0xFFFFFF);
        }

        pickText = String.format("%s  (%.1f, %.1f)  %s", frame, ux, uy, what);
        System.out.printf("[pick] %s%n        new Seed(%.1f, %.1f, SKIN),   // <- fix SKIN %n",
                pickText, ux, uy);
        repaint();
    }

    // ------------------------------------------------------------- controls

    /**
     * SPACE หยุด/เล่น (ต้องหยุดก่อนถึงจะคลิกเก็บพิกัดทัน)
     * LEFT/RIGHT เดินเฟรมทีละภาพ
     * C สลับเปิด/ปิดสี ไว้เทียบกับลายเส้นเปล่า
     */
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
            lastW = -1; // บังคับให้สร้างภาพใหม่ทุกเฟรม
        });
    }

    /** เดินเฟรมด้วยมือ - หยุด timer ให้อัตโนมัติ ไม่งั้นมันแย่งเปลี่ยนเฟรม */
    private void step(int d) {
        if (frames.isEmpty()) return;
        if (!paused) {
            paused = true;
            if (timer != null) timer.stop();
        }
        currentIndex = (currentIndex + d + frames.size()) % frames.size();
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

    // ----------------------------------------------------------------- main

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            JFrame frame = new JFrame("Multi-Frame SVG Switch");
            frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
            frame.add(new MultiSvgAnimationApp(ArtConfig.FILES));
            frame.setSize(600, 600);
            frame.setLocationRelativeTo(null);
            frame.setVisible(true);
        });
    }
}
