import com.kitfox.svg.SVGDiagram;
import com.kitfox.svg.SVGUniverse;

import javax.imageio.ImageIO;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.io.File;

/** Bake docs/icons/modules SVG sources into the three runtime texture sizes. */
public class Bake {
    private static final int[] SIZES = {24, 48, 96};

    public static void main(String[] args) throws Exception {
        File root = new File("../..");
        File svgDir = new File(root, "docs/icons/modules");
        File outRoot = new File(root, "src/main/resources/assets/fpsmaster/textures/gui/icons");
        SVGUniverse universe = new SVGUniverse();
        File[] files = svgDir.listFiles((dir, name) -> name.endsWith(".svg"));
        if (files == null) throw new IllegalStateException("Missing " + svgDir);
        for (File source : files) {
            SVGDiagram diagram = universe.getDiagram(universe.loadSVG(source.toURI().toURL()));
            diagram.setIgnoringClipHeuristic(true);
            String name = source.getName().replace(".svg", ".png");
            for (int size : SIZES) {
                File outDir = new File(outRoot, size + "/modules");
                outDir.mkdirs();
                BufferedImage image = new BufferedImage(size, size, BufferedImage.TYPE_INT_ARGB);
                Graphics2D graphics = image.createGraphics();
                graphics.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                graphics.setRenderingHint(RenderingHints.KEY_STROKE_CONTROL, RenderingHints.VALUE_STROKE_PURE);
                graphics.scale(size / diagram.getWidth(), size / diagram.getHeight());
                diagram.render(graphics);
                graphics.dispose();
                ImageIO.write(image, "png", new File(outDir, name));
            }
        }
        System.out.println("Baked " + files.length + " module icons at 24/48/96");
    }
}
