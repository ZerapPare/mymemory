import java.awt.Graphics2D;

/**
 * ของที่วาดทับลงบนภาพ SVG ที่ลงสีแล้ว
 *
 * time เป็นวินาที (เวลาจริง) ตัวที่ไม่ขยับก็ไม่ต้องสนใจค่านี้
 */
public interface Drawable {
    void draw(Graphics2D g, double time);
}
