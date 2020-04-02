package org.parkers.gatekeep.gamedata;

import discord4j.core.object.entity.Attachment;

import javax.imageio.ImageIO;
import java.awt.Graphics2D;
import java.awt.Image;
import java.awt.image.BufferedImage;
import java.awt.image.ColorModel;
import java.awt.image.WritableRaster;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.net.URLConnection;
import java.util.Set;

class MyNewImage {
    // testing utility function
    static MyNewImage readUrl(String url, String fileName) throws IOException {
        URLConnection connection = new URL(url).openConnection();
        connection.setRequestProperty("User-Agent", "Gatekeeper");
        return new MyNewImage(fileName, connection.getInputStream());
    }



    private BufferedImage image;
    private String name, extension;

    String getName() {
        return name;
    }

    MyNewImage readImage(Set<Attachment> attachmentSet) {
        for (Attachment attachment : attachmentSet) {
            try {
                String url = attachment.getUrl();
                URLConnection connection = new URL(url).openConnection();
                connection.setRequestProperty("User-Agent", "Gatekeeper");
                return new MyNewImage(attachment.getFilename(), connection.getInputStream());
            } catch (Exception ignored) { }
        }
        return null;
    }


    MyNewImage(String fileName, InputStream imageInput) throws IOException {
        name = fileName;
        extension = fileName.substring(fileName.lastIndexOf('.') + 1);

        image = ImageIO.read(imageInput);
    }

    InputStream getInputStream() throws IOException {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        ImageIO.write(image, extension, out);
        return new ByteArrayInputStream(out.toByteArray());
    }

    MyNewImage copy() {
        MyNewImage newImage = new MyNewImage(name, extension);

        ColorModel cm = image.getColorModel();
        boolean isAlpha = cm.isAlphaPremultiplied();
        WritableRaster raster = image.copyData(null);

        newImage.image = new BufferedImage(cm, raster, isAlpha, null);

        return newImage;
    }
    MyNewImage resizeCopy(int maxLength) {
        int w, h, tw, th;
        w = image.getWidth();
        h = image.getHeight();
        tw = maxLength;
        th = maxLength;

        if (w > h)
            th = getResize(tw, w, h);
        else if (w < h)
            tw = getResize(th, h, w);

        MyNewImage img = new MyNewImage(name, extension);

        Image tmp = image.getScaledInstance(tw, th, Image.SCALE_SMOOTH);
        BufferedImage simg = new BufferedImage(tw, th, BufferedImage.TYPE_INT_ARGB);

        Graphics2D g2d = simg.createGraphics();
        g2d.drawImage(tmp, 0, 0, null);
        g2d.dispose();

        img.image = simg;
        return img;
    }
    private int getResize(int max, int ref, int scale) {
        double scalar = ref;
        scalar /= max;

        return (int) (scale / scalar);
    }

    void drawImage(MyNewImage src, int x, int y) {
        Graphics2D g2d = image.createGraphics();
        g2d.drawImage(src.image, null, x, y);
        g2d.dispose();
    }





    // setup method for copy functions
    private MyNewImage(String name, String extension) {
        this.name = name;
        this.extension = extension;
    }
}
