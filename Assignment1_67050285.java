import javax.swing.JFrame;
import javax.swing.SwingUtilities;

/**
 * MY MEMORIES - Java 2D animation, 600x600, 15 seconds.
 *
 * Entry point. This class only opens the window; every other job lives in
 * its own file:
 *
 *   Film      the canvas, the clock and the preview keys
 *   Shots     the film script - six shots on one timeline
 *   Figure    every character, one drawStudent call per person
 *   Scene     the school common room of shots 1 and 2
 *   Hall      result page, lecture hall, stage, rain cloud
 *   Art       palette, colour maths, canvas size
 *   Midpoint  hand written circle / ellipse rasterisers
 *
 * No circle or ellipse anywhere comes from the Java API: drawOval, fillOval
 * and drawArc appear nowhere in the project.
 *
 * Build and run:
 *
 *   javac *.java
 *   java Assignment1_67050285
 */
public class Assignment1_67050285 {

    public static void main(String[] args) {
        SwingUtilities.invokeLater(new Runnable() {
            public void run() {
                open();
            }
        });
    }

    /** Builds the frame on the event dispatch thread. */
    static void open() {
        Film film = new Film();
        JFrame frame = new JFrame("MY MEMORIES");
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setResizable(false);
        frame.add(film);
        // pack, not setSize: this is what makes the canvas exactly 600x600
        // rather than 600x600 minus the title bar and borders
        frame.pack();
        frame.setLocationRelativeTo(null);
        frame.setVisible(true);
        film.requestFocusInWindow();
    }
}
