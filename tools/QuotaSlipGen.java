import java.awt.image.BufferedImage;
import java.io.File;
import javax.imageio.ImageIO;

/** Draws the Quota Slip item texture: a paper slip with a copper clip and written lines. Usage: java tools/QuotaSlipGen.java <outFile> */
public class QuotaSlipGen {
	private static final int PAPER = 0xFFE8E2D0, EDGE = 0xFFB8AF96, INK = 0xFF6B6250, COPPER = 0xFFC46A3C, COPPER_LIGHT = 0xFFE0955F;

	public static void main(String[] args) throws Exception {
		BufferedImage image = new BufferedImage(16, 16, BufferedImage.TYPE_INT_ARGB);
		fill(image, 3, 2, 10, 13, EDGE);
		fill(image, 4, 3, 8, 11, PAPER);
		for (int y = 6; y <= 12; y += 2) {
			fill(image, 5, y, y == 10 ? 4 : 6, 1, INK);
		}
		fill(image, 6, 1, 4, 3, COPPER);
		fill(image, 7, 1, 2, 1, COPPER_LIGHT);
		ImageIO.write(image, "png", new File(args[0]));
	}

	private static void fill(BufferedImage image, int x, int y, int width, int height, int argb) {
		for (int px = x; px < x + width; px++) {
			for (int py = y; py < y + height; py++) {
				image.setRGB(px, py, argb);
			}
		}
	}
}
