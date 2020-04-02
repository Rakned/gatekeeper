package org.parkers.gatekeep.gamedata;

import discord4j.core.event.domain.message.MessageCreateEvent;
import discord4j.core.object.entity.Attachment;
import discord4j.core.spec.MessageCreateSpec;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.geom.Line2D;
import java.awt.image.BufferedImage;
import java.awt.image.ColorModel;
import java.awt.image.WritableRaster;
import java.io.*;
import java.net.URL;
import java.net.URLConnection;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;



public class GameMap {
    private boolean isReady = false;
    private MyImage baseImage, unitImage;
    private BufferedImage[][] squares = null;

    private double corner_x = 128, corner_y = 128, gridSize = 256;
    private int gridWidth = 6, gridHeight = 3;

    private Map<String, Unit> unitMap = new HashMap<>();

    public static void imageResponseTest(MessageCreateSpec spec) {
        try {
            MyNewImage one, two, three;
            one = MyNewImage.readUrl("https://cdn.discordapp.com/attachments/686269264067690595/693937117587308574/cathS.png", "cathS.png");
            two = MyNewImage.readUrl("https://cdn.discordapp.com/attachments/686269264067690595/693937350857982073/toot.png", "toot.png");
            three = two.resizeCopy(200);
            one.drawImage(two, 32, 32);
            one.drawImage(three, 400, 800);

            spec.addFile(one.name, one.getInputStream());
        } catch (IOException e) {
            spec.setContent(e.getMessage());
        }
    }


    private class MyImage {
        private BufferedImage image;
        private String name, ext;

        MyImage(String name, InputStream stream) throws IOException {
            image = ImageIO.read(stream);

            this.name = name.substring(0, name.lastIndexOf('.'));
            this.ext = name.substring(name.lastIndexOf('.') + 1);
        }

        MyImage(MyImage oldFile) {
            this.name = oldFile.name;
            this.ext = oldFile.ext;

            this.image = copyImage(oldFile.image);
        }

        MyImage drawGrid(double ulx, double uly, double gridsize, int width, int height) {
            Graphics2D graphics = image.createGraphics();
            graphics.setColor(Color.BLACK);

            Line2D.Double line = new Line2D.Double();
            // draw rows
            line.setLine(ulx, uly, width * gridsize + ulx, uly);
            graphics.draw(line);
            for (int i = 0; i < height; i++) {
                line.y1 += gridsize;
                line.y2 = line.y1;

                graphics.draw(line);

                // todo: implement drawing row IDs
            }

            // draw columns
            line.setLine(ulx, uly, ulx, height * gridsize + uly);
            graphics.draw(line);
            for (int i = 0; i < width; i++) {
                line.x1 += gridsize;
                line.x2 = line.x1;

                graphics.draw(line);

                // todo: implement drawing column IDs
            }

            graphics.dispose();

            return this;
        }

        String getName() {
            return name;
        }
        InputStream getStream() throws IOException {
            ByteArrayOutputStream out = new ByteArrayOutputStream();

            ImageIO.write(image, ext, out);

            return new ByteArrayInputStream(out.toByteArray());
        }

        void attach(String title, MessageCreateSpec spec) {
            try {
                spec.addFile(title + "." + ext, getStream() );
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        private BufferedImage copyImage(BufferedImage oldImg) {
            ColorModel cm = oldImg.getColorModel();
            boolean isAlpha = cm.isAlphaPremultiplied();
            WritableRaster raster = oldImg.copyData(null);
            return new BufferedImage(cm, raster, isAlpha, null);
        }

        // method for converting a MyImage from the attachment recovery function to an equivalent Unit entity
        Unit genUnit(int tsize) {
            return new Unit(image, tsize);
        }

        void emptySpace(Unit unit, MyImage base) {
            emptySpace(unit.x, unit.y, base);
        }
        void emptySpace(int x, int y, MyImage base) {
            if (x < 0 || x >= gridWidth || y < 0 || y >= gridHeight)
                return;

            Rectangle sq = getSquare(x, y);
            BufferedImage sub = base.image.getSubimage(sq.x, sq.y, sq.width, sq.height);

            Graphics2D g2d = image.createGraphics();
            g2d.drawImage(sub, null, sq.x, sq.y);
            g2d.dispose();
        }

        void placeUnit(int x, int y, Unit unit) {
            unit.setPos(x, y);

            if (x < 0 || x >= gridWidth || y < 0 || y >= gridHeight)
                return;

            Rectangle square = getSquare(x, y);

            Graphics2D g2d = image.createGraphics();
            g2d.drawImage(unit.portrait, null, square.x, square.y);
            g2d.dispose();
        }

        Rectangle getSquare(int x, int y) {
            return new Rectangle((int)(corner_x + x * gridSize), (int)(corner_y + y * gridSize), (int) gridSize, (int) gridSize);
        }
    }

    // general-use command processing interface
    public void command(MessageCreateEvent event, MessageCreateSpec spec) {
        String command = event.getMessage().getContent().orElse(null);
        if (command == null) {
            spec.setContent("Error reading input!  Please try again.");
            return;
        }

        String[] args = command.substring(2).toLowerCase().split("\\s+");

        switch (args[0]) {
            case "map":
                mapSetup(event, spec, args);
                break;

            case "game":
                gameUpdate(event, spec, args);
                break;

            case "move":
                // todo: casual movement!
                break;
        }
    }

    // setup command processing - complete for now
    private void mapSetup(MessageCreateEvent event, MessageCreateSpec out, String[] args) {
        StringBuilder message = new StringBuilder();

        if (isReady) {
            message.append("This map is already fully initialized!  No further changes may be made at this time.");
        } else switch (args[1]) {
            case "help":
                message.append("```")
                        .append("g!map help")
                        .append("\n\tPrints out this helpful message!")
                        .append("\n\n")
                        .append("g!map setimg")
                        .append("\n\tSets the background the map uses to the first attached file.")
                        .append("\n\n")
                        .append("g!map filecheck")
                        .append("\n\tShows what image(s) are currently in use/storage with this map.")
                        .append("```Please note that this help message is currently out-of-date.");
                break;

            case "setimg":
                this.saveImage(args, event, message);
                break;

            case "setwidth":
                if (args.length == 3) {
                    gridWidth = Integer.parseInt(args[2]);
                    message.append("Grid width set to ")
                            .append(gridWidth);
                }
                break;

            case "setheight":
                if (args.length == 3) {
                    gridHeight = Integer.parseInt(args[2]);
                    message.append("Grid height set to ")
                            .append(gridHeight);
                }
                break;

            case "setcorner":
                if (args.length == 4) {
                    corner_x = Double.parseDouble(args[2]);
                    corner_y = Double.parseDouble(args[3]);
                    message.append("Grid corner set to (")
                            .append(corner_x)
                            .append(", ")
                            .append(corner_y)
                            .append(")");
                } else {
                    message.append("Use: `g!map setcorner <x> <y>`");
                }
                break;

            case "setsize":
                // todo unimplemented!
                message.append("This method has not been implemented yet...");
                break;

            case "checkdata":
                if (baseImage != null)
                    baseImage.attach("background", out);
                else
                    message.append("No image file detected!\n");
                message.append("```")
                        .append("corner x co-ord:\n\t")
                        .append(corner_x)
                        .append("\ncorner y:\n\t")
                        .append(corner_y)
                        .append("\ngrid width:\n\t")
                        .append(gridWidth)
                        .append("\ngrid height:\n\t")
                        .append(gridHeight)
                        .append("\nsquare size:\n\t")
                        .append(gridSize)
                        .append("```");
                break;

            case "preview":
                genFinal().attach("preview", out);
                message.append("This is what the file will look like when it's finalized - do so now if everything looks ok!");
                break;

            case "finalize":
                isReady = true;
                baseImage = genFinal();
                unitImage = new MyImage(baseImage);
                message.append("The map has been finalized.  You won't be able to change this image later, so I hope you're satisfied!");
                unitImage.attach("map", out);
                break;

            default:
                message.append("I'm sorry, but I can't recognize the command `g!map ")
                        .append(args[1])
                        .append("`; please check if you've made a typo!");
        }

        out.setContent(message.toString());
    }

    // manual gamestate manipulation - todo incomplete
    private void gameUpdate(MessageCreateEvent event, MessageCreateSpec out, String[] args) {
        if (args.length < 2) {
            out.setContent("You need to specify an option for the game update!");
        } else switch (args[1]) {
            case "addunit":
                out.setContent(args.length == 3 ? addChar(args[2], readAttachment(event)) :
                        "You didn't have the right number of arguments!  I can only create a character whose name does not include any whitespace.");
                break;

            case "addmany":
                // todo group add procedure (for bandits npcs etc)
                out.setContent("This method has not yet been implemented.");
                break;

            case "moveunit":
            case "move":
                if (args.length != 5) {
                    out.setContent("BAD ARGUMENT COUNT");
                    return;
                }
                if (!unitMap.containsKey(args[2])) {
                    out.setContent("COULD NOT FIND UNIT NAMED \"" + args[2] + "\"");
                    return;
                }

                int x = Integer.parseInt(args[3]), y = Integer.parseInt(args[4]);
                Unit unit = unitMap.get(args[2]);

                if (!isSpaceEmpty(x, y)) {
                    out.setContent("Sorry, that location is already occupied.");
                    return;
                }

                unitImage.emptySpace(unit, baseImage);
                unitImage.placeUnit(x, y, unit);

                out.setContent("Attempted to move the unit!");
                unitImage.attach("map", out);

                break;

            case "checkmap":
                unitImage.attach("map", out);
                break;

            default:
                out.setContent("I'm afraid `g!game " + args[2] + "` is not a recognized command.  Please try again.");
        }
    }




    private void saveImage(String[] args, MessageCreateEvent event, StringBuilder message) {
        MyImage in;

        if (args.length == 2) {
            in = readAttachment(event);

            if (in == null) {
                message.append("Usage: `g!map setimg` (with attached file)");
                return;
            }

        } else {
            // todo: try to copy an image directly from the provided url!
            message.append("Usage: `g!map setimg` (with attached file)");
            return;
        }

        baseImage = in;
        message.append("Background image set to ")
                .append(in.getName());
    }
    private MyImage readAttachment(MessageCreateEvent event) {
        Set<Attachment> set = event.getMessage().getAttachments();

        for (Attachment attach : set) {
            try {
                String url = attach.getUrl();
                URLConnection connection = new URL(url).openConnection();
                connection.setRequestProperty("User-Agent", "Gatekeeper");
                return new MyImage(attach.getFilename(), connection.getInputStream());
            } catch (Exception ignored) { }
        }

        return null;
    }
    private MyImage genFinal() {
        return new MyImage(baseImage).drawGrid(corner_x, corner_y, gridSize, gridWidth, gridHeight);
    }

    private String addChar(String name, MyImage img) {
        if (unitMap.containsKey(name))
            return "A character named " + name + "already exits on this map.  Please choose a new name for your unit.";

        if (img == null)
            return "I'm afraid I can't find any attached files to use for your character.  Please try again with a file attached.";

        unitMap.put(name, img.genUnit((int) gridSize - 3));

        return "Character " + name + " was successfully created.";
    }
    // attempts to relocate a unit to the specified (x, y) coordinate location
    private void tryMove(Unit unit, int x, int y, MessageCreateSpec spec) {
        if (isSpaceEmpty(x, y)) {
            spec.setContent("Trying to move character...");
            unitImage.emptySpace(unit, baseImage);
            unitImage.placeUnit(x, y, unit);
            unit.setPos(x, y);
        } else
            spec.setContent("That space is already occupied...");
    }

    private boolean isSpaceEmpty(int x, int y) {
        if (x == -1 && y == -1) {
            return true;
        }
        for (Unit u : unitMap.values()) {
            if (u.isAt(x, y))
                    return false;
        }
        return true;
    }
}
