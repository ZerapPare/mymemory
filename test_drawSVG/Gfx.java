import java.util.Arrays;

/**
 * รูปทรงทั้งหมด สร้างจาก Raster.plot() อย่างเดียว ไม่มีคำสั่งวาดของ Java2D
 *
 * แบ่งเป็นสามพวก
 *   1. ตัวสร้างรูปทรง - คายลิสต์จุด {x0,y0,x1,y1,...} ออกมา ไม่พล็อตเอง
 *      เอาไป polyline() = วาดขอบ  เอาไป scanlineFill() = ถมไส้  ใช้จุดชุดเดียวกัน
 *   2. ตัววาดเส้น - Bresenham
 *   3. ตัวถม - scanline (nonzero winding) กับ flood fill 4 ทิศ
 *
 * ที่ต้องคายจุดแทนที่จะพล็อตเลย เพราะเกือบทุกรูปทรงในโปรเจกต์ต้องทั้งถมไส้และ
 * วาดขอบทับ ถ้าพล็อตเองต้องเขียนอัลกอริทึมซ้ำสองรอบ
 */
public final class Gfx {

    private Gfx() {
    }

    // =================================================================
    // 1. ตัวสร้างรูปทรง - คายจุด
    // =================================================================

    public static double[] rect(double x, double y, double w, double h) {
        return new double[] { x, y, x + w, y, x + w, y + h, x, y + h };
    }

    /**
     * midpoint ellipse - คืน "x ที่กว้างที่สุด" ของแต่ละแถว ดัชนีคือระยะจากกึ่งกลาง
     *
     * ยกอัลกอริทึมมาจาก Midpoint.fillEllipse เดิม แยก region 1 / region 2
     * ตามจุดที่ความชันเปลี่ยน คืนเป็นตารางเพื่อใช้ได้ทั้งถมและสร้างจุดขอบ
     */
    public static int[] ellipseSpans(int rx, int ry) {
        if (rx < 0 || ry < 0) return new int[] { 0 };

        int[] maxX = new int[ry + 1];
        Arrays.fill(maxX, -1);

        double rx2 = (double) rx * rx;
        double ry2 = (double) ry * ry;
        double x = 0, y = ry;
        double dx = 0, dy = 2 * rx2 * y;
        double p = ry2 - rx2 * ry + 0.25 * rx2;

        while (dx < dy) {
            widen(maxX, (int) y, (int) x);
            x++;
            dx += 2 * ry2;
            if (p < 0) {
                p += ry2 + dx;
            } else {
                y--;
                dy -= 2 * rx2;
                p += ry2 + dx - dy;
            }
        }

        p = ry2 * (x + 0.5) * (x + 0.5) + rx2 * (y - 1) * (y - 1) - rx2 * ry2;
        while (y >= 0) {
            widen(maxX, (int) y, (int) x);
            y--;
            dy -= 2 * rx2;
            if (p > 0) {
                p += rx2 - dy;
            } else {
                x++;
                dx += 2 * ry2;
                p += rx2 - dy + dx;
            }
        }

        // แถวที่อัลกอริทึมข้ามไป (ตอนไล่เร็วในแนวตั้ง) ยืมค่าจากแถวที่แคบกว่าถัดไป
        int carry = 0;
        for (int i = ry; i >= 0; i--) {
            if (maxX[i] < 0) maxX[i] = carry;
            carry = maxX[i];
        }
        return maxX;
    }

    private static void widen(int[] maxX, int row, int x) {
        if (row >= 0 && row < maxX.length && x > maxX[row]) maxX[row] = x;
    }

    /** จุดรอบวงรี เรียงตามเส้นรอบวง - ขึ้นข้างขวาแล้ววนลงข้างซ้าย */
    public static double[] ellipsePoints(double cx, double cy, int rx, int ry) {
        int[] maxX = ellipseSpans(rx, ry);
        double[] pts = new double[(ry * 2 + 1) * 4];
        int n = 0;
        for (int dy = -ry; dy <= ry; dy++) {
            pts[n++] = cx + maxX[Math.abs(dy)];
            pts[n++] = cy + dy;
        }
        for (int dy = ry; dy >= -ry; dy--) {
            pts[n++] = cx - maxX[Math.abs(dy)];
            pts[n++] = cy + dy;
        }
        return Arrays.copyOf(pts, n);
    }

    /**
     * ส่วนโค้ง - คัดจุดจากวงรีเต็มวงเฉพาะช่วงมุมที่ต้องการ
     *
     * มุมนับแบบเดียวกับ Graphics.drawArc คือ 0 องศาอยู่ทางขวา ทวนเข็มเป็นบวก
     * ซึ่งในพิกัดจอที่แกน y ชี้ลง แปลว่าต้องกลับเครื่องหมาย y
     */
    public static double[] arcPoints(double cx, double cy, int rx, int ry,
                                     double startDeg, double extentDeg) {
        int[] maxX = ellipseSpans(rx, ry);
        double lo = Math.min(startDeg, startDeg + extentDeg);
        double hi = Math.max(startDeg, startDeg + extentDeg);

        double[] pts = new double[(ry * 2 + 1) * 4];
        int n = 0;
        for (int dy = -ry; dy <= ry; dy++) {
            n = keep(pts, n, cx + maxX[Math.abs(dy)], cy + dy, cx, cy, lo, hi);
        }
        for (int dy = ry; dy >= -ry; dy--) {
            n = keep(pts, n, cx - maxX[Math.abs(dy)], cy + dy, cx, cy, lo, hi);
        }
        return Arrays.copyOf(pts, n);
    }

    private static int keep(double[] pts, int n, double x, double y,
                            double cx, double cy, double lo, double hi) {
        double a = Math.toDegrees(Math.atan2(cy - y, x - cx));
        // เลื่อนให้ตกในช่วง [lo, lo+360) ก่อนเทียบ
        while (a < lo) a += 360;
        while (a >= lo + 360) a -= 360;
        if (a <= hi) {
            pts[n++] = x;
            pts[n++] = y;
        }
        return n;
    }

    /** สี่เหลี่ยมมุมมน - 4 ด้าน + 4 ส่วนโค้งที่มุม */
    public static double[] roundRect(double x, double y, double w, double h,
                                     double arcW, double arcH) {
        int rx = (int) Math.round(Math.min(arcW, w) / 2);
        int ry = (int) Math.round(Math.min(arcH, h) / 2);
        if (rx <= 0 || ry <= 0) return rect(x, y, w, h);

        double l = x, t = y, rr = x + w, b = y + h;
        double[] out = new double[4096];
        int n = 0;

        n = append(out, n, arcPoints(l + rx, t + ry, rx, ry, 90, 90));    // มุมบนซ้าย
        n = push(out, n, l, t + ry);
        n = push(out, n, l, b - ry);
        n = append(out, n, arcPoints(l + rx, b - ry, rx, ry, 180, 90));   // ล่างซ้าย
        n = push(out, n, l + rx, b);
        n = push(out, n, rr - rx, b);
        n = append(out, n, arcPoints(rr - rx, b - ry, rx, ry, 270, 90));  // ล่างขวา
        n = push(out, n, rr, b - ry);
        n = push(out, n, rr, t + ry);
        n = append(out, n, arcPoints(rr - rx, t + ry, rx, ry, 0, 90));    // บนขวา
        n = push(out, n, rr - rx, t);
        n = push(out, n, l + rx, t);

        return Arrays.copyOf(out, n);
    }

    private static int push(double[] out, int n, double x, double y) {
        out[n++] = x;
        out[n++] = y;
        return n;
    }

    private static int append(double[] out, int n, double[] src) {
        System.arraycopy(src, 0, out, n, src.length);
        return n + src.length;
    }

    /** Bézier กำลังสอง - คายจุดตามสูตรพารามิเตอร์ ไม่ได้ใช้ quadTo ของ Java2D */
    public static double[] bezier2(double x0, double y0, double x1, double y1,
                                   double x2, double y2, int steps) {
        double[] pts = new double[(steps + 1) * 2];
        for (int i = 0; i <= steps; i++) {
            double t = (double) i / steps;
            double u = 1 - t;
            pts[i * 2] = u * u * x0 + 2 * u * t * x1 + t * t * x2;
            pts[i * 2 + 1] = u * u * y0 + 2 * u * t * y1 + t * t * y2;
        }
        return pts;
    }

    /** Bézier กำลังสาม - สูตรเดียวกับ SvgLoader.customCurveTo */
    public static double[] bezier3(double x0, double y0, double x1, double y1,
                                   double x2, double y2, double x3, double y3, int steps) {
        double[] pts = new double[(steps + 1) * 2];
        for (int i = 0; i <= steps; i++) {
            double t = (double) i / steps;
            double u = 1 - t;
            double tt = t * t, uu = u * u;
            double uuu = uu * u, ttt = tt * t;
            pts[i * 2] = uuu * x0 + 3 * uu * t * x1 + 3 * u * tt * x2 + ttt * x3;
            pts[i * 2 + 1] = uuu * y0 + 3 * uu * t * y1 + 3 * u * tt * y2 + ttt * y3;
        }
        return pts;
    }

    // =================================================================
    // 2. ตัววาดเส้น
    // =================================================================

    /** Bresenham เต็มทั้ง 8 octant หนา 1 พิกเซล */
    public static void line(Raster r, int x0, int y0, int x1, int y1, int argb) {
        int dx = Math.abs(x1 - x0);
        int dy = -Math.abs(y1 - y0);
        int sx = x0 < x1 ? 1 : -1;
        int sy = y0 < y1 ? 1 : -1;
        int err = dx + dy;

        while (true) {
            r.plot(x0, y0, argb);
            if (x0 == x1 && y0 == y1) break;
            int e2 = 2 * err;
            if (e2 >= dy) {
                err += dy;
                x0 += sx;
            }
            if (e2 <= dx) {
                err += dx;
                y0 += sy;
            }
        }
    }

    /**
     * เส้นหนา - เดิน Bresenham แล้ววางวงกลมทึบทุกก้าว
     *
     * ได้ปลายมนกับข้อต่อมนมาเอง ตรงกับ BasicStroke(CAP_ROUND, JOIN_ROUND)
     * ที่โค้ดเดิมใช้แทบทุกที่
     */
    public static void thickLine(Raster r, double x0, double y0, double x1, double y1,
                                 double width, int argb) {
        // วงกลมรัศมี radius กว้าง 2*radius+1 พิกเซล จึงต้องใช้ floor ไม่ใช่ round
        // ไม่งั้นเส้นหนา 3 จะกลายเป็น 5 แล้วไปปิดช่องที่ควรเปิดไว้ให้ flood fill
        int radius = (int) Math.floor(width / 2.0);
        if (radius < 1) {
            line(r, (int) Math.round(x0), (int) Math.round(y0),
                    (int) Math.round(x1), (int) Math.round(y1), argb);
            return;
        }

        int[] disc = ellipseSpans(radius, radius);
        int ix0 = (int) Math.round(x0), iy0 = (int) Math.round(y0);
        int ix1 = (int) Math.round(x1), iy1 = (int) Math.round(y1);

        int dx = Math.abs(ix1 - ix0);
        int dy = -Math.abs(iy1 - iy0);
        int sx = ix0 < ix1 ? 1 : -1;
        int sy = iy0 < iy1 ? 1 : -1;
        int err = dx + dy;

        while (true) {
            stamp(r, ix0, iy0, disc, radius, argb);
            if (ix0 == ix1 && iy0 == iy1) break;
            int e2 = 2 * err;
            if (e2 >= dy) {
                err += dy;
                ix0 += sx;
            }
            if (e2 <= dx) {
                err += dx;
                iy0 += sy;
            }
        }
    }

    private static void stamp(Raster r, int cx, int cy, int[] maxX, int radius, int argb) {
        for (int dy = -radius; dy <= radius; dy++) {
            int x = maxX[Math.abs(dy)];
            r.span(cy + dy, cx - x, cx + x, argb);
        }
    }

    /** ลากเส้นต่อกันตามลิสต์จุด - close = ลากปิดกลับจุดแรก */
    public static void polyline(Raster r, double[] pts, boolean close, double width, int argb) {
        int n = pts.length / 2;
        if (n < 2) return;
        for (int i = 0; i + 1 < n; i++) {
            segment(r, pts[i * 2], pts[i * 2 + 1], pts[i * 2 + 2], pts[i * 2 + 3], width, argb);
        }
        if (close) {
            segment(r, pts[(n - 1) * 2], pts[(n - 1) * 2 + 1], pts[0], pts[1], width, argb);
        }
    }

    private static void segment(Raster r, double x0, double y0, double x1, double y1,
                                double width, int argb) {
        if (width <= 1.5) {
            line(r, (int) Math.round(x0), (int) Math.round(y0),
                    (int) Math.round(x1), (int) Math.round(y1), argb);
        } else {
            thickLine(r, x0, y0, x1, y1, width, argb);
        }
    }

    /**
     * เขียนข้อความด้วยฟอนต์บิตแมป - มาแทน g.drawString()
     *
     * x, y คือมุมซ้ายบนของตัวอักษรตัวแรก ไม่ใช่เส้นฐานแบบ drawString
     */
    public static void text(Raster r, int x, int y, String s, int argb) {
        for (int i = 0; i < s.length(); i++) {
            int[] cols = Font5x7.glyph(s.charAt(i));
            int gx = x + i * Font5x7.ADVANCE;
            for (int col = 0; col < Font5x7.W; col++) {
                int bits = cols[col];
                for (int row = 0; row < Font5x7.H; row++) {
                    if ((bits & (1 << row)) != 0) r.plot(gx + col, y + row, argb);
                }
            }
        }
    }

    // =================================================================
    // 2.5 คอนทัวร์จาก Path2D
    // =================================================================

    /**
     * ลิสต์จุดของรูปปิดหลายวง - pts เป็น {x,y} เรียงติดกัน
     * ends[i] บอกว่าคอนทัวร์ที่ i จบที่จุดลำดับไหน (หน่วยเป็นจำนวนจุด)
     */
    public static final class Contours {
        public final double[] pts;
        public final int[] ends;

        public Contours(double[] pts, int[] ends) {
            this.pts = pts;
            this.ends = ends;
        }
    }

    /**
     * อ่านจุดออกจาก Path2D ที่ SvgLoader แตกไว้แล้ว
     *
     * PathIterator เป็นแค่ตัวอ่านข้อมูล ไม่ได้วาดอะไร และ path ที่ SvgLoader คืนมา
     * มีแต่ MOVETO/LINETO/CLOSE ล้วน เพราะ customLineTo/customCurveTo คำนวณจุด
     * เองหมดแล้ว ไม่มีเส้นโค้งจริงเหลือให้ต้อง flatten ซ้ำ
     */
    public static Contours contours(java.awt.Shape sh) {
        double[] buf = new double[6];
        int n = 0;
        for (java.awt.geom.PathIterator it = sh.getPathIterator(null); !it.isDone(); it.next()) {
            int t = it.currentSegment(buf);
            if (t == java.awt.geom.PathIterator.SEG_MOVETO
                    || t == java.awt.geom.PathIterator.SEG_LINETO) {
                n++;
            }
        }

        double[] pts = new double[n * 2];
        int[] endBuf = new int[n + 1];
        int p = 0, e = 0;
        for (java.awt.geom.PathIterator it = sh.getPathIterator(null); !it.isDone(); it.next()) {
            int t = it.currentSegment(buf);
            if (t == java.awt.geom.PathIterator.SEG_MOVETO) {
                if (p > 0 && (e == 0 || endBuf[e - 1] != p)) endBuf[e++] = p;
                pts[p * 2] = buf[0];
                pts[p * 2 + 1] = buf[1];
                p++;
            } else if (t == java.awt.geom.PathIterator.SEG_LINETO) {
                pts[p * 2] = buf[0];
                pts[p * 2 + 1] = buf[1];
                p++;
            } else if (t == java.awt.geom.PathIterator.SEG_CLOSE) {
                if (e == 0 || endBuf[e - 1] != p) endBuf[e++] = p;
            }
        }
        if (p > 0 && (e == 0 || endBuf[e - 1] != p)) endBuf[e++] = p;

        return new Contours(pts, Arrays.copyOf(endBuf, e));
    }

    /** ลากขอบของทุกคอนทัวร์ - ใช้คู่กับ scanlineFill เพื่อปิดรูจิ๋วระหว่างสลิ่ว */
    public static void strokeContours(Raster r, Contours c, double width, int argb) {
        int start = 0;
        for (int end : c.ends) {
            int count = end - start;
            if (count >= 2) {
                double[] sub = new double[count * 2];
                System.arraycopy(c.pts, start * 2, sub, 0, count * 2);
                polyline(r, sub, true, width, argb);
            }
            start = end;
        }
    }

    // =================================================================
    // 3. ตัวถม
    // =================================================================

    /** ถมวงรีด้วย scanline โดยตรง - ไม่ต้องผ่าน scanlineFill เพราะรู้ span อยู่แล้ว */
    public static void fillEllipse(Raster r, int cx, int cy, int rx, int ry, int argb) {
        if (rx < 0 || ry < 0) return;
        int[] maxX = ellipseSpans(rx, ry);
        for (int dy = -ry; dy <= ry; dy++) {
            int x = maxX[Math.abs(dy)];
            r.span(cy + dy, cx - x, cx + x, argb);
        }
    }

    /**
     * ถมรูปหลายเหลี่ยมด้วย scanline + กฎ nonzero winding
     *
     * ต้องเป็น nonzero ไม่ใช่ even-odd เพราะ potrace วาดคอนทัวร์ของ "รู" กลับทิศ
     * กับขอบนอก ถ้าใช้ even-odd รูในลายเส้นจะถูกถมทึบหมด
     *
     * pts เป็น {x,y} เรียงติดกัน · ends บอกว่าแต่ละคอนทัวร์จบที่จุดที่เท่าไร
     * (หน่วยเป็นจำนวนจุด ไม่ใช่จำนวน double) ทุกคอนทัวร์ถือว่าปิดเอง
     */
    public static void scanlineFill(Raster r, double[] pts, int[] ends, int argb) {
        int nPts = pts.length / 2;
        if (nPts < 3) return;
        if (ends == null || ends.length == 0) ends = new int[] { nPts };

        // ---- สร้างตารางขอบ ----
        // เก็บเฉพาะขอบที่ข้ามแถว ขอบแนวนอนไม่มีผลกับการนับ winding
        int maxEdges = nPts;
        double[] eYTop = new double[maxEdges];
        double[] eYBot = new double[maxEdges];
        double[] eX = new double[maxEdges];
        double[] eSlope = new double[maxEdges];
        int[] eDir = new int[maxEdges];
        int nEdges = 0;

        int start = 0;
        for (int end : ends) {
            int count = end - start;
            if (count >= 3) {
                for (int i = 0; i < count; i++) {
                    int a = start + i;
                    int b = start + (i + 1) % count;
                    double ax = pts[a * 2], ay = pts[a * 2 + 1];
                    double bx = pts[b * 2], by = pts[b * 2 + 1];
                    if (ay == by) continue;

                    double slope = (bx - ax) / (by - ay);
                    if (ay < by) {
                        eYTop[nEdges] = ay;
                        eYBot[nEdges] = by;
                        eX[nEdges] = ax;
                        eDir[nEdges] = 1;
                    } else {
                        eYTop[nEdges] = by;
                        eYBot[nEdges] = ay;
                        eX[nEdges] = bx;
                        eDir[nEdges] = -1;
                    }
                    eSlope[nEdges] = slope;
                    nEdges++;
                }
            }
            start = end;
        }
        if (nEdges == 0) return;

        double minY = Double.MAX_VALUE, maxY = -Double.MAX_VALUE;
        for (int i = 0; i < nEdges; i++) {
            if (eYTop[i] < minY) minY = eYTop[i];
            if (eYBot[i] > maxY) maxY = eYBot[i];
        }
        int y0 = Math.max(0, (int) Math.ceil(minY - 0.5));
        int y1 = Math.min(r.h - 1, (int) Math.ceil(maxY - 0.5) - 1);
        if (y0 > y1) return;

        // ---- ตารางขอบ: จัดขอบเข้าถังตามแถวที่เริ่มมีผล ----
        // ใช้ลิสต์โยงด้วย int array แทนการเรียงทั้งชุด จะได้ไม่ต้อง sort O(n log n)
        // และไม่ต้องบ็อกซ์ Integer ซึ่งเป็นตัวกินเวลาหลักตอนรูปมีจุดเป็นแสน
        int rows = y1 - y0 + 1;
        int[] bucket = new int[rows];
        Arrays.fill(bucket, -1);
        int[] nextEdge = new int[nEdges];

        for (int i = 0; i < nEdges; i++) {
            int ys = (int) Math.ceil(eYTop[i] - 0.5);
            if (ys < y0) ys = y0;
            if (ys > y1) continue;
            int b = ys - y0;
            nextEdge[i] = bucket[b];
            bucket[b] = i;
        }

        int[] active = new int[nEdges];
        int nActive = 0;
        long[] keys = new long[nEdges];

        for (int y = y0; y <= y1; y++) {
            double yc = y + 0.5;

            for (int e = bucket[y - y0]; e != -1; e = nextEdge[e]) {
                active[nActive++] = e;
            }
            // ตัดขอบที่พ้นแถวนี้ไปแล้วออก
            int keep = 0;
            for (int i = 0; i < nActive; i++) {
                if (eYBot[active[i]] > yc) active[keep++] = active[i];
            }
            nActive = keep;
            if (nActive == 0) continue;

            // อัดพิกัด x กับทิศทางลงคีย์เดียวกัน จะได้เรียงด้วย Arrays.sort ของ
            // ชนิดพื้นฐาน (dual-pivot quicksort) ไม่ต้องเรียงสองอาเรย์คู่กันเอง
            for (int i = 0; i < nActive; i++) {
                int e = active[i];
                double x = eX[e] + (yc - eYTop[e]) * eSlope[e];
                keys[i] = (encode(x) << 1) | (eDir[e] > 0 ? 1L : 0L);
            }
            Arrays.sort(keys, 0, nActive);

            int winding = 0;
            for (int i = 0; i < nActive - 1; i++) {
                winding += (keys[i] & 1L) != 0 ? 1 : -1;
                if (winding != 0) {
                    int xa = (int) Math.ceil(decode(keys[i] >>> 1) - 0.5);
                    int xb = (int) Math.ceil(decode(keys[i + 1] >>> 1) - 0.5) - 1;
                    if (xa <= xb) r.span(y, xa, xb, argb);
                }
            }
        }
    }

    /**
     * แปลงพิกัด x เป็นจำนวนเต็มที่เรียงลำดับตรงกับค่าเดิม เพื่ออัดรวมกับทิศทางขอบ
     *
     * ปัดเป็นหน่วย 1/256 พิกเซล ละเอียดเกินพอสำหรับหาจุดตัดของ scanline
     * และบวก BIAS ให้เป็นบวกเสมอ คีย์ที่ได้จึงเรียงด้วย Arrays.sort ของ long ตรงๆ
     */
    private static final int SUB = 256;
    private static final long BIAS = 1L << 40;

    private static long encode(double x) {
        return Math.round(x * SUB) + BIAS;
    }

    private static double decode(long k) {
        return (k - BIAS) / (double) SUB;
    }
}
