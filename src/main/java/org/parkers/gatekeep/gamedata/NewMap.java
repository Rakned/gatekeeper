package org.parkers.gatekeep.gamedata;

import discord4j.core.object.entity.Message;
import discord4j.core.object.entity.MessageChannel;
import discord4j.core.spec.MessageCreateSpec;
import reactor.core.publisher.Mono;

import java.io.IOException;

public class NewMap {
    private MyImage map, unit;
    private MyImage[][] blanks;
    private boolean mapCompleted = false;

    public int ulx, uly, width, height, size;

    public void setMapImage(MyImage image) {
        if (!mapCompleted) {
            map = image;
        }
    }
    public void setUnitImage(MyImage image) {
        if (!mapCompleted) {
            unit = image;
        }
    }


    public void complete() {
        // map completion check
        if (mapCompleted) { return; }
        else { mapCompleted = true; }

        // create array of blank images to reference
        blanks = new MyImage[width][height];
        for (int i = 0; i < width; i++) {
            for (int j = 0; j < height; j++) {
                blanks[i][j] = map.subImage(ulx + i*width, uly + j*height, size, size);
            }
        }

        if (unit != null) {
            unit = unit.copyResize(size);
        }
    }

    public void clearSquare(int x, int y) {
        if (squareInBounds(x, y)) {
            map.drawImage(blanks[x][y], ulx + x*width, uly + y*width);
        }
    }

    public void addSquare(int x, int y) {

    }

    public void printMap(MessageCreateSpec spec) {
        try {
            map.attach(spec);
        } catch (IOException e) {
            spec.setContent("```IO ERROR ENCOUNTERED WHEN PRINTING MAP TO INPUTSTREAM```");
        }
    }
    public Mono<Message> printMap(MessageChannel channel) {
        return channel.createMessage(this::printMap);
    }

    public void printUnit(MessageCreateSpec spec) {
        try {
            unit.attach(spec);
        } catch (IOException e) {
            spec.setContent("```IO ERROR ENCOUNTERED WHEN PRINTING UNIT TO INPUTSTREAM```");
        }
    }

    public void printSquareBcg(int x, int y, MessageCreateSpec spec) {
        try {
            blanks[x][y].attach(spec);
            spec.setContent("(" + x + ", " + y + ")");
        } catch (Exception e) {
            e.printStackTrace();
            spec.setContent("Error encountered! :(");
        }
    }











    private boolean squareInBounds(int x, int y) {
        return x >= 0 && y >= 0 && x < width && y < height;
    }


    private static String[] getArgs(String str) {
        return str.split("\\s");
        /*String[] product = str.split("\\s+(?=(?:[^\"]*\"[^\"]*\")*[^\"]*$)");
        for (int i = 0; i < product.length; i++) {
            product[i] = product[i].replaceAll("\"", "");
        }
        return product;*/
    }
}
