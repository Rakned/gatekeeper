package org.parkers.gatekeep.gamedata;

import discord4j.core.object.entity.Message;
import discord4j.core.object.entity.MessageChannel;
import discord4j.core.spec.MessageCreateSpec;
import reactor.core.publisher.Mono;

import java.io.IOException;

public class NewMap {
    private MyImage map, unit, blank;
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
        if (mapCompleted) {
            return;
        } else {
            mapCompleted = true;
        }

        // create array of blank images to reference
        blank = map.copy();

        if (unit != null) {
            unit = unit.copyResize(size);
        }
    }

    public void clearSquare(int x, int y) {
        if (!mapCompleted)
            return;
        if (squareInBounds(x, y)) {
            int a = ulx + x * size, b = uly + y * size;
            map.drawImage(blank.subImage(a, b, size, size), a, b);
        }
    }

    public void addSquare(int x, int y) {
        if (!mapCompleted)
            return;
        if (squareInBounds(x, y)) {
            map.drawImage(unit, ulx + x * size, uly + y * size);
        }
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

    public Mono<Message> printUnit(MessageChannel channel) {
        return channel.createMessage(this::printUnit);
    }

    private boolean squareInBounds(int x, int y) {
        return x >= 0 && y >= 0 && x < width && y < height;
    }
}
