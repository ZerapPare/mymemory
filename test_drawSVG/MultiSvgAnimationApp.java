import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.geom.AffineTransform;
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

public class MultiSvgAnimationApp extends JPanel {
    private final List<Path2D> frames = new ArrayList<>();
    private int currentIndex = 0;

    // ============ THE PALETTE - เปลี่ยนสีที่นี่ที่เดียว ============
    // ตารางข้างล่างบอกแค่ว่าสี "ไปลงตรงไหน" ไม่ได้บอกว่าเป็นสีอะไร
    private static final Color BACKDROP = new Color(0xF2E4CB);
    private static final Color HAIR     = new Color(0x6B4A2E);
    private static final Color SKIN     = new Color(0xFFDCB8);
    private static final Color SHIRT    = new Color(0x7FA8D4);
    private static final Color SCREEN   = new Color(0xDCEBFF);
    // ============================================================

    /** จุดเริ่มเทสีหนึ่งจุด พิกัดเป็นหน่วย viewBox (0..600 x 0..384) ไม่ใช่พิกเซลจอ */
    private static class Seed {
        final double x, y;
        final Color color;
        Seed(double x, double y, Color color) { this.x = x; this.y = y; this.color = color; }
    }

    /**
     * พิกัดพวกนี้วัดมาจากภาพที่เรนเดอร์จริง ไม่ได้เดา - label ช่องว่างทุกก้อนแล้ว
     * ใช้กึ่งกลางของแถวที่กว้างที่สุดของแต่ละก้อน (ไม่ใช่ centroid เพราะบริเวณ
     * เว้าอย่างแขนจะทำให้ centroid หลุดออกไปนอกรูป)
     *
     * ทั้ง 6 ไฟล์เป็นภาพต่างกันจริงแค่ 3 แบบ (a1=b1, a2=b2, a3=b3) และมีแต่
     * แท็บเล็ตที่ขยับ ส่วนอื่นอยู่ที่เดิมทุกเฟรม จึงแยกเป็นสองตาราง
     */
    private static final Seed[] COMMON = {
        new Seed( 30.0,  30.0, BACKDROP), // พื้นหลัง - รวมโต๊ะด้วย เพราะเส้นขอบโต๊ะไม่ปิด
        new Seed(229.8, 207.6, SHIRT),    // ลำตัว
        new Seed(301.0,  75.5, HAIR),     // ผม
        new Seed(294.4, 142.0, SKIN),     // ใบหน้า
        new Seed(350.3, 243.5, SKIN),     // ท่อนแขน
        new Seed(306.4, 200.0, SKIN),     // มือที่คาง
    };

    /** แท็บเล็ตเป็นอย่างเดียวที่ขยับ เลยต้องมี seed ต่อเฟรม */
    private static final Seed[] SCREEN_PER_FRAME = {
        new Seed(411.7, 289.3, SCREEN), // a1
        new Seed(365.4, 323.6, SCREEN), // a2
        new Seed(404.5, 294.9, SCREEN), // a3
        new Seed(411.7, 289.3, SCREEN), // b1 (ภาพเดียวกับ a1)
        new Seed(365.4, 323.6, SCREEN), // b2 (ภาพเดียวกับ a2)
        new Seed(404.5, 294.9, SCREEN), // b3 (ภาพเดียวกับ a3)
    };

    /** เทสีลงเฉพาะพิกเซลขาวล้วน เส้นหมึกและสีที่เทไปแล้วกั้นอยู่ */
    private static final int BLANK = 0xFFFFFF;

    /**
     * ทุกเฟรมใช้กรอบนี้ร่วมกัน ไม่ใช้ bounds ของแต่ละเฟรม - ไม่งั้นตัวการ์ตูน
     * จะเต้นตอนสลับ และ seed ที่วัดไว้จะเลื่อนตามไปด้วย
     * (a1/b1 เป็น 600x383 ที่เหลือ 600x384 ต่างกัน 0.26% ใช้ค่าเดียวได้)
     */
    private static final double VBW = 600, VBH = 384, PAD = 24;

    private final List<BufferedImage> buffers = new ArrayList<>();
    private int lastW = -1, lastH = -1;
    private boolean colorOn = true;

    /** ชื่อสั้นของแต่ละเฟรม (a1, a2, ...) ไว้โชว์บนจอตอนเก็บพิกัด */
    private final List<String> names = new ArrayList<>();
    private Timer timer;
    private boolean paused = false;

    /** ข้อความจากการคลิกครั้งล่าสุด โชว์บนจอเพื่อไม่ต้องคอยมองคอนโซล */
    private String pickText = "click to pick a point";

    public MultiSvgAnimationApp(String[] filePaths) {
        for (String filePath : filePaths) {
            Path2D path = loadSvg(filePath);
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
            timer = new Timer(350, e -> {
                currentIndex = (currentIndex + 1) % frames.size();
                repaint();
            });
            timer.start();
        }
    }

    /**
     * คลิกแล้วบอกพิกัดในหน่วย viewBox พร้อมสีที่จุดนั้น และพิมพ์บรรทัด
     * new Seed(...) สำเร็จรูปให้คัดลอกไปวางในตารางข้างบนได้เลย
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

        double s = Math.min(Math.max(1, w - 2 * PAD) / VBW, Math.max(1, h - 2 * PAD) / VBH);
        double ux = (px - w / 2.0) / s + VBW / 2;
        double uy = (py - h / 2.0) / s + VBH / 2;

        String frame = currentIndex < names.size() ? names.get(currentIndex) : "?";
        String what = "out";
        if (currentIndex < buffers.size() && px >= 0 && px < w && py >= 0 && py < h) {
            int rgb = buffers.get(currentIndex).getRGB(px, py) & 0xFFFFFF;
            what = describe(rgb);
        }

        pickText = String.format("%s  (%.1f, %.1f)  %s", frame, ux, uy, what);
        System.out.printf("[pick] %s%n        new Seed(%.1f, %.1f, SKIN),   // <- fix SKIN %n",
                pickText, ux, uy);
        repaint();
    }

    /** บอกว่าพิกเซลนั้นคืออะไร จะได้รู้ว่าคลิกโดนเส้นหรือโดนช่องว่าง */
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

    /**
     * สร้างภาพทุกเฟรมไว้ล่วงหน้าหนึ่งครั้งต่อขนาดหน้าต่าง
     *
     * ที่ต้องวาดลง BufferedImage ไม่ใช่วาดลงจอตรงๆ เพราะ flood fill ต้อง "อ่าน"
     * พิกเซลกลับมา ซึ่ง Graphics2D ของ panel ทำไม่ได้ ผลพลอยได้คือสลับเฟรม
     * เหลือแค่ blit ภาพเดียว
     */
    private void ensureFrames(int w, int h) {
        if (w <= 0 || h <= 0 || (w == lastW && h == lastH && !buffers.isEmpty())) return;

        double s = Math.min(Math.max(1, w - 2 * PAD) / VBW, Math.max(1, h - 2 * PAD) / VBH);
        AffineTransform at = new AffineTransform();
        at.translate(w / 2.0, h / 2.0);
        at.scale(s, s);
        at.translate(-VBW / 2, -VBH / 2);

        buffers.clear();
        for (int i = 0; i < frames.size(); i++) {
            buffers.add(rasterise(frames.get(i), i, at, w, h));
        }
        lastW = w;
        lastH = h;
    }

    /** วาดลายเส้นลงภาพ แล้วเทสีทีละ seed */
    private BufferedImage rasterise(Path2D path, int frameIndex, AffineTransform at, int w, int h) {
        BufferedImage img = new BufferedImage(w, h, BufferedImage.TYPE_INT_RGB);
        Graphics2D g = img.createGraphics();
        g.setColor(Color.WHITE);
        g.fillRect(0, 0, w, h);

        // ปิด AA ไม่ใช่เรื่องความสวย แต่เพราะ flood fill ต้องการขอบคมชัด
        // ถ้าเปิด ขอบหมึกจะเป็นเทาไล่เฉด สีจะหยุดก่อนถึงเส้นแล้วเหลือขอบขาวซีด
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_OFF);
        g.setStroke(new BasicStroke(1f));
        g.setColor(Color.BLACK);

        Shape sh = at.createTransformedShape(path);
        g.fill(sh);
        // ลากขอบทับไม่ใช่การตกแต่ง แต่เป็นการอุดรู เส้น potrace เป็นสลิ่วบางที่
        // แค่แตะกัน ถมอย่างเดียวเหลือรูจิ๋วให้สีลอดทะลุไปบริเวณข้างเคียง
        g.draw(sh);
        g.dispose();

        if (!colorOn) return img;

        int[] px = ((DataBufferInt) img.getRaster().getDataBuffer()).getData();

        // seed ที่วัดไว้ที่ขนาดหนึ่ง อาจไปตกใต้เส้นที่อีกขนาดเพราะเส้นขยับตาม
        // สเกล ให้ขยับหาพิกเซลว่างใกล้สุดได้ 1.5 หน่วย viewBox ซึ่งแคบกว่า
        // ระยะระหว่างบริเวณมาก จึงข้ามเส้นไปผิดฝั่งไม่ได้
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
                // ไม่มีที่ให้เท และสีที่อยู่ตรงนั้นก็ผิด แปลว่าบริเวณนี้รวมเข้ากับ
                // เพื่อนบ้านคนละสี ถ้าสีตรงกันถือว่าไม่เป็นไร จึงเตือนเฉพาะกรณีผิด
                System.err.println("[seed] (" + seed.x + ", " + seed.y + ") ที่ " + w + "x" + h
                        + " - บริเวณรวมกับเพื่อนบ้านคนละสี");
            }
        }
        return img;
    }

    private static double s(AffineTransform at) {
        return at.getScaleX();
    }

    /**
     * flood fill แบบ span 4 ทิศ
     *
     * ใช้ 4 ทิศไม่ใช่ 8 เพื่อไม่ให้สีเล็ดลอดตามแนวทแยงระหว่างพิกเซลหมึกสองตัว
     * ที่แตะกันแค่มุมเดียว เติมทั้งแถวรวดเดียวแล้ว push แค่จุดเดียวต่อแถวใหม่
     * สแต็กเลยอยู่ระดับร้อย ไม่ใช่หนึ่งช่องต่อพิกเซล - บริเวณเดียวที่นี่อาจ
     * ใหญ่เกือบเต็มจอ ถ้าใช้ recursion จะ StackOverflow
     */
    private static void floodFill(int[] px, int w, int h, int sx, int sy, int rgb) {
        int fill = rgb & 0xFFFFFF;
        if (fill == BLANK || (px[sy * w + sx] & 0xFFFFFF) != BLANK) return;

        int[] stack = new int[1024];
        int sp = 0;
        stack[sp++] = sy * w + sx;

        while (sp > 0) {
            int p = stack[--sp];
            if ((px[p] & 0xFFFFFF) != BLANK) continue; // แถวก่อนหน้าเติมไปแล้ว
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

    /**
     * พิกเซลว่างที่ใกล้ seed ที่สุด ไล่หาเป็นวงทีละชั้น บริเวณที่เทสีไปแล้ว
     * ไม่นับว่าว่าง การขยับจึงไปแย่งสีของเพื่อนบ้านไม่ได้ อย่างแย่ที่สุดคือ
     * หาไม่เจอแล้วฟ้อง
     */
    private static int findBlankNear(int[] px, int w, int h, int sx, int sy, int maxR) {
        for (int r = 0; r <= maxR; r++) {
            for (int dy = -r; dy <= r; dy++) {
                for (int dx = -r; dx <= r; dx++) {
                    if (r > 0 && Math.abs(dx) != r && Math.abs(dy) != r) continue; // เอาเฉพาะขอบวง
                    int x = sx + dx, y = sy + dy;
                    if (x >= 0 && x < w && y >= 0 && y < h && (px[y * w + x] & 0xFFFFFF) == BLANK) {
                        return y * w + x;
                    }
                }
            }
        }
        return -1;
    }

    /** มีพิกเซลในรัศมีที่เป็นสีที่ seed นี้ต้องการอยู่แล้วหรือไม่ */
    private static boolean hasColourNear(int[] px, int w, int h, int sx, int sy, int maxR, int rgb) {
        for (int y = Math.max(0, sy - maxR); y <= Math.min(h - 1, sy + maxR); y++) {
            for (int x = Math.max(0, sx - maxR); x <= Math.min(w - 1, sx + maxR); x++) {
                if ((px[y * w + x] & 0xFFFFFF) == rgb) return true;
            }
        }
        return false;
    }

    private Path2D parseSvgPath(String d) {
        Path2D.Double path = new Path2D.Double();
        Pattern p = Pattern.compile("([a-zA-Z])|([-+]?\\d*\\.?\\d+(?:[eE][-+]?\\d+)?)");
        Matcher m = p.matcher(d);

        String cmd = "";
        double startX = 0, startY = 0;

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
                            
                            // ส่ง path เข้าไปบันทึกจุด
                            customLineTo(path, x, y); 
                            break;
                        }
                        case "C": {
                            double x1 = Double.parseDouble(token); m.find(); double y1 = Double.parseDouble(m.group());
                            m.find(); double x2 = Double.parseDouble(m.group()); m.find(); double y2 = Double.parseDouble(m.group());
                            m.find(); double x3 = Double.parseDouble(m.group()); m.find(); double y3 = Double.parseDouble(m.group());
                            if (isRel) {
                                x1 += curX; y1 += curY; x2 += curX; y2 += curY; x3 += curX; y3 += curY;
                            }
                            
                            // ส่ง path เข้าไปบันทึกจุด
                            customCurveTo(path, x1, y1, x2, y2, x3, y3); 
                            break;
                        }
                    }
                } catch (Exception ignored) {}
            }
        }
        return path;
    }

    private double curX = 0;
    private double curY = 0;

    /**
     * คำนวณจุดตามแนวเส้นตรง แล้วใส่ลงใน Path2D
     */
    public void customLineTo(Path2D.Double path, double targetX, double targetY) {
        double distance = Math.hypot(targetX - curX, targetY - curY);
        int steps = Math.max((int) distance, 1);

        for (int i = 1; i <= steps; i++) {
            double t = (double) i / steps;
            double x = (1 - t) * curX + t * targetX;
            double y = (1 - t) * curY + t * targetY;
            
            path.lineTo(x, y);
        }

        curX = targetX;
        curY = targetY;
    }

    /**
     * คำนวณจุดตามสูตร Cubic Bézier แล้วใส่ลงใน Path2D
     */
    public void customCurveTo(Path2D.Double path, double x1, double y1, double x2, double y2, double x3, double y3) {
        int steps = 30; // 30-50 steps เพียงพอต่อความเนียนและประหยัด RAM
        double x0 = curX;
        double y0 = curY;

        for (int i = 1; i <= steps; i++) {
            double t = (double) i / steps;
            double u = 1 - t;

            double tt = t * t;
            double uu = u * u;
            double uuu = uu * u;
            double ttt = tt * t;

            double x = uuu * x0 + 3 * uu * t * x1 + 3 * u * tt * x2 + ttt * x3;
            double y = uuu * y0 + 3 * uu * t * y1 + 3 * u * tt * y2 + ttt * y3;

            path.lineTo(x, y);
        }

        curX = x3;
        curY = y3;
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

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            JFrame frame = new JFrame("Multi-Frame SVG Switch");
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
            frame.setSize(800, 700);
            frame.setLocationRelativeTo(null);
            frame.setVisible(true);
        });
    }
}