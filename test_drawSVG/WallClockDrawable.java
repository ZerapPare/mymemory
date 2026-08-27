/**
 * นาฬิกาแขวน - วงกลมจาก midpoint ellipse เข็มกับขีดจาก Bresenham
 *
 * สีมาจาก ArtConfig ที่เดียว
 */
public class WallClockDrawable implements Drawable {
    private final int cx, cy;
    private final double speed;  // รอบต่อวินาที (ปรับได้)

    public WallClockDrawable(int cx, int cy, double speed) {
        this.cx = cx;
        this.cy = cy;
        this.speed = speed;
    }

    @Override
    public void draw(Raster r, double time) {
        drawWallClock(r, cx, cy, time * speed);
    }

    private void drawWallClock(Raster r, int cx, int cy, double t) {
        int face = Raster.argb(ArtConfig.CLOCK_FACE);
        int ink = Raster.argb(ArtConfig.INK);

        Gfx.fillEllipse(r, cx, cy, 34, 34, face);
        Gfx.polyline(r, Gfx.ellipsePoints(cx, cy, 36, 36), true, 1, ink);
        Gfx.polyline(r, Gfx.ellipsePoints(cx, cy, 35, 35), true, 1, ink);
        Gfx.polyline(r, Gfx.ellipsePoints(cx, cy, 30, 30), true, 1, ink);

        for (int i = 0; i < 12; i++) {
            double a = i * Math.PI / 6.0;
            double s = (i % 3 == 0) ? 21 : 24;
            Gfx.thickLine(r,
                    cx + Math.sin(a) * s, cy - Math.cos(a) * s,
                    cx + Math.sin(a) * 27, cy - Math.cos(a) * 27, 2, ink);
        }

        hand(r, cx, cy, Math.toRadians(150), 13, 3, ink);
        hand(r, cx, cy, Math.toRadians(348), 21, 2.4, ink);

        double revs = t / 2.5 * 3.0;
        double stepped = Math.floor(revs * 20.0) / 20.0;
        hand(r, cx, cy, stepped * 2 * Math.PI, 26, 1.8, Raster.argb(ArtConfig.CLOCK_SECOND));

        Gfx.fillEllipse(r, cx, cy, 2, 2, ink);
    }

    private void hand(Raster r, int cx, int cy, double angle, double length,
                      double width, int argb) {
        double ex = cx + Math.sin(angle) * length;
        double ey = cy - Math.cos(angle) * length;
        Gfx.thickLine(r, cx, cy, ex, ey, width, argb);
    }
}
