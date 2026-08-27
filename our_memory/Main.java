import javax.swing.JFrame;
import javax.swing.SwingUtilities;

public final class Main {

    private static final int WIDTH = 600;
    private static final int HEIGHT = 600;

    private static final int START_SCENE = 0;

    private Main() {
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            JFrame frame = new JFrame("Multi-Frame SVG with Drawable Objects");
            frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
            frame.add(new MultiSvgAnimationApp(START_SCENE));
            frame.setSize(WIDTH, HEIGHT);
            frame.setLocationRelativeTo(null);
            frame.setVisible(true);
        });
    }
}
