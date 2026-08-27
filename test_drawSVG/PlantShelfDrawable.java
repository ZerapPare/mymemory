import java.awt.Color;

/**
 * ชั้นวางต้นไม้ 3 กระถาง พอร์ตมาจาก Scene.drawPlantShelf / drawPottedPlant
 *
 * ย่อขนาดด้วยการคูณตัวเลขเอง ไม่มี transform ของ Java2D ให้ใช้แล้ว
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
    public void draw(Raster r, double time) {
        // time ไม่ใช้ - ชั้นวางไม่ขยับ แต่ต้องรับตาม interface
        int boardW = sc(BOARD_W);
        int boardH = sc(BOARD_H);
        int left = centreX - boardW / 2;

        int wood = Raster.argb(Art.S1_WOOD);
        int woodD = Raster.argb(Art.S1_WOOD_D);
        double edge = Math.max(1, 2 * scale);

        double[] board = Gfx.rect(left, shelfY, boardW, boardH);
        Gfx.scanlineFill(r, board, null, wood);
        Gfx.polyline(r, board, true, edge, woodD);

        // ขายึดสองอัน เอียงเข้าหากันเหมือนต้นฉบับ
        int bTop = shelfY + boardH;
        int bBot = shelfY + sc(36);
        Gfx.thickLine(r, left + sc(28), bTop, left + sc(32), bBot, edge, woodD);
        Gfx.thickLine(r, left + sc(152), bTop, left + sc(148), bBot, edge, woodD);

        for (int i = 0; i < POT_DX.length; i++) {
            drawPottedPlant(r, left + sc(POT_DX[i]), shelfY, sc(POT_RX[i]));
        }
    }

    private void drawPottedPlant(Raster r, int cx, int baseY, int rx) {
        int lip = sc(22);   // ความสูงของปากกระถางเหนือฐาน
        int rim = sc(5);    // ครึ่งความสูงของวงรีปากกระถาง

        int woodD = Raster.argb(Art.S1_WOOD_D);
        double leafEdge = Math.max(1, 2.2 * scale);

        // ใบไม้ก่อน กระถางจะได้ทับโคนก้าน - ใบละสอง Bézier กำลังสองต่อกันเป็นรูปปิด
        for (int i = -1; i <= 1; i++) {
            double tipX = cx + i * rx * 1.5;
            double tipY = baseY - sc(34) - rx - Math.abs(i) * sc(-8);

            double[] a = Gfx.bezier2(cx, baseY - lip,
                    cx + i * rx * 1.8, baseY - sc(34), tipX, tipY, 16);
            double[] b = Gfx.bezier2(tipX, tipY,
                    cx + i * rx * 0.4, baseY - sc(32), cx, baseY - lip, 16);

            // ต่อสองเส้นเป็นคอนทัวร์เดียว ตัดจุดซ้ำตรงรอยต่อออก
            double[] leaf = new double[a.length + b.length - 2];
            System.arraycopy(a, 0, leaf, 0, a.length);
            System.arraycopy(b, 2, leaf, a.length, b.length - 2);

            int fill = Raster.argb(i == 0 ? Art.S1_LEAF
                    : Art.lerp(Art.S1_LEAF, Art.S1_LEAF_D, 0.45));
            Gfx.scanlineFill(r, leaf, null, fill);
            Gfx.polyline(r, leaf, true, leafEdge, Raster.argb(Art.S1_LEAF_D));
        }

        double[] pot = {
                cx - rx, baseY - lip,
                cx + rx, baseY - lip,
                cx + rx * 0.72, baseY,
                cx - rx * 0.72, baseY,
        };
        Gfx.scanlineFill(r, pot, null, Raster.argb(Art.S1_WOOD));
        Gfx.polyline(r, pot, true, Math.max(1, 2 * scale), woodD);

        // ปากกระถางเป็นวงรี วาดด้วย midpoint ellipse
        Gfx.fillEllipse(r, cx, baseY - lip, rx, rim,
                Raster.argb(Art.lerp(Art.S1_WOOD, Color.WHITE, 0.3)));
        Gfx.polyline(r, Gfx.ellipsePoints(cx, baseY - lip, rx, rim), true, 1, woodD);
    }

    /** ย่อระยะหนึ่งค่าแล้วปัดเป็นพิกเซลเต็ม */
    private int sc(double v) {
        return (int) Math.round(v * scale);
    }
}
