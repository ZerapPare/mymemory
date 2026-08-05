import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import javax.swing.JPanel;

/**
 * The projector: the canvas, the clock and the preview keys.
 *
 * It knows nothing about what is being drawn - it advances time and asks
 * Shots to render it.
 *
 * Preview keys: 1-6 jump to a shot, space pauses, left/right step 0.1 s,
 * r rewinds, h hides the time readout. Press h before recording.
 */
final class Film extends JPanel implements ActionListener, KeyListener {

    static final double FRAME = 1.0 / 60.0;

    final Shots shots = new Shots();

    double time = 0.0;
    boolean paused = false;
    boolean showHud = true; // preview aid, press H to hide before recording
    javax.swing.Timer timer;

    Film() {
        setPreferredSize(new Dimension(Art.W, Art.H));
        setBackground(Color.BLACK);
        setFocusable(true);
        addKeyListener(this);
        timer = new javax.swing.Timer(16, this);
        timer.start();
    }

    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        Graphics2D g2 = (Graphics2D) g.create();
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g2.setRenderingHint(RenderingHints.KEY_STROKE_CONTROL, RenderingHints.VALUE_STROKE_PURE);
        g2.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
        g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);

        shots.render(g2, time);

        if (showHud) {
            drawHud(g2);
        }
        g2.dispose();
    }

    public void actionPerformed(ActionEvent e) {
        if (!paused) {
            time += FRAME;
            if (time >= Shots.DURATION) {
                time -= Shots.DURATION;
            }
        }
        repaint();
    }

    public void keyPressed(KeyEvent e) {
        int c = e.getKeyCode();
        if (c >= KeyEvent.VK_1 && c <= KeyEvent.VK_6) {
            time = Shots.SHOT_START[c - KeyEvent.VK_1];
        } else if (c == KeyEvent.VK_SPACE) {
            paused = !paused;
        } else if (c == KeyEvent.VK_R) {
            time = 0.0;
        } else if (c == KeyEvent.VK_H) {
            showHud = !showHud;
        } else if (c == KeyEvent.VK_LEFT) {
            time = Math.max(0.0, time - 0.1);
        } else if (c == KeyEvent.VK_RIGHT) {
            time = Math.min(Shots.DURATION - FRAME, time + 0.1);
        }
        repaint();
    }

    public void keyReleased(KeyEvent e) { }

    public void keyTyped(KeyEvent e) { }

    void drawHud(Graphics2D g) {
        g.setFont(new Font("Monospaced", Font.PLAIN, 11));
        g.setColor(new Color(0, 0, 0, 90));
        g.drawString(String.format("t=%5.2f  [1-6] shot  [space] pause  [<-/->] step  [h] hide",
                time), 8, Art.H - 8);
    }
}
