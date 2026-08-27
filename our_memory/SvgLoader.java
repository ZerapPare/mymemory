import java.awt.geom.AffineTransform;
import java.awt.geom.Path2D;
import java.io.File;
import java.nio.file.Files;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * SVG Parsing / Data Loading
 *
 * อ่านไฟล์ SVG ด้วย regex แล้วแปลงคำสั่งใน d="..." เป็น Path2D
 * เส้นตรงกับเส้นโค้งคำนวณจุดเอง ไม่เรียก path.curveTo ของ Java2D
 */
public final class SvgLoader {

    private SvgLoader() {
    }

    /** โหลดหนึ่งไฟล์ ได้ Path2D ที่ผ่าน group transform คืน null ถ้าอ่านไม่ได้ */
    public static Path2D loadSvg(String fileName) {
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

    // SVG Path Parser 
    public static Path2D parseSvgPath(String d) {
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

                            // ส่ง path เข้าไปบันทึกจุด
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

                            // ส่ง path เข้าไปบันทึกจุด
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

    /** คำนวณจุดตามแนวเส้นตรง แล้วใส่ลงใน Path2D */
    public static void customLineTo(Path2D.Double path, double targetX, double targetY, double curX, double curY) {
        double distance = Math.hypot(targetX - curX, targetY - curY);
        int steps = Math.max((int) distance, 1);
        for (int i = 1; i <= steps; i++) {
            double t = (double) i / steps;
            double x = (1 - t) * curX + t * targetX;
            double y = (1 - t) * curY + t * targetY;
            path.lineTo(x, y);
        }
    }

    /** คำนวณจุดตามสูตร Cubic Bezier แล้วใส่ลงใน Path2D */
    public static void customCurveTo(Path2D.Double path, double x1, double y1, double x2, double y2, double x3, double y3, double curX, double curY) {
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

    public static AffineTransform parseGroupTransform(String content) {
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
}
