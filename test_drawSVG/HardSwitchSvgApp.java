import javax.swing.*;
import java.awt.*;
import java.awt.geom.AffineTransform;
import java.awt.geom.Point2D;
import java.io.File;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.awt.image.BufferedImage;
import java.util.LinkedList;
import java.util.Queue;
import java.awt.geom.Path2D;

public class HardSwitchSvgApp extends JPanel {

    // คลาสสำหรับเก็บข้อมูลส่วนของเส้นที่พร้อมวาด
    private static class SvgSegment {
        String type; // "L" หรือ "C"
        Point2D.Double p0, p1, p2, p3;

        SvgSegment(String type, Point2D.Double p0, Point2D.Double p1) {
            this.type = type; this.p0 = p0; this.p1 = p1;
        }
        SvgSegment(String type, Point2D.Double p0, Point2D.Double p1, Point2D.Double p2, Point2D.Double p3) {
            this.type = type; this.p0 = p0; this.p1 = p1; this.p2 = p2; this.p3 = p3;
        }
    }
	public static class ColorPoint {
		public double svgX; // พิกัด X ดั้งเดิมในไฟล์ SVG
		public double svgY; // พิกัด Y ดั้งเดิมในไฟล์ SVG
		public Color color;

		public ColorPoint(double svgX, double svgY, Color color) {
			this.svgX = svgX;
			this.svgY = svgY;
			this.color = color;
		}
	}

    // private List<SvgSegment> shape1Segments = new ArrayList<>();
    // private List<SvgSegment> shape2Segments = new ArrayList<>();
    private List<List<SvgSegment>> frames = new ArrayList<>();
    private int currentFrame = 0;
	List<ColorPoint> myColors = new ArrayList<>();

    public HardSwitchSvgApp() {
        // โหลดและ Parse พิกัดเตรียมไว้ล่วงหน้า (ทำครั้งเดียว)
        String[] fileNames = {
			"test_drawSVG/a1.svg", 
			"test_drawSVG/a2.svg", 
			"test_drawSVG/a3.svg", 
			"test_drawSVG/b1.svg", 
			"test_drawSVG/b2.svg", 
			"test_drawSVG/b3.svg"
		};

		

		// ระบุสีผิวหน้า (ประมาณช่วงซ้าย-กลาง บนๆ ของตัวรูป)
		// myColors.add(new ColorPoint(0.50, 0.25, new Color(255, 224, 189)));

		// ระบุสีเสื้อ (ช่วงกลางลำตัว)
		myColors.add(new ColorPoint(104, 104, Color.BLUE));

		// ระบุสีแว่นตา / โต๊ะ (ขยับเปอร์เซ็นต์ตามตำแหน่งจริง)

		// โหลดทุกไฟล์เก็บใส่ List ของเฟรม
		for (String fileName : fileNames) {
			List<SvgSegment> frameSegments = new ArrayList<>();
			loadSvg(fileName, frameSegments);
			frames.add(frameSegments);
		}

		// Timer เลื่อนเฟรมถัดไปเรื่อยๆ (วนลูปกลับมา 0 ด้วย %)
		Timer timer = new Timer(150, e -> {
			if (!frames.isEmpty()) {
				currentFrame = (currentFrame + 1) % frames.size();
				repaint();
			}
		});
		timer.start();
    }

    private void loadSvg(String fileName, List<SvgSegment> targetList) {
        try {
            File file = new File(fileName);
            if (!file.exists()) return;

            String content = Files.readString(file.toPath());
            
            // อ่านค่า Scale/Translate จาก tag <g transform="...">
            AffineTransform groupTransform = parseGroupTransform(content);

            Pattern pathPattern = Pattern.compile("d=\"(.*?)\"", Pattern.DOTALL);
            Matcher pathMatcher = pathPattern.matcher(content);

            while (pathMatcher.find()) {
                String d = pathMatcher.group(1);
                // สกัดคำสั่งแล้วแปลงพิกัดตาม transform ทันที
                parseSvgPathToSegments(d, groupTransform, targetList);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void parseSvgPathToSegments(String d, AffineTransform transform, List<SvgSegment> targetList) {
        Pattern p = Pattern.compile("([a-zA-Z])|([-+]?\\d*\\.?\\d+(?:[eE][-+]?\\d+)?)");
        Matcher m = p.matcher(d);

        String cmd = "";
        Point2D.Double startPt = new Point2D.Double(0, 0);
        Point2D.Double curPt = new Point2D.Double(0, 0);

        while (m.find()) {
            String token = m.group();
            if (token.matches("[a-zA-Z]")) {
                cmd = token;
                if (cmd.equalsIgnoreCase("Z")) {
                    targetList.add(new SvgSegment("L", transformPoint(curPt, transform), transformPoint(startPt, transform)));
                    curPt.setLocation(startPt);
                }
            } else {
                try {
                    boolean isRel = Character.isLowerCase(cmd.charAt(0));
                    switch (cmd.toUpperCase()) {
                        case "M": {
                            double x = Double.parseDouble(token); m.find();
                            double y = Double.parseDouble(m.group());
                            if (isRel) { x += curPt.x; y += curPt.y; }
                            curPt.setLocation(x, y);
                            startPt.setLocation(x, y);
                            cmd = isRel ? "l" : "L";
                            break;
                        }
                        case "L": {
                            double x = Double.parseDouble(token); m.find();
                            double y = Double.parseDouble(m.group());
                            if (isRel) { x += curPt.x; y += curPt.y; }
                            Point2D.Double nextPt = new Point2D.Double(x, y);

                            targetList.add(new SvgSegment("L", transformPoint(curPt, transform), transformPoint(nextPt, transform)));
                            curPt.setLocation(nextPt);
                            break;
                        }
                        case "C": {
                            double x1 = Double.parseDouble(token); m.find(); double y1 = Double.parseDouble(m.group());
                            m.find(); double x2 = Double.parseDouble(m.group()); m.find(); double y2 = Double.parseDouble(m.group());
                            m.find(); double x3 = Double.parseDouble(m.group()); m.find(); double y3 = Double.parseDouble(m.group());

                            if (isRel) {
                                x1 += curPt.x; y1 += curPt.y;
                                x2 += curPt.x; y2 += curPt.y;
                                x3 += curPt.x; y3 += curPt.y;
                            }

                            Point2D.Double p1 = new Point2D.Double(x1, y1);
                            Point2D.Double p2 = new Point2D.Double(x2, y2);
                            Point2D.Double p3 = new Point2D.Double(x3, y3);

                            targetList.add(new SvgSegment("C", 
                                transformPoint(curPt, transform), 
                                transformPoint(p1, transform), 
                                transformPoint(p2, transform), 
                                transformPoint(p3, transform)));

                            curPt.setLocation(p3);
                            break;
                        }
                    }
                } catch (Exception ignored) {}
            }
        }
    }

    // แปลงพิกัด X, Y ตามค่า scale และ translate ของ SVG
    private Point2D.Double transformPoint(Point2D.Double pt, AffineTransform tx) {
        if (tx == null) return pt;
        Point2D.Double dst = new Point2D.Double();
        tx.transform(pt, dst);
        return dst;
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

	@Override
	protected void paintComponent(Graphics g) {
		super.paintComponent(g);
		if (frames.isEmpty()) return;

		BufferedImage img = new BufferedImage(getWidth(), getHeight(), BufferedImage.TYPE_INT_ARGB);
		Graphics2D gImg = img.createGraphics();
		
		// เคลียร์พื้นหลังเป็นสีขาว
		gImg.setColor(Color.WHITE);
		gImg.fillRect(0, 0, getWidth(), getHeight());

		// 1. วาดเส้นขอบสีดำลง BufferedImage
		gImg.setColor(Color.BLACK);
		gImg.setStroke(new BasicStroke(1.5f));
		List<SvgSegment> currentSegments = frames.get(currentFrame);
		
		for (SvgSegment seg : currentSegments) {
			if (seg.type.equals("L")) {
				drawBresenhamLine(gImg, (int) Math.round(seg.p0.x), (int) Math.round(seg.p0.y), 
									(int) Math.round(seg.p1.x), (int) Math.round(seg.p1.y));
			} else if (seg.type.equals("C")) {
				drawBezierCurve(gImg, seg.p0, seg.p1, seg.p2, seg.p3);
			}
		}
		gImg.dispose();

		// 2. หยอดสีโดยใช้พิกัด SVG (ส่ง null ถ้าไม่มี transform หรือส่งค่า transform ของเฟรมไป)
		fillCustomColors(img, myColors, null); 

		// 3. วาดภาพขึ้นจอ
		g.drawImage(img, 0, 0, null);
	}

    // --- Custom Drawing Algorithms ---
    public void drawBresenhamLine(Graphics g, int x0, int y0, int x1, int y1) {
        int dx = Math.abs(x1 - x0);
        int dy = Math.abs(y1 - y0);
        int sx = x0 < x1 ? 1 : -1;
        int sy = y0 < y1 ? 1 : -1;
        int err = dx - dy;
		Graphics2D g2d = (Graphics2D) g;
		g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
		g2d.setRenderingHint(RenderingHints.KEY_STROKE_CONTROL, RenderingHints.VALUE_STROKE_PURE);

        while (true) {
            // g.fillRect(x0, y0, 1, 1);
			g2d.drawLine(x0, y0, x1, y1);
            if (x0 == x1 && y0 == y1) break;
            int e2 = 2 * err;
            if (e2 > -dy) {
                err -= dy;
                x0 += sx;
            }
            if (e2 < dx) {
                err += dx;
                y0 += sy;
            }
        }
    }

    private Point2D.Double getCubicBezierPoint(Point2D.Double p0, Point2D.Double p1, Point2D.Double p2, Point2D.Double p3, double t) {
        double u = 1 - t;
        double tt = t * t;
        double uu = u * u;
        double uuu = uu * u;
        double ttt = tt * t;

        double x = uuu * p0.x + 3 * uu * t * p1.x + 3 * u * tt * p2.x + ttt * p3.x;
        double y = uuu * p0.y + 3 * uu * t * p1.y + 3 * u * tt * p2.y + ttt * p3.y;

        return new Point2D.Double(x, y);
    }

    public void drawBezierCurve(Graphics g, Point2D.Double p0, Point2D.Double p1, Point2D.Double p2, Point2D.Double p3) {
        int steps = 150;
        Point2D.Double prevPoint = p0;

        for (int i = 1; i <= steps; i++) {
            double t = i / (double) steps;
            Point2D.Double currentPoint = getCubicBezierPoint(p0, p1, p2, p3, t);
			drawBresenhamLine(g, (int) Math.round(prevPoint.x), (int) Math.round(prevPoint.y), 
                             (int) Math.round(currentPoint.x), (int) Math.round(currentPoint.y));            prevPoint = currentPoint;
        }
    }

	public void drawAndFillOutlineSvg(Graphics2D g2d, List<SvgSegment> segments, Color fillColor) {
		g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

		// 1. กำหนด Rule แบบ WIND_EVEN_ODD เพื่อแยกพื้นที่ข้างใน/ข้างนอกอัตโนมัติ
		Path2D.Double path = new Path2D.Double(Path2D.WIND_EVEN_ODD);
		
		boolean isNewSubpath = true;
		for (SvgSegment seg : segments) {
			if (isNewSubpath) {
				path.moveTo(seg.p0.x, seg.p0.y);
				isNewSubpath = false;
			}

			if (seg.type.equals("L")) {
				path.lineTo(seg.p1.x, seg.p1.y);
			} else if (seg.type.equals("C")) {
				path.curveTo(seg.p1.x, seg.p1.y, seg.p2.x, seg.p2.y, seg.p3.x, seg.p3.y);
			}

			// หากเจอจุดปิดคำสั่ง Z ให้เริ่ม Subpath ใหม่
			if (seg.p1.equals(seg.p0)) { 
				path.closePath();
				isNewSubpath = true;
			}
		}

		// 2. หยอดสีพื้นหลังรูปทรงลงไปก่อน (เช่น สีเนื้อ หรือ สีขาว)
		g2d.setColor(fillColor);
		g2d.fill(path);

		// 3. วาดเส้นขอบสีดำทับด้านบนเพื่อความคมชัด
		g2d.setColor(Color.BLACK);
		g2d.setStroke(new BasicStroke(1.5f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
		g2d.draw(path);
	}

	public void fillCustomColors(BufferedImage img, List<ColorPoint> colorPoints, AffineTransform transform) {
		for (ColorPoint cp : colorPoints) {
			// แปลงพิกัด SVG ดั้งเดิม -> พิกัดจริงบนภาพ BufferedImage
			Point2D.Double svgPt = new Point2D.Double(cp.svgX, cp.svgY);
			Point2D.Double realPt = transformPoint(svgPt, transform);

			int targetX = (int) Math.round(realPt.x);
			int targetY = (int) Math.round(realPt.y);

			// หยอดสีเฉพาะจุดที่เป็นสีขาว/ว่างเปล่า
			floodFill(img, targetX, targetY, Color.WHITE, cp.color);
		}
	}

	public void floodFill(BufferedImage img, int startX, int startY, Color targetColor, Color fillColor) {
		int targetRGB = targetColor.getRGB();
		int fillRGB = fillColor.getRGB();

		if (targetRGB == fillRGB) return;
		if (img.getRGB(startX, startY) != targetRGB) return;

		Queue<Point> queue = new LinkedList<>();
		queue.add(new Point(startX, startY));

		while (!queue.isEmpty()) {
			Point p = queue.poll();
			int x = p.x;
			int y = p.y;

			if (x < 0 || x >= img.getWidth() || y < 0 || y >= img.getHeight()) continue;
			if (img.getRGB(x, y) != targetRGB) continue;

			img.setRGB(x, y, fillRGB);

			// กระจาย 4 ทิศทาง (ซ้าย, ขวา, บน, ล่าง)
			queue.add(new Point(x + 1, y));
			queue.add(new Point(x - 1, y));
			queue.add(new Point(x, y + 1));
			queue.add(new Point(x, y - 1));
		}
	}

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            JFrame frame = new JFrame("Frame-by-Frame SVG Switch");
            frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
            frame.add(new HardSwitchSvgApp());
            frame.setSize(800, 700);
            frame.setLocationRelativeTo(null);
            frame.setVisible(true);
        });
    }
}