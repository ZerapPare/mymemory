import javax.swing.*;
import java.awt.*;
import java.awt.geom.AffineTransform;
import java.awt.geom.Path2D;
import java.awt.geom.Rectangle2D;
import java.io.File;
import java.nio.file.Files;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class HardSwitchSvgApp extends JPanel {
    private Path2D shape1 = new Path2D.Double();
    private Path2D shape2 = new Path2D.Double();
    
    private boolean showShape1 = true;

    public HardSwitchSvgApp() {
        loadSvg("test_drawSVG/shot1.svg", shape1);
        loadSvg("test_drawSVG/shot2.svg", shape2);

        Timer timer = new Timer(200, e -> {
            showShape1 = !showShape1; // สลับสถานะ true <-> false
            repaint();
        });
        timer.start();
    }

    private void loadSvg(String fileName, Path2D targetPath) {
        try {
            File file = new File(fileName);
            if (!file.exists()) return;

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
        }
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        Graphics2D g2d = (Graphics2D) g;
        g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

        Path2D currentShape = showShape1 ? shape1 : shape2;

        if (currentShape != null) {
            Rectangle2D bounds = currentShape.getBounds2D();
            if (bounds.getWidth() == 0) return;

            // จัดตำแหน่งให้อยู่ตรงกลางหน้าจอ
            AffineTransform at = new AffineTransform();
            at.translate(getWidth() / 2.0, getHeight() / 2.0);
            at.translate(-bounds.getCenterX(), -bounds.getCenterY());

            Shape renderShape = at.createTransformedShape(currentShape);

            // วาดรูป
            g2d.setColor(Color.BLACK);
            g2d.fill(renderShape);
        }
    }

    private Path2D parseSvgPath(String d) {
        Path2D.Double path = new Path2D.Double();
        Pattern p = Pattern.compile("([a-zA-Z])|([-+]?\\d*\\.?\\d+(?:[eE][-+]?\\d+)?)");
        Matcher m = p.matcher(d);

        String cmd = "";
        double startX = 0, startY = 0, curX = 0, curY = 0;

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
                            path.lineTo(x, y);
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
                            path.curveTo(x1, y1, x2, y2, x3, y3);
                            curX = x3; curY = y3;
                            break;
                        }
                    }
                } catch (Exception ignored) {}
            }
        }
        return path;
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
            JFrame frame = new JFrame("Frame-by-Frame SVG Switch");
            frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
            frame.add(new HardSwitchSvgApp());
            frame.setSize(800, 700);
            frame.setLocationRelativeTo(null);
            frame.setVisible(true);
        });
    }
}