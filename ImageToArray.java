import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import javax.imageio.ImageIO;
import java.io.File;
import java.io.PrintWriter;

public class ImageToArray {
    public static void main(String[] args) throws Exception {
        BufferedImage img = ImageIO.read(new File("ImageAnimationDemo (2).png"));

        // Adjust these values for clarity (150x200 is great)
        int targetW = 150;
        int targetH = 200;

        // Shrink with crisp pixels
        BufferedImage small = new BufferedImage(targetW, targetH, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g2d = small.createGraphics();
        g2d.drawImage(img, 0, 0, targetW, targetH, null);
        g2d.dispose();

        int[] pixels = new int[targetW * targetH];
        small.getRGB(0, 0, targetW, targetH, pixels, 0, targetW);

        // Build hex string
        StringBuilder hexBuilder = new StringBuilder(pixels.length * 8);
        for (int p : pixels) {
            hexBuilder.append(String.format("%08X", p));
        }
        String hex = hexBuilder.toString();

        // Split into chunks of 4000 characters (well under 65535 bytes)
        int CHUNK_SIZE = 4000;
        int numChunks = (int) Math.ceil((double) hex.length() / CHUNK_SIZE);

        try (PrintWriter out = new PrintWriter("pixel_data.txt")) {
            out.println("    public static final int STUDENT_W = " + targetW + ";");
            out.println("    public static final int STUDENT_H = " + targetH + ";");
            out.println();

            // Generate small String constants (each under 65KB)
            for (int c = 0; c < numChunks; c++) {
                int start = c * CHUNK_SIZE;
                int end = Math.min(start + CHUNK_SIZE, hex.length());
                out.print("    private static final String HEX_" + c + " = \"");
                out.print(hex.substring(start, end));
                out.println("\";");
            }
            out.println();

            // Group them into an array
            out.print("    private static final String[] HEX_CHUNKS = {");
            for (int c = 0; c < numChunks; c++) {
                if (c % 10 == 0) out.print("\n        ");
                out.print("HEX_" + c + ", ");
            }
            out.println("\n    };");
            out.println();

            // The static block - just a tiny loop (under 50 bytes of bytecode!)
            out.println("    public static final int[] PIXEL_DATA;");
            out.println("    static {");
            out.println("        StringBuilder sb = new StringBuilder();");
            out.println("        for (String chunk : HEX_CHUNKS) {");
            out.println("            sb.append(chunk);");
            out.println("        }");
            out.println("        String allHex = sb.toString();");
            out.println("        int totalPixels = allHex.length() / 8;");
            out.println("        PIXEL_DATA = new int[totalPixels];");
            out.println("        for (int i = 0; i < totalPixels; i++) {");
            out.println("            int start = i * 8;");
            out.println("            PIXEL_DATA[i] = (int) Long.parseLong(allHex.substring(start, start + 8), 16);");
            out.println("        }");
            out.println("    }");
        }

        System.out.println("Done! Open 'pixel_data.txt' and copy everything inside.");
    }
}
