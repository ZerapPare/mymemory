import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.geom.AffineTransform;
import java.awt.geom.Line2D;
import java.awt.geom.Path2D;
import java.awt.geom.Point2D;
import java.awt.image.BufferedImage;
import java.awt.image.DataBufferInt;
import java.io.File;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

// ============================================================
//  1. Interface Drawable (ถ้าแยกไฟล์ -> Drawable.java)
// ============================================================
interface Drawable {
    void draw(Graphics2D g, double time);
}

// ============================================================
//  5. คลาสหลัก MultiSvgAnimationApp (JPanel)
// ============================================================
public class MultiSvgAnimationApp extends JPanel {

    // ----- ข้อมูลของแต่ละเฟรม (SVG + Drawable objects) -----
    private static class FrameData {
        Path2D svgPath;
        List<Drawable> drawables = new ArrayList<>();
        FrameData(Path2D path) {
            this.svgPath = path;
        }
    }

    private final List<FrameData> frames = new ArrayList<>();
    private int currentIndex = 0;

    // ------ สีที่ใช้ใน SVG (เหมือนเดิม) ------
    private static final Color BACKDROP = new Color(0xF2E4CB);
    private static final Color HAIR     = new Color(0x6B4A2E);
    private static final Color SKIN     = new Color(0xFFDCB8);
    private static final Color SHIRT    = new Color(0x7FA8D4);
    private static final Color SCREEN   = new Color(0xDCEBFF);

    private static class Seed {
        final double x, y;
        final Color color;
        Seed(double x, double y, Color color) { this.x = x; this.y = y; this.color = color; }
    }

    private static final Seed[] COMMON = {
        new Seed( 30.0,  30.0, BACKDROP),
        new Seed(229.8, 207.6, SHIRT),
        new Seed(301.0,  75.5, HAIR),
        new Seed(325.2, 111.1, SKIN),
        new Seed(294.4, 142.0, SKIN),
        new Seed(350.3, 243.5, SHIRT),
        new Seed(306.4, 200.0, SKIN),
        new Seed(333.5, 210.4, SKIN),
        new Seed(327.9, 175.8, SKIN),
    };

    private static final Seed[] SCREEN_PER_FRAME = {
        new Seed(411.7, 289.3, SCREEN),
        new Seed(365.4, 323.6, SCREEN),
        new Seed(404.5, 294.9, SCREEN),
        new Seed(411.7, 289.3, SCREEN),
        new Seed(365.4, 323.6, SCREEN),
        new Seed(404.5, 294.9, SCREEN),
    };

    private static final double[][] DAMS = {
        { 149.4, 355.4, -37.4, 456.5 },
        {485.1, 238.3, 659.5, 262.5}
    };
    private static final double DAM_WIDTH = 1.0;
    private static final int BLANK = 0xFFFFFF;
    private static final double VBW = 600, VBH = 384, PAD = 24;

    // ---- พื้นหลังที่เรนเดอร์แล้ว (Cache) ----
    private final List<BufferedImage> backgrounds = new ArrayList<>();
    private int lastW = -1, lastH = -1;
    private boolean colorOn = true;

    private final List<String> names = new ArrayList<>();

    // ---- Timer สำหรับเปลี่ยนเฟรมและรีเฟรช ----
    private Timer timer;
    private boolean paused = false;
    private int frameCounter = 0;        // นับจำนวน tick
    private static final int FRAME_INTERVAL = 350; // ms ต่อเฟรม
    private static final int TICK_INTERVAL = 30;   // ms ต่อการวาด

    private String pickText = "click to pick a point";

    // =================== Constructor ===================
    public MultiSvgAnimationApp(String[] filePaths) {
        for (String filePath : filePaths) {
            Path2D path = loadSvg(filePath);
            if (path != null) {
                FrameData fd = new FrameData(path);
                // ***** เพิ่ม Drawable ต่าง ๆ ลงในเฟรมนี้ *****
                // ตัวอย่าง: เพิ่มนาฬิกาที่ตำแหน่ง (300,92) ความเร็ว 1 รอบ/วินาที
                fd.drawables.add(new WallClockDrawable(92, 70, 0.25));
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
            timer = new Timer(TICK_INTERVAL, e -> {
                // เปลี่ยนเฟรมตามเวลา
                if (!paused) {
                    frameCounter += TICK_INTERVAL;
                    if (frameCounter >= FRAME_INTERVAL) {
                        frameCounter = 0;
                        currentIndex = (currentIndex + 1) % frames.size();
                    }
                }
                repaint();
            });
            timer.start();
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

        double s = Math.min(Math.max(1, w - 2 * PAD) / VBW, Math.max(1, h - 2 * PAD) / VBH);
        double ux = (px - w / 2.0) / s + VBW / 2;
        double uy = (py - h / 2.0) / s + VBH / 2;

        String frame = currentIndex < names.size() ? names.get(currentIndex) : "?";
        String what = "out";
        if (currentIndex < backgrounds.size() && px >= 0 && px < w && py >= 0 && py < h) {
            int rgb = backgrounds.get(currentIndex).getRGB(px, py) & 0xFFFFFF;
            what = describe(rgb);
        }

        pickText = String.format("%s  (%.1f, %.1f)  %s", frame, ux, uy, what);
        System.out.printf("[pick] %s%n        new Seed(%.1f, %.1f, SKIN),   // <- fix SKIN %n",
                pickText, ux, uy);
        repaint();
    }

    private static String describe(int rgb) {
        if (rgb == 0x000000) return "black";
        if (rgb == BLANK) return "blank";
        if (rgb == (BACKDROP.getRGB() & 0xFFFFFF)) return "background";
        if (rgb == (HAIR.getRGB() & 0xFFFFFF)) return "hair";
        if (rgb == (SKIN.getRGB() & 0xFFFFFF)) return "skin";
        if (rgb == (SHIRT.getRGB() & 0xFFFFFF)) return "shirt";
        if (rgb == (SCREEN.getRGB() & 0xFFFFFF)) return "screen";
        return String.format("#%06X", rgb);
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

    // =================== โหลด SVG ===================
    private Path2D loadSvg(String fileName) {
        File file = new File(fileName);
        if (!file.exists()) return null;

        Path2D targetPath = new Path2D.Double();
        try {
            String content = Files.readString(file.toPath());
            AffineTransform groupTransform = parseGroupTransform(content);

            Pattern pathPattern = Pattern.compile("d=\"(.*?)\"", Pattern.DOTALL);
            Matcher pathMatcher = pathPattern.matcher(content);

            while (pathMatcher.find()) {
                Path2D singlePath = parseSvgPath(pathMatcher.group(1));
                if (groupTransform != null) {
                    singlePath.transform(groupTransform);
                }
                targetPath.append(singlePath, false);
            }
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
        return targetPath;
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

        double s = Math.min(Math.max(1, w - 2 * PAD) / VBW, Math.max(1, h - 2 * PAD) / VBH);
        AffineTransform at = new AffineTransform();
        at.translate(w / 2.0, h / 2.0);
        at.scale(s, s);
        at.translate(-VBW / 2, -VBH / 2);

        backgrounds.clear();
        for (int i = 0; i < frames.size(); i++) {
            backgrounds.add(rasteriseBackground(frames.get(i).svgPath, i, at, w, h));
        }
        lastW = w;
        lastH = h;
    }

    /** วาดเฉพาะพื้นหลัง (SVG + flood fill) ไม่รวม Drawable */
    private BufferedImage rasteriseBackground(Path2D path, int frameIndex, AffineTransform at, int w, int h) {
        BufferedImage img = new BufferedImage(w, h, BufferedImage.TYPE_INT_RGB);
        Graphics2D g = img.createGraphics();
        g.setColor(Color.WHITE);
        g.fillRect(0, 0, w, h);

        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_OFF);
        g.setStroke(new BasicStroke(1f));
        g.setColor(Color.BLACK);

        Shape sh = at.createTransformedShape(path);
        g.fill(sh);
        g.draw(sh);

        // เส้นอุด (DAMS)
        g.setStroke(new BasicStroke((float) Math.max(3, DAM_WIDTH * s(at)),
                BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
        Point2D.Double p1 = new Point2D.Double();
        Point2D.Double p2 = new Point2D.Double();
        for (double[] d : DAMS) {
            p1.setLocation(d[0], d[1]);
            p2.setLocation(d[2], d[3]);
            at.transform(p1, p1);
            at.transform(p2, p2);
            g.drawLine((int) Math.round(p1.x), (int) Math.round(p1.y),
                       (int) Math.round(p2.x), (int) Math.round(p2.y));
        }
        g.dispose();

        if (!colorOn) return img;

        int[] px = ((DataBufferInt) img.getRaster().getDataBuffer()).getData();
        int radius = Math.max(2, (int) Math.round(1.5 * s(at)));

        List<Seed> seeds = new ArrayList<>(Arrays.asList(COMMON));
        if (frameIndex < SCREEN_PER_FRAME.length) seeds.add(SCREEN_PER_FRAME[frameIndex]);

        Point2D.Double p = new Point2D.Double();
        for (Seed seed : seeds) {
            p.setLocation(seed.x, seed.y);
            at.transform(p, p);
            int sx = (int) Math.round(p.x);
            int sy = (int) Math.round(p.y);
            int want = seed.color.getRGB() & 0xFFFFFF;

            int start = findBlankNear(px, w, h, sx, sy, radius);
            if (start >= 0) {
                floodFill(px, w, h, start % w, start / w, want);
            } else if (!hasColourNear(px, w, h, sx, sy, radius, want)) {
                System.err.println("[seed] (" + seed.x + ", " + seed.y + ") ที่ " + w + "x" + h
                        + " - บริเวณรวมกับเพื่อนบ้านคนละสี");
            }
        }
        return img;
    }

    private static double s(AffineTransform at) {
        return at.getScaleX();
    }

    // =================== Flood Fill (เหมือนเดิม) ===================
    private static void floodFill(int[] px, int w, int h, int sx, int sy, int rgb) {
        int fill = rgb & 0xFFFFFF;
        if (fill == BLANK || (px[sy * w + sx] & 0xFFFFFF) != BLANK) return;

        int[] stack = new int[1024];
        int sp = 0;
        stack[sp++] = sy * w + sx;

        while (sp > 0) {
            int p = stack[--sp];
            if ((px[p] & 0xFFFFFF) != BLANK) continue;
            int y = p / w, row = y * w;

            int left = p - row;
            while (left > 0 && (px[row + left - 1] & 0xFFFFFF) == BLANK) left--;
            int right = p - row;
            while (right < w - 1 && (px[row + right + 1] & 0xFFFFFF) == BLANK) right++;
            for (int i = left; i <= right; i++) px[row + i] = 0xFF000000 | fill;

            for (int dy = -1; dy <= 1; dy += 2) {
                int ny = y + dy;
                if (ny < 0 || ny >= h) continue;
                int nrow = ny * w;
                boolean inRun = false;
                for (int i = left; i <= right; i++) {
                    if ((px[nrow + i] & 0xFFFFFF) == BLANK) {
                        if (!inRun) {
                            if (sp == stack.length) stack = Arrays.copyOf(stack, stack.length * 2);
                            stack[sp++] = nrow + i;
                            inRun = true;
                        }
                    } else {
                        inRun = false;
                    }
                }
            }
        }
    }

    private static int findBlankNear(int[] px, int w, int h, int sx, int sy, int maxR) {
        for (int r = 0; r <= maxR; r++) {
            for (int dy = -r; dy <= r; dy++) {
                for (int dx = -r; dx <= r; dx++) {
                    if (r > 0 && Math.abs(dx) != r && Math.abs(dy) != r) continue;
                    int x = sx + dx, y = sy + dy;
                    if (x >= 0 && x < w && y >= 0 && y < h && (px[y * w + x] & 0xFFFFFF) == BLANK) {
                        return y * w + x;
                    }
                }
            }
        }
        return -1;
    }

    private static boolean hasColourNear(int[] px, int w, int h, int sx, int sy, int maxR, int rgb) {
        for (int y = Math.max(0, sy - maxR); y <= Math.min(h - 1, sy + maxR); y++) {
            for (int x = Math.max(0, sx - maxR); x <= Math.min(w - 1, sx + maxR); x++) {
                if ((px[y * w + x] & 0xFFFFFF) == rgb) return true;
            }
        }
        return false;
    }

    // =================== SVG Path Parser (เหมือนเดิม) ===================
    private Path2D parseSvgPath(String d) {
        Path2D.Double path = new Path2D.Double();
        Pattern p = Pattern.compile("([a-zA-Z])|([-+]?\\d*\\.?\\d+(?:[eE][-+]?\\d+)?)");
        Matcher m = p.matcher(d);

        String cmd = "";
        double startX = 0, startY = 0;
        double curX = 0, curY = 0;

        while (m.find()) {
            String token = m.group();
            if (token.matches("[a-zA-Z]")) {
                cmd = token;
                if (cmd.equalsIgnoreCase("Z")) {
                    path.closePath();
                    curX = startX; curY = startY;
                }
            } else {
                try {
                    boolean isRel = Character.isLowerCase(cmd.charAt(0));
                    switch (cmd.toUpperCase()) {
                        case "M": {
                            double x = Double.parseDouble(token); m.find();
                            double y = Double.parseDouble(m.group());
                            if (isRel) { x += curX; y += curY; }
                            path.moveTo(x, y);
                            curX = x; curY = y; startX = x; startY = y;
                            cmd = isRel ? "l" : "L";
                            break;
                        }
                        case "L": {
                            double x = Double.parseDouble(token); m.find();
                            double y = Double.parseDouble(m.group());
                            if (isRel) { x += curX; y += curY; }
                            customLineTo(path, x, y, curX, curY);
                            curX = x; curY = y;
                            break;
                        }
                        case "C": {
                            double x1 = Double.parseDouble(token); m.find(); double y1 = Double.parseDouble(m.group());
                            m.find(); double x2 = Double.parseDouble(m.group()); m.find(); double y2 = Double.parseDouble(m.group());
                            m.find(); double x3 = Double.parseDouble(m.group()); m.find(); double y3 = Double.parseDouble(m.group());
                            if (isRel) {
                                x1 += curX; y1 += curY; x2 += curX; y2 += curY; x3 += curX; y3 += curY;
                            }
                            customCurveTo(path, x1, y1, x2, y2, x3, y3, curX, curY);
                            curX = x3; curY = y3;
                            break;
                        }
                    }
                } catch (Exception ignored) {}
            }
        }
        return path;
    }

    private void customLineTo(Path2D.Double path, double targetX, double targetY, double curX, double curY) {
        double distance = Math.hypot(targetX - curX, targetY - curY);
        int steps = Math.max((int) distance, 1);
        for (int i = 1; i <= steps; i++) {
            double t = (double) i / steps;
            double x = (1 - t) * curX + t * targetX;
            double y = (1 - t) * curY + t * targetY;
            path.lineTo(x, y);
        }
    }

    private void customCurveTo(Path2D.Double path, double x1, double y1, double x2, double y2, double x3, double y3, double curX, double curY) {
        int steps = 30;
        double x0 = curX, y0 = curY;
        for (int i = 1; i <= steps; i++) {
            double t = (double) i / steps;
            double u = 1 - t;
            double tt = t * t, uu = u * u;
            double uuu = uu * u, ttt = tt * t;
            double x = uuu * x0 + 3 * uu * t * x1 + 3 * u * tt * x2 + ttt * x3;
            double y = uuu * y0 + 3 * uu * t * y1 + 3 * u * tt * y2 + ttt * y3;
            path.lineTo(x, y);
        }
    }

    private AffineTransform parseGroupTransform(String content) {
        AffineTransform tx = new AffineTransform();
        Pattern pattern = Pattern.compile("transform=\"([^\"]+)\"");
        Matcher matcher = pattern.matcher(content);

        if (matcher.find()) {
            String transformStr = matcher.group(1);
            Matcher tMatch = Pattern.compile("translate\\s*\\(\\s*([-+]?\\d*\\.?\\d+)[,\\s]+([-+]?\\d*\\.?\\d+)\\s*\\)").matcher(transformStr);
            if (tMatch.find()) {
                tx.translate(Double.parseDouble(tMatch.group(1)), Double.parseDouble(tMatch.group(2)));
            }
            Matcher sMatch = Pattern.compile("scale\\s*\\(\\s*([-+]?\\d*\\.?\\d+)[,\\s]+([-+]?\\d*\\.?\\d+)\\s*\\)").matcher(transformStr);
            if (sMatch.find()) {
                tx.scale(Double.parseDouble(sMatch.group(1)), Double.parseDouble(sMatch.group(2)));
            }
        }
        return tx;
    }

    // =================== main ===================
    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            JFrame frame = new JFrame("Multi-Frame SVG with Drawable Objects");
            frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

            String[] files = {
                "test_drawSVG/a1.svg",
                "test_drawSVG/a2.svg",
                "test_drawSVG/a3.svg",
                "test_drawSVG/b1.svg",
                "test_drawSVG/b2.svg",
                "test_drawSVG/b3.svg"
            };

            frame.add(new MultiSvgAnimationApp(files));
            frame.setSize(600, 600);
            frame.setLocationRelativeTo(null);
            frame.setVisible(true);
        });
    }
}