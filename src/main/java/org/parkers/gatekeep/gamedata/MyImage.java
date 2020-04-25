package org.parkers.gatekeep.gamedata;

import discord4j.core.event.domain.message.MessageCreateEvent;
import discord4j.core.object.entity.Attachment;
import discord4j.core.spec.MessageCreateSpec;

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

class MyImage {
    // testing utility function
    static MyImage readUrl(String url, String fileName) throws IOException {
        URLConnection connection = new URL(url).openConnection();
        connection.setRequestProperty("User-Agent", "Gatekeeper");
        return new MyImage(fileName, connection.getInputStream());
    }



    private BufferedImage image;
    private String name, extension;

    static MyImage readImage(MessageCreateEvent event) {
        Set<Attachment> attachmentSet = event.getMessage().getAttachments();

        for (Attachment attachment : attachmentSet) {
            try {
                String url = attachment.getUrl();
                URLConnection connection = new URL(url).openConnection();
                connection.setRequestProperty("User-Agent", "Gatekeeper");
                return new MyImage(attachment.getFilename(), connection.getInputStream());
            } catch (Exception ignored) { }
        }

        return null;
    }
    private MyImage(String fileName, InputStream imageInput) throws IOException {
        name = fileName;
        extension = fileName.substring(fileName.lastIndexOf('.') + 1);

        image = ImageIO.read(imageInput);
    }

    String getName() {
        return name;
    }
    private InputStream getInputStream() throws IOException {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        ImageIO.write(image, extension, out);
        return new ByteArrayInputStream(out.toByteArray());
    }
    int getWidth() {
        return image.getWidth();
    }
    public int getHeight() {
        return image.getHeight();
    }


    MyImage copy() {
        MyImage newImage = new MyImage(name, extension);

        ColorModel cm = image.getColorModel();
        boolean isAlpha = cm.isAlphaPremultiplied();
        WritableRaster raster = image.copyData(null);

        newImage.image = new BufferedImage(cm, raster, isAlpha, null);

        return newImage;
    }

    MyImage copyResize(int maxLength) {
        int w, h, tw, th;
        w = image.getWidth();
        h = image.getHeight();
        tw = maxLength;
        th = maxLength;

        if (w > h)
            th = resizeFactor(tw, w, h);
        else if (w < h)
            tw = resizeFactor(th, h, w);

        MyImage img = new MyImage(name, extension);

        Image tmp = image.getScaledInstance(tw, th, Image.SCALE_SMOOTH);
        BufferedImage simg = new BufferedImage(tw, th, BufferedImage.TYPE_INT_ARGB);

        Graphics2D g2d = simg.createGraphics();
        g2d.drawImage(tmp, 0, 0, null);
        g2d.dispose();

        img.image = simg;
        return img;
    }
    private static int resizeFactor(int max, int ref, int scale) {
        double scalar = ref;
        scalar /= max;

        return (int) (scale / scalar);
    }

    MyImage subImage(int x, int y, int width, int height) {
        MyImage newImage = new MyImage(name, extension);
        newImage.image = image.getSubimage(x, y, width, height);
        return newImage;
    }

    void attach(MessageCreateSpec spec) throws IOException {
        spec.addFile(name, this.getInputStream());
    }



    void drawImage(MyImage src, int x, int y) {
        Graphics2D g2d = image.createGraphics();
        g2d.drawImage(src.image, null, x, y);
        g2d.dispose();
    }


    // setup creation method for copy functions
    private MyImage(String name, String extension) {
        this.name = name;
        this.extension = extension;
    }
}
