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

    /** ผืนผ้าใบของเราเอง - ทุกพิกเซลที่ออกจอผ่าน plot() ของตัวนี้ */
    private Raster canvas;
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
					// ไป Scene ถัดไป (loadScene รีเซ็ต sceneElapsedTime ให้เอง)
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

    /**
     * รูป SVG ที่หมุนขยายทับหน้าจอ
     *
     * เวลาของมันมาจาก sceneElapsedTime ตัวเดียวกับที่ใช้ตัดซีน ไม่ใช่เวลาจริง
     * จังหวะพุ่งจนขาวเต็มจอจะได้ตรงกับตอนตัดไปซีนถัดไปเป๊ะ และหยุดตามตอนกด SPACE
     */
    private KmitlBoardDrawable zoomingOverlay;

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
        // รีเซ็ตที่นี่ที่เดียว - ปุ่ม N ก็เรียกทางนี้ อนิเมชันป้ายจะได้เริ่มจากศูนย์เสมอ
        sceneElapsedTime = 0;

        // -----------------------------------------------------------
        if (sceneIndex == 2) { 
			// รอ 2.0 -> หมุนเข้า 1.5 -> หยุดนิ่ง 1.5 -> พุ่งเข้าหาคนดู 1.0 = 6.0 วิ
			// ต้องเท่ากับ ArtConfig.SCENE_DURATION[2] เป๊ะ ซีนจะได้ตัดตอนจอขาวพอดี
			this.zoomingOverlay = new KmitlBoardDrawable(2.0, 1.5, 1.5, 1.0);
		} else {
			this.zoomingOverlay = null;
		}
    }

    // =================== paintComponent ===================
    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        if (frames.isEmpty()) return;

        int w = getWidth(), h = getHeight();
        double time = System.currentTimeMillis() / 1000.0;

        ensureBackgrounds(w, h);
        FrameData fd = frames.get(currentIndex);

        // ผืนผ้าใบของเราเอง - ทุกพิกเซลที่ออกจอผ่าน Raster.plot() ทั้งหมด
        // ใช้ซ้ำจนกว่าขนาดหน้าต่างจะเปลี่ยน จะได้ไม่ต้องจองใหม่ทุก 30ms
        if (canvas == null || canvas.w != w || canvas.h != h) {
            canvas = new Raster(w, h);
        }

        // 1. สีผนัง - เดิมมาจาก flood fill ตอนนี้ย้ายมาถมพื้น panel แทน
        //    เพราะผนังในภาพตัวละครถูกทำให้โปร่งไปแล้ว
        canvas.clear(ArtConfig.BACKDROP.getRGB());

        // 2. props ที่ติดผนัง - วาดก่อน ตัวละครจะได้บังได้
        for (Drawable d : fd.behind) {
            d.draw(canvas, time);
        }

        // 3. ตัวละคร - ผนังโปร่ง props ข้างหลังจึงทะลุขึ้นมา
        if (currentIndex < backgrounds.size()) {
            blit(canvas, backgrounds.get(currentIndex));
        }

        // 4. props ที่อยู่หน้าคน
        for (Drawable d : fd.front) {
            d.draw(canvas, time);
        }

        // 5. Render Component Overlay หมุนขยายทับข้างบน
        //    ใช้ sceneElapsedTime ไม่ใช่ time - นาฬิกาเรือนเดียวกับที่ตัดซีน
        if (zoomingOverlay != null) {
            zoomingOverlay.setPanelSize(w, h);
            zoomingOverlay.draw(canvas, sceneElapsedTime / 1000.0);
        }

        drawHud(canvas);

        // เอาผืนผ้าใบขึ้นจอ - คำสั่ง Java2D ตัวเดียวที่เหลือในโปรเจกต์
        // ไม่ได้วาดรูปทรงอะไรให้ แค่คัดลอกบิตแมปที่เราพล็อตเองทั้งใบ
        g.drawImage(canvas.image(), 0, 0, null);
    }

    /** วางภาพตัวละครที่ cache ไว้ลงผืนผ้าใบ ผ่าน plot() ทีละจุดเหมือนกัน */
    private static void blit(Raster r, BufferedImage img) {
        int iw = Math.min(r.w, img.getWidth());
        int ih = Math.min(r.h, img.getHeight());
        for (int y = 0; y < ih; y++) {
            for (int x = 0; x < iw; x++) {
                r.plot(x, y, img.getRGB(x, y));
            }
        }
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
    
    /** HUD เขียนด้วยฟอนต์บิตแมปที่พล็อตเอง ไม่ใช่ drawString - ต้องเป็น ASCII ล้วน */
    private void drawHud(Raster r) {
        Gfx.text(r, 10, 10, (scene != null ? scene.name + " / " : "")
                + (currentIndex < names.size() ? names.get(currentIndex) : "?")
                + (paused ? "  [stop]" : "")
                + "  |  " + (colorOn ? "color" : "line")
                + "  |  SPACE=stop  LEFT/RIGHT=frame  N=scene  C=color", 0x66000000);
        Gfx.text(r, 10, 22, pickText, 0xCC0033AA);
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
            // HUD ใช้ฟอนต์บิตแมป ASCII ข้อความจึงต้องเป็นอังกฤษ
            what = (argb >>> 24) == 0 ? "wall (transparent)" : ArtConfig.describe(argb & 0xFFFFFF);
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
