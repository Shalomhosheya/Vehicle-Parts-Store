package com.buildme.partsstore.ui.components;

import com.buildme.partsstore.model.Category;

import javax.imageio.ImageIO;
import javax.swing.*;
import java.awt.*;
import java.awt.geom.AffineTransform;
import java.awt.geom.Ellipse2D;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

/**
 * Manages part images entirely on the presentation side.
 *
 * Real photos are copied into a local "part-images" folder (created next
 * to wherever the app runs) and named after the part's id, e.g.
 * "P-ENG-001.jpg" - see saveImage()/removeImage().
 *
 * For parts that don't have a real photo attached, getIcon(id, category, size)
 * draws a distinct, recognisable icon for that part's category instead of a
 * generic blank placeholder (gear for engine parts, disc for brakes, a
 * lightning bolt for electrical, a car silhouette for body panels, a paint
 * drop for paint/accessories, a tyre for tyres). These are drawn entirely in
 * code with Graphics2D - nothing is downloaded, so this works fully offline
 * and raises no copyright concerns, unlike scraping real product photos from
 * the internet.
 *
 * Scaled icons are cached in memory so the table doesn't re-decode/redraw
 * on every repaint.
 */
public class ImageStore {

    private static final Path IMAGE_DIR = Paths.get("part-images");

    private final Map<String, ImageIcon> cache = new HashMap<>();

    public ImageStore() {
        try {
            Files.createDirectories(IMAGE_DIR);
        } catch (IOException ignored) {
            // if we can't create it, hasImage()/findImageFile() will just report "no image"
        }
    }

    /** Copies sourceFile into local storage under the given part id, replacing any previous image. */
    public void saveImage(String partId, File sourceFile) throws IOException {
        removeImage(partId);
        String ext = extensionOf(sourceFile.getName());
        Path target = IMAGE_DIR.resolve(partId + "." + ext);
        Files.copy(sourceFile.toPath(), target, StandardCopyOption.REPLACE_EXISTING);
        evictCache(partId);
    }

    /** Deletes any stored image for this part id, if one exists. */
    public void removeImage(String partId) {
        File existing = findImageFile(partId);
        if (existing != null) {
            existing.delete();
        }
        evictCache(partId);
    }

    public boolean hasImage(String partId) {
        return findImageFile(partId) != null;
    }

    /**
     * Returns a square icon of the given size for this part: the real
     * uploaded photo if one exists, otherwise an automatically generated
     * icon for its category.
     */
    public ImageIcon getIcon(String partId, Category category, int size) {
        String catKey = category == null ? "NONE" : category.toString();
        String key = partId + "@" + size + "@" + catKey;
        ImageIcon cached = cache.get(key);
        if (cached != null) {
            return cached;
        }
        ImageIcon icon = loadScaled(partId, category, size);
        cache.put(key, icon);
        return icon;
    }

    /** Overload for callers that don't have a Category handy - falls back to a generic icon. */
    public ImageIcon getIcon(String partId, int size) {
        return getIcon(partId, null, size);
    }

    private ImageIcon loadScaled(String partId, Category category, int size) {
        File file = findImageFile(partId);
        if (file != null) {
            try {
                BufferedImage img = ImageIO.read(file);
                if (img != null) {
                    Image scaled = img.getScaledInstance(size, size, Image.SCALE_SMOOTH);
                    return new ImageIcon(scaled);
                }
            } catch (IOException ignored) {
                // fall through to generated icon
            }
        }
        return categoryIcon(category, size);
    }

    // ---------------------------------------------------------------
    // Generated category icons - all hand-drawn with Graphics2D, no
    // network access, no external assets, no copyright concerns.
    // ---------------------------------------------------------------

    private ImageIcon categoryIcon(Category category, int size) {
        String name = category == null ? "" : category.toString().toUpperCase(Locale.ROOT);

        BufferedImage img = new BufferedImage(size, size, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g2 = img.createGraphics();
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

        Color background;
        switch (name) {
            case "ENGINE":      background = new Color(255, 233, 210); break;
            case "BRAKES":      background = new Color(255, 214, 214); break;
            case "ELECTRICAL":  background = new Color(255, 247, 194); break;
            case "BODY":        background = new Color(210, 231, 255); break;
            case "PAINT":       background = new Color(233, 214, 255); break;
            case "TYRES":       background = new Color(224, 224, 224); break;
            default:            background = new Color(230, 230, 230);
        }
        g2.setColor(background);
        g2.fillRoundRect(0, 0, size, size, size / 6, size / 6);

        switch (name) {
            case "ENGINE":     drawGear(g2, size, new Color(180, 105, 30)); break;
            case "BRAKES":     drawBrakeDisc(g2, size, new Color(150, 40, 40)); break;
            case "ELECTRICAL": drawLightningBolt(g2, size, new Color(190, 150, 0)); break;
            case "BODY":       drawCarSilhouette(g2, size, new Color(40, 90, 160)); break;
            case "PAINT":      drawPaintDrop(g2, size, new Color(120, 60, 170)); break;
            case "TYRES":      drawTyre(g2, size, new Color(40, 40, 40)); break;
            default:           drawWrench(g2, size, new Color(120, 120, 120));
        }

        g2.dispose();
        return new ImageIcon(img);
    }

    private void drawGear(Graphics2D g2, int size, Color color) {
        int cx = size / 2, cy = size / 2;
        int outerR = (int) (size * 0.32);
        int innerR = (int) (size * 0.14);
        g2.setColor(color);
        int teeth = 8;
        for (int i = 0; i < teeth; i++) {
            double angle = (2 * Math.PI / teeth) * i;
            AffineTransform old = g2.getTransform();
            g2.translate(cx, cy);
            g2.rotate(angle);
            g2.fillRoundRect(-size / 14, -outerR - size / 10, size / 7, size / 6, 3, 3);
            g2.setTransform(old);
        }
        g2.fill(new Ellipse2D.Double(cx - outerR, cy - outerR, outerR * 2, outerR * 2));
        g2.setColor(new Color(255, 255, 255, 230));
        g2.fill(new Ellipse2D.Double(cx - innerR, cy - innerR, innerR * 2, innerR * 2));
    }

    private void drawBrakeDisc(Graphics2D g2, int size, Color color) {
        int cx = size / 2, cy = size / 2;
        int r = (int) (size * 0.34);
        g2.setColor(color);
        g2.fill(new Ellipse2D.Double(cx - r, cy - r, r * 2, r * 2));
        g2.setColor(new Color(255, 255, 255, 220));
        int hub = r / 3;
        g2.fill(new Ellipse2D.Double(cx - hub, cy - hub, hub * 2, hub * 2));
        g2.setColor(color.darker());
        g2.setStroke(new BasicStroke(Math.max(1f, size / 32f)));
        int boltR = (int) (hub * 1.7);
        for (int i = 0; i < 5; i++) {
            double angle = (2 * Math.PI / 5) * i;
            int bx = (int) (cx + boltR * Math.cos(angle));
            int by = (int) (cy + boltR * Math.sin(angle));
            g2.fillOval(bx - 2, by - 2, 4, 4);
        }
    }

    private void drawLightningBolt(Graphics2D g2, int size, Color color) {
        Polygon bolt = new Polygon();
        bolt.addPoint((int) (size * 0.55), (int) (size * 0.15));
        bolt.addPoint((int) (size * 0.30), (int) (size * 0.55));
        bolt.addPoint((int) (size * 0.47), (int) (size * 0.55));
        bolt.addPoint((int) (size * 0.40), (int) (size * 0.85));
        bolt.addPoint((int) (size * 0.68), (int) (size * 0.42));
        bolt.addPoint((int) (size * 0.50), (int) (size * 0.42));
        g2.setColor(color);
        g2.fillPolygon(bolt);
    }

    private void drawCarSilhouette(Graphics2D g2, int size, Color color) {
        g2.setColor(color);
        int bodyW = (int) (size * 0.7), bodyH = (int) (size * 0.28);
        int bx = (size - bodyW) / 2, by = (int) (size * 0.42);
        g2.fillRoundRect(bx, by, bodyW, bodyH, bodyH / 2, bodyH / 2);
        // cabin
        int cabW = (int) (size * 0.38), cabH = (int) (size * 0.2);
        g2.fillRoundRect(bx + (bodyW - cabW) / 2, by - cabH + 6, cabW, cabH, 8, 8);
        // wheels
        g2.setColor(Color.DARK_GRAY);
        int wheelR = (int) (size * 0.09);
        g2.fillOval(bx + wheelR / 2, by + bodyH - wheelR, wheelR * 2, wheelR * 2);
        g2.fillOval(bx + bodyW - wheelR * 2 - wheelR / 2, by + bodyH - wheelR, wheelR * 2, wheelR * 2);
    }

    private void drawPaintDrop(Graphics2D g2, int size, Color color) {
        int cx = size / 2;
        Polygon drop = new Polygon();
        drop.addPoint(cx, (int) (size * 0.18));
        drop.addPoint((int) (size * 0.28), (int) (size * 0.58));
        drop.addPoint((int) (size * 0.72), (int) (size * 0.58));
        g2.setColor(color);
        g2.fillPolygon(drop);
        int r = (int) (size * 0.22);
        g2.fill(new Ellipse2D.Double(cx - r, size * 0.5, r * 2, r * 2));
    }

    private void drawTyre(Graphics2D g2, int size, Color color) {
        int cx = size / 2, cy = size / 2;
        int outerR = (int) (size * 0.34);
        g2.setColor(color);
        g2.fill(new Ellipse2D.Double(cx - outerR, cy - outerR, outerR * 2, outerR * 2));
        g2.setColor(new Color(200, 200, 200));
        int innerR = (int) (outerR * 0.45);
        g2.fill(new Ellipse2D.Double(cx - innerR, cy - innerR, innerR * 2, innerR * 2));
        g2.setColor(color);
        g2.setStroke(new BasicStroke(Math.max(1f, size / 24f)));
        for (int i = 0; i < 5; i++) {
            double angle = (2 * Math.PI / 5) * i;
            int x1 = (int) (cx + innerR * 0.7 * Math.cos(angle));
            int y1 = (int) (cy + innerR * 0.7 * Math.sin(angle));
            int x2 = (int) (cx + outerR * 0.9 * Math.cos(angle));
            int y2 = (int) (cy + outerR * 0.9 * Math.sin(angle));
            g2.drawLine(x1, y1, x2, y2);
        }
    }

    private void drawWrench(Graphics2D g2, int size, Color color) {
        g2.setColor(color);
        g2.setStroke(new BasicStroke(Math.max(2f, size / 10f), BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
        g2.drawLine((int) (size * 0.25), (int) (size * 0.75), (int) (size * 0.65), (int) (size * 0.35));
        g2.fillOval((int) (size * 0.55), (int) (size * 0.15), (int) (size * 0.3), (int) (size * 0.3));
        g2.setColor(new Color(255, 255, 255, 230));
        g2.fillOval((int) (size * 0.63), (int) (size * 0.23), (int) (size * 0.14), (int) (size * 0.14));
    }

    private File findImageFile(String partId) {
        File dir = IMAGE_DIR.toFile();
        File[] matches = dir.listFiles((d, name) -> name.startsWith(partId + "."));
        return (matches != null && matches.length > 0) ? matches[0] : null;
    }

    private void evictCache(String partId) {
        cache.keySet().removeIf(k -> k.startsWith(partId + "@"));
    }

    private String extensionOf(String filename) {
        int dot = filename.lastIndexOf('.');
        return dot >= 0 ? filename.substring(dot + 1).toLowerCase(Locale.ROOT) : "png";
    }
}