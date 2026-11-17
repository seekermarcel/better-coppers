import java.awt.image.BufferedImage;
import java.io.File;
import javax.imageio.ImageIO;

/**
 * Scraper front textures: the vanilla dispenser fronts with a copper axe across them.
 * args: vanillaTexturesDir (assets/minecraft/textures) outDir
 */
public class ScraperGen {
	public static void main(String[] args) throws Exception {
		File textures = new File(args[0]);
		File out = new File(args[1]);
		out.mkdirs();
		BufferedImage axe = ImageIO.read(new File(textures, "item/copper_axe.png"));
		for (String name : new String[] {"dispenser_front", "dispenser_front_vertical"}) {
			BufferedImage front = ImageIO.read(new File(textures, "block/" + name + ".png"));
			BufferedImage image = new BufferedImage(16, 16, BufferedImage.TYPE_INT_ARGB);
			for (int y = 0; y < 16; y++) {
				for (int x = 0; x < 16; x++) {
					int a = axe.getRGB(x, y);
					image.setRGB(x, y, (a >>> 24) > 0 ? a | 0xFF000000 : front.getRGB(x, y));
				}
			}
			ImageIO.write(image, "png", new File(out, name.replace("dispenser", "scraper") + ".png"));
		}
	}
}
