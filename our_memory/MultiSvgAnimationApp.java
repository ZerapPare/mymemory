import javax.swing.JPanel;
import javax.swing.Timer;
import java.awt.Graphics;
import java.awt.geom.AffineTransform;
import java.awt.geom.Path2D;
import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.List;

/**
 * Swing UI / Presentation
 */
public class MultiSvgAnimationApp extends JPanel {

    /** ข้อมูลของแต่ละเฟรม  */
    private static class FrameData {
        Path2D svgPath;
        /** วาดก่อนตัวละคร - ของที่ติดผนัง ตัวละครจะบังได้ */
        List<Drawable> behind = new ArrayList<>();
        /** วาดหลังตัวละคร - ของที่อยู่หน้าคน */
        List<Drawable> front = new ArrayList<>();
        FrameData(Path2D path) {
            this.svgPath = path;
        }
    }

    private final List<FrameData> frames = new ArrayList<>();

    private final List<BufferedImage> backgrounds = new ArrayList<>();

    /** ทุกพิกเซลที่ออกจอผ่าน plot()  */
    private Raster canvas;
    private int lastW = -1, lastH = -1;

    private int currentIndex = 0;
    private Timer timer;
    private int frameCounter = 0;        // นับจำนวน tick

    /** ซีนที่กำลังเล่น */
    private ArtConfig.Scene scene;
    private int sceneIndex = 0;

    private int sceneElapsedTime = 0;

    /**
     * ฉากที่หมุนขยายทับหน้าจอเปลี่ยนฉาก
     */
    private KmitlBoardDrawable zoomingOverlay;

    // Constructor 
    public MultiSvgAnimationApp(int startScene) {
        setOpaque(true);

        timer = new Timer(ArtConfig.TICK_INTERVAL, e -> {
            sceneElapsedTime += ArtConfig.TICK_INTERVAL;
            frameCounter += ArtConfig.TICK_INTERVAL;

            if (frameCounter >= ArtConfig.FRAME_INTERVAL) {
                frameCounter = 0;
                currentIndex = (currentIndex + 1) % frames.size();
            }

            if (sceneElapsedTime >= ArtConfig.SCENE_DURATION[sceneIndex]) {
                loadScene((sceneIndex + 1) % ArtConfig.SCENES.length);
            }

            repaint();
        });

        loadScene(startScene);
        timer.start();
    }

    // loadScene 
    /**
     * สลับซีน - โหลดไฟล์ของซีนนั้น แล้วทิ้ง cache พื้นหลังทั้งหมด
     */
    public void loadScene(int index) {
        ArtConfig.Scene next = ArtConfig.SCENES[index];

        List<FrameData> loaded = new ArrayList<>();
        for (String filePath : next.files) {
            Path2D path = SvgLoader.loadSvg(filePath);
            if (path == null) continue;
            FrameData fd = new FrameData(path);
            if (!next.name.equals("sad")) {
                addProps(fd);
            }
            loaded.add(fd);
        }

        if (loaded.isEmpty()) return;

        scene = next;
        sceneIndex = index;
        frames.clear();
        frames.addAll(loaded);
        backgrounds.clear();
        currentIndex = 0;
        frameCounter = 0;
        lastW = -1;
        sceneElapsedTime = 0;

        // ป้าย KMITL ผูกกับซีน happy 
        // รอ 2.0 หมุนเข้า 1.5 หยุดนิ่ง 1.5 พุ่ง 1.0 = 6.0 วิ
        this.zoomingOverlay = (sceneIndex == 2)
                ? new KmitlBoardDrawable(2.0, 1.5, 1.5, 1.0)
                : null;
    }

    // paintComponent 
    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        if (frames.isEmpty()) return;

        int w = getWidth(), h = getHeight();
        double time = System.currentTimeMillis() / 1000.0;

        ensureBackgrounds(w, h);
        FrameData fd = frames.get(currentIndex);

        // ใช้ซ้ำจนกว่าขนาดหน้าต่างจะเปลี่ยน
        if (canvas == null || canvas.w != w || canvas.h != h) {
            canvas = new Raster(w, h);
        }

        // 1. สีผนัง 
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

        // 5. ป้าย KMITL หมุนขยายทับข้างบน
        if (zoomingOverlay != null) {
            zoomingOverlay.setPanelSize(w, h);
            zoomingOverlay.draw(canvas, sceneElapsedTime / 1000.0);
        }

        g.drawImage(canvas.image(), 0, 0, null);
    }

    /** วางตัวละครลง canvas ทีละจุด */
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
        fd.behind.add(new WindowBackgroundDrawable());
        fd.behind.add(new WallClockDrawable(92, 70, 0.25));
        fd.behind.add(new PlantShelfDrawable(92, 180, 1.0));
        fd.front.add(new DeskDecorDrawable());
    }

    // สร้างพื้นหลัง
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
                    frames.get(i).svgPath, scene.seedsFor(i), scene.dams, at, w, h, true));
        }
        lastW = w;
        lastH = h;
    }
}