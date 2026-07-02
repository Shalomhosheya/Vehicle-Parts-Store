package com.buildme.partsstore.ui.components;


import javax.imageio.ImageIO;
import javax.swing.*;
import java.awt.*;
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
 * Images are copied into a local "part-images" folder (created next to
 * wherever the app runs) and named after the part's id, e.g.
 * "P-ENG-001.jpg". This keeps Part/Inventory completely untouched -
 * they still know nothing about images, exactly as they knew nothing
 * about Swing before.
 *
 * Scaled icons are cached in memory so the table doesn't re-decode the
 * same image file on every repaint.
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

    /** Returns a square icon of the given size - the real image if one exists, otherwise a placeholder. */
    public ImageIcon getIcon(String partId, int size) {
        String key = partId + "@" + size;
        ImageIcon cached = cache.get(key);
        if (cached != null) {
            return cached;
        }
        ImageIcon icon = loadScaled(partId, size);
        cache.put(key, icon);
        return icon;
    }

    private ImageIcon loadScaled(String partId, int size) {
        File file = findImageFile(partId);
        if (file != null) {
            try {
                BufferedImage img = ImageIO.read(file);
                if (img != null) {
                    Image scaled = img.getScaledInstance(size, size, Image.SCALE_SMOOTH);
                    return new ImageIcon(scaled);
                }
            } catch (IOException ignored) {
                // fall through to placeholder
            }
        }
        return placeholder(size);
    }

    private ImageIcon placeholder(int size) {
        BufferedImage img = new BufferedImage(size, size, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g2 = img.createGraphics();
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

        g2.setColor(new Color(235, 235, 235));
        g2.fillRoundRect(0, 0, size, size, size / 6, size / 6);
        g2.setColor(new Color(190, 190, 190));
        g2.setStroke(new BasicStroke(Math.max(1f, size / 24f)));
        g2.drawRoundRect(1, 1, size - 2, size - 2, size / 6, size / 6);

        // simple "picture" glyph: a small sun + mountain, like a generic no-image icon
        int pad = size / 6;
        g2.setColor(new Color(200, 200, 200));
        g2.fillOval(size - pad * 2, pad, pad, pad); // sun
        Polygon mountain = new Polygon();
        mountain.addPoint(pad, size - pad);
        mountain.addPoint(size / 2, size / 3);
        mountain.addPoint(size - pad, size - pad);
        g2.fillPolygon(mountain);

        g2.dispose();
        return new ImageIcon(img);
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