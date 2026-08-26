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
        /** วาดก่อนตัวละคร - ของที่ติดผนัง ตัวละครจะบังได้ */
        List<Drawable> behind = new ArrayList<>();
        /** วาดหลังตัวละคร - ของที่อยู่หน้าคน เช่นของบนโต๊ะ */
        List<Drawable> front = new ArrayList<>();
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

    /** ซีนที่กำลังเล่น - seed กับเส้นอุดมาจากตัวนี้ ไม่ใช่จากค่ากลาง */
    private ArtConfig.Scene scene;
    private int sceneIndex = 0;

    private String pickText = "click to pick a point";

	private int sceneElapsedTime = 0;

    // =================== Constructor ===================
    public MultiSvgAnimationApp(int startScene) {
        setOpaque(true);
        setBackground(Color.WHITE);
        installKeys();
        installPicker();

        timer = new Timer(ArtConfig.TICK_INTERVAL, e -> {
			if (!paused) {
				sceneElapsedTime += ArtConfig.TICK_INTERVAL;
				frameCounter += ArtConfig.TICK_INTERVAL;

				if (frameCounter >= ArtConfig.FRAME_INTERVAL) {
					frameCounter = 0;

					if (currentIndex + 1 >= frames.size()) {
						currentIndex = 0;
					} else {
						currentIndex++;
					}
				}

				// ถ้าเล่น Scene นี้ครบเวลาที่กำหนด
				if (sceneElapsedTime >= ArtConfig.SCENE_DURATION[sceneIndex]) {
					sceneElapsedTime = 0;
					// ไป Scene ถัดไป
					loadScene((sceneIndex + 1) % ArtConfig.SCENES.length);
				}
			}

			repaint();
		});

        loadScene(startScene);
        timer.start();
    }

    /**
     * สลับซีน - โหลดไฟล์ของซีนนั้น แล้วทิ้ง cache พื้นหลังทั้งหมด
     *
     * ต้องตั้ง lastW = -1 ด้วย ไม่งั้น ensureBackgrounds เห็นว่าขนาดหน้าต่าง
     * ไม่เปลี่ยนแล้ว return ทันที จะได้ภาพซีนเก่าค้าง
     */

    // ... (คงตัวแปรเดิมไว้) ...

    /** เพิ่มตัวแปรสำหรับรูป SVG ที่จะหมุนขยายทับหน้าจอ */
    private KmitlBoardDrawable zoomingOverlay;
    private double sceneStartTime = -1; // ตัวจับเวลาเริ่มซีน (หน่วยวินาที)

    // =================== loadScene ===================
    public void loadScene(int index) {
        ArtConfig.Scene next = ArtConfig.SCENES[index];

        List<FrameData> loaded = new ArrayList<>();
        List<String> loadedNames = new ArrayList<>();
        for (String filePath : next.files) {
            Path2D path = SvgLoader.loadSvg(filePath);
            if (path == null) continue;
            FrameData fd = new FrameData(path);
			if (!next.name.equals("sad")) {
				addProps(fd);
			}
            loaded.add(fd);
            String n = new File(filePath).getName();
            loadedNames.add(n.endsWith(".svg") ? n.substring(0, n.length() - 4) : n);
        }

        if (loaded.isEmpty()) return;

        scene = next;
        sceneIndex = index;
        frames.clear();
        frames.addAll(loaded);
        names.clear();
        names.addAll(loadedNames);
        backgrounds.clear();
        currentIndex = 0;
        frameCounter = 0;
        lastW = -1;
        sceneStartTime = -1; // รีเซ็ตเวลาเริ่มต้นซีนใหม่

        // -----------------------------------------------------------
        if (sceneIndex == 2) { 
			this.zoomingOverlay = new KmitlBoardDrawable(2.5, 2.0);
		} else {
			this.zoomingOverlay = null;
		}
    }

    // =================== paintComponent ===================
    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        if (frames.isEmpty()) return;

        Graphics2D g2 = (Graphics2D) g;
        double time = System.currentTimeMillis() / 1000.0;

        if (sceneStartTime < 0) sceneStartTime = time;

        ensureBackgrounds(getWidth(), getHeight());
        FrameData fd = frames.get(currentIndex);

        // 1. สีผนัง - เดิมมาจาก flood fill ตอนนี้ย้ายมาถมพื้น panel แทน
        //    เพราะผนังในภาพตัวละครถูกทำให้โปร่งไปแล้ว
        g2.setColor(ArtConfig.BACKDROP);
        g2.fillRect(0, 0, getWidth(), getHeight());

        // 2. props ที่ติดผนัง - วาดก่อน ตัวละครจะได้บังได้
        for (Drawable d : fd.behind) {
            d.draw(g2, time);
        }

        // 3. ตัวละคร - ผนังโปร่ง props ข้างหลังจึงทะลุขึ้นมา
        if (currentIndex < backgrounds.size()) {
            g.drawImage(backgrounds.get(currentIndex), 0, 0, null);
        }

        // 4. props ที่อยู่หน้าคน
        for (Drawable d : fd.front) {
            d.draw(g2, time);
        }

        // 5. Render Component Overlay หมุนขยายทับข้างบน
        if (zoomingOverlay != null) {
            zoomingOverlay.draw(g2, time - sceneStartTime);
        }

        drawHud(g2);
    }


    /** ของประกอบฉากที่วาดทับทุกเฟรม */
    private void addProps(FrameData fd) {
        // ติดผนัง - ตัวละครบังได้
        fd.behind.add(new WindowBackgroundDrawable());
        fd.behind.add(new WallClockDrawable(92, 70, 0.25));
        // ชั้นวางต้นไม้ใต้นาฬิกา (กึ่งกลาง x, ระดับแผ่นชั้น, สเกล)
        fd.behind.add(new PlantShelfDrawable(92, 180, 1.0));

        // อยู่บนโต๊ะ หน้าตัวละคร
        fd.front.add(new DeskDecorDrawable());
    }

    // =================== paintComponent ===================
    
    private void drawHud(Graphics2D g) {
        g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING,
                RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
        g.setFont(new Font(Font.SANS_SERIF, Font.PLAIN, 12));
        g.setColor(new Color(0x66000000, true));
        g.drawString((scene != null ? scene.name + " / " : "")
                + (currentIndex < names.size() ? names.get(currentIndex) : "?")
                + (paused ? "  [stop]" : "")
                + "  |  " + (colorOn ? "color" : "line")
                + "  |  SPACE=stop  LEFT/RIGHT=frame  N=scene  C=color", 10, 18);
        g.setColor(new Color(0xCC0033AA, true));
        g.drawString(pickText, 10, 34);
    }

    // =================== สร้างพื้นหลัง (Cache) ===================
    private void ensureBackgrounds(int w, int h) {
        if (w <= 0 || h <= 0 || (w == lastW && h == lastH && !backgrounds.isEmpty())) return;

        double vbw = ArtConfig.VBW;
		double vbh = ArtConfig.VBH;
		if (scene != null && "sad".equals(scene.name)) {
			vbw = ArtConfig.VBW_SAD;
			vbh = ArtConfig.VBH_SAD;
		}

		double s = Math.min(Math.max(1, w - 2 * ArtConfig.PAD) / vbw,
							Math.max(1, h - 2 * ArtConfig.PAD) / vbh);
		AffineTransform at = new AffineTransform();
		at.translate(w / 2.0, h / 2.0);
		at.scale(s, s);
		at.translate(-vbw / 2, -vbh / 2);

		backgrounds.clear();
		for (int i = 0; i < frames.size(); i++) {
			backgrounds.add(FillEngine.rasteriseBackground(
					frames.get(i).svgPath, scene.seedsFor(i), scene.dams, at, w, h, colorOn));
		}
		lastW = w;
		lastH = h;

		// ภาพทึบทั้งใบจะบัง props ที่อยู่ข้างหลัง - เป็นปัญหาเฉพาะซีนที่มี props หลัง
		if (!frames.isEmpty() && !frames.get(0).behind.isEmpty()
				&& !FillEngine.hasTransparent(backgrounds.get(0))) {
			System.err.println("[layer] ซีน " + scene.name + " ไม่มี seed BACKDROP "
					+ "ภาพจึงทึบทั้งใบ props ที่อยู่ข้างหลังจะถูกบัง");
		}
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

		double vbw = ArtConfig.VBW;
		double vbh = ArtConfig.VBH;
		if (scene != null && "sad".equals(scene.name)) {
			vbw = ArtConfig.VBW_SAD;
			vbh = ArtConfig.VBH_SAD;
		}

		double s = Math.min(Math.max(1, w - 2 * ArtConfig.PAD) / vbw,
							Math.max(1, h - 2 * ArtConfig.PAD) / vbh);
		double ux = (px - w / 2.0) / s + vbw / 2;
		double uy = (py - h / 2.0) / s + vbh / 2;

        String frame = currentIndex < names.size() ? names.get(currentIndex) : "?";
        String what = "out";
        if (currentIndex < backgrounds.size() && px >= 0 && px < w && py >= 0 && py < h) {
            int argb = backgrounds.get(currentIndex).getRGB(px, py);
            // พิกเซลโปร่งมี RGB = 0 ซึ่งชนกับสีหมึกดำ ต้องเช็ค alpha ก่อน
            what = (argb >>> 24) == 0 ? "wall (โปร่ง)" : ArtConfig.describe(argb & 0xFFFFFF);
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
        bind("N", () -> {
            // สลับซีนด้วยมือ - หยุดไว้ก่อนจะได้ดูทัน
            if (!paused) {
                paused = true;
                if (timer != null) timer.stop();
            }
            loadScene((sceneIndex + 1) % ArtConfig.SCENES.length);
        });
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
            frame.add(new MultiSvgAnimationApp(0));
            frame.setSize(600, 600);
            frame.setLocationRelativeTo(null);
            frame.setVisible(true);
        });
    }
}
