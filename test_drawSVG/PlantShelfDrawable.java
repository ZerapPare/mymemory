import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.geom.GeneralPath;
import java.awt.geom.Line2D;
import java.awt.geom.Rectangle2D;

/**
 * ชั้นวางต้นไม้ 3 กระถาง พอร์ตมาจาก Scene.drawPlantShelf / drawPottedPlant
 *
 * ย่อขนาดด้วยการคูณตัวเลขเอง ไม่ใช้ g.scale() เพราะ Midpoint.ellipse พล็อต
 * ทีละพิกเซลด้วย fillRect(x, y, 1, 1) ถ้าอยู่ใต้ transform ที่ย่อ พิกเซลจะ
 * กลายเป็นเศษส่วนแล้วหายหรือเพี้ยน ต้องส่งพิกัดที่ย่อแล้วเป็นจำนวนเต็มเข้าไป
 *
 * พิกัดเป็นพิกเซลจอตรงๆ ไม่ขยายตามหน้าต่าง เหมือน WallClockDrawable
 */
public class PlantShelfDrawable implements Drawable {

    /** ขนาดดั้งเดิมใน Scene ก่อนย่อ */
    private static final double BOARD_W = 180, BOARD_H = 12;
    private static final double[] POT_DX = { 40, 94, 142 };  // ระยะจากขอบซ้ายของชั้น
    private static final int[] POT_RX = { 20, 15, 18 };

    private final int centreX;
    private final int shelfY;
    private final double scale;

    public PlantShelfDrawable(int centreX, int shelfY, double scale) {
        this.centreX = centreX;
        this.shelfY = shelfY;
        this.scale = scale;
    }

    @Override
    public void draw(Graphics2D g, double time) {
        // time ไม่ใช้ - ชั้นวางไม่ขยับ แต่ต้องรับตาม interface
        int boardW = sc(BOARD_W);
        int boardH = sc(BOARD_H);
        int left = centreX - boardW / 2;

        g.setColor(Art.S1_WOOD);
        g.fill(new Rectangle2D.Double(left, shelfY, boardW, boardH));
        g.setColor(Art.S1_WOOD_D);
        g.setStroke(new BasicStroke((float) Math.max(1, 2 * scale)));
        g.draw(new Rectangle2D.Double(left, shelfY, boardW, boardH));

        // ขายึดสองอัน เอียงเข้าหากันเหมือนต้นฉบับ
        int bTop = shelfY + boardH;
        int bBot = shelfY + sc(36);
        g.draw(new Line2D.Double(left + sc(28), bTop, left + sc(32), bBot));
        g.draw(new Line2D.Double(left + sc(152), bTop, left + sc(148), bBot));

        for (int i = 0; i < POT_DX.length; i++) {
            drawPottedPlant(g, left + sc(POT_DX[i]), shelfY, sc(POT_RX[i]));
        }
    }

    private void drawPottedPlant(Graphics2D g, int cx, int baseY, int rx) {
        int lip = sc(22);   // ความสูงของปากกระถางเหนือฐาน
        int rim = sc(5);    // ครึ่งความสูงของวงรีปากกระถาง

        // ใบไม้ก่อน กระถางจะได้ทับโคนก้าน
        g.setStroke(new BasicStroke((float) Math.max(1, 2.2 * scale),
                BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
        for (int i = -1; i <= 1; i++) {
            double tipX = cx + i * rx * 1.5;
            double tipY = baseY - sc(34) - rx - Math.abs(i) * sc(-8);
            GeneralPath leaf = new GeneralPath();
            leaf.moveTo(cx, baseY - lip);
            leaf.quadTo(cx + i * rx * 1.8, baseY - sc(34), tipX, tipY);
            leaf.quadTo(cx + i * rx * 0.4, baseY - sc(32), cx, baseY - lip);
            leaf.closePath();
            g.setColor(i == 0 ? Art.S1_LEAF : Art.lerp(Art.S1_LEAF, Art.S1_LEAF_D, 0.45));
            g.fill(leaf);
            g.setColor(Art.S1_LEAF_D);
            g.draw(leaf);
        }

        GeneralPath pot = new GeneralPath();
        pot.moveTo(cx - rx, baseY - lip);
        pot.lineTo(cx + rx, baseY - lip);
        pot.lineTo(cx + rx * 0.72, baseY);
        pot.lineTo(cx - rx * 0.72, baseY);
        pot.closePath();
        g.setColor(Art.S1_WOOD);
        g.fill(pot);
        g.setColor(Art.S1_WOOD_D);
        g.setStroke(new BasicStroke((float) Math.max(1, 2 * scale)));
        g.draw(pot);

        // ปากกระถางเป็นวงรี วาดด้วย midpoint ellipse ที่เขียนเอง
        g.setColor(Art.lerp(Art.S1_WOOD, Color.WHITE, 0.3));
        Midpoint.fillEllipse(g, cx, baseY - lip, rx, rim);
        g.setColor(Art.S1_WOOD_D);
        Midpoint.ellipse(g, cx, baseY - lip, rx, rim);
    }

    /** ย่อระยะหนึ่งค่าแล้วปัดเป็นพิกเซลเต็ม */
    private int sc(double v) {
        return (int) Math.round(v * scale);
    }
}
