package org.parkers.gatekeep.gamedata;

import discord4j.core.event.domain.message.MessageCreateEvent;
import discord4j.core.object.entity.Attachment;
import discord4j.core.object.entity.Message;
import reactor.core.publisher.Mono;

import java.util.HashMap;
import java.util.Map;

public class GameMap {
    private static final String SET_ERROR_NO_ARG_RESPONSE = "You need to include an argument describing what you wish to set.\n" +
            "Valid arguments are: `map`, `x`, `y`, `width`, `height`, `size`, `all`",
            SET_ERROR_BAD_ARG_COUNT_RESPONSE = "You need to include one value to set as the new attribute value.",
            SET_ERROR_BAD_INPUT_RESPONSE = "Sorry, but the value you provided was not within the attribute's bounds.  Please try again.";


    public Mono<Void> doSomething(MessageCreateEvent event) {
        String[] args = getArgs(event);

        if (args.length == 0) {
            return event.getMessage().getChannel().flatMap(channel ->
                    channel.createMessage("Error reading message content.")).then();
        }

        if (isNonFinal()) {
            return doGameInit(event, args);
        } else {
            return doGameUpdate(event, args);
        }
    }
    private Mono<Void> doGameInit(MessageCreateEvent event, String[] args) {
        switch (args[0]) {
            case "set":
                return setAttribute(event, args);

            case "complete":
                return complete(event);

            default:
                return event.getMessage().getChannel().then();
        }
    }
    private Mono<Void> doGameUpdate(MessageCreateEvent event, String[] args) {
        switch (args[0]) {
            default:
                return event.getMessage().getChannel().then();
        }
    }
    private static String[] getArgs(MessageCreateEvent event) {
        String message = event.getMessage().getContent().orElse("");

        if (message.length() <= 2) {
            return new String[0];
        }

        message = message.substring(2);

        // todo replace with more advanced regex (detect quotes?)
        return message.split("\\s+");
    }

    private Mono<Void> setAttribute(MessageCreateEvent event, String[] args) {
        if (args.length == 1) {
            return simpleResponse(event, SET_ERROR_NO_ARG_RESPONSE);
        }

        if (args[1].equalsIgnoreCase("map")) {
            if (setMap(event)) {
                return simpleResponse(event, "Map set successfully.");
            } else {
                return simpleResponse(event, "Map could not be set.  Check if the file was attached properly, and try again.");
            }
        }

        // todo implement
        if (args[1].equalsIgnoreCase("all")) {
            return simpleResponse(event, "unimplemented");
        }

        if (args.length < 3) {
            return simpleResponse(event, SET_ERROR_BAD_ARG_COUNT_RESPONSE);
        }

        int value = Integer.parseInt(args[2]);
        if (value <= 0) {
            return simpleResponse(event, SET_ERROR_BAD_INPUT_RESPONSE);
        }

        switch (args[1]) {
            case "x":
                ulx = value;
                return simpleResponse(event, "Upper left corner X value set.");

            case "y":
                uly = value;
                return simpleResponse(event, "Upper left corner Y value set.");

            case "width":
                width = value;
                return simpleResponse(event, "Grid width dimension set.");

            case "height":
                height = value;
                return simpleResponse(event, "Grid height dimension set.");

            case "size":
                size = value;
                return simpleResponse(event, "Grid square size set.");

            default:
                return simpleResponse(event, "Could not recognize argument `" + args[1] + "`.  Please try again.");
        }
    }
    private boolean setMap(MessageCreateEvent event) {
        try {
            Attachment file = event.getMessage().getAttachments().iterator().next();
            map = MyImage.readUrl(file.getUrl(), file.getFilename());

            return true;
        } catch (Exception ignored) {
            return false;
        }
    }

    private Mono<Void> simpleResponse(MessageCreateEvent event, String message) {
        return response(event, message).then();
    }
    private Mono<Message> response(MessageCreateEvent event, String message) {
        return event.getMessage().getChannel().flatMap(messageChannel -> messageChannel.createMessage(message));
    }




    private MyImage map, blank;
    private int ulx, uly, width, height, size;
    private Map<String, MyImage> units = null;
    private boolean[][] spaceOccupation = null;

    private boolean mapCompleted = false;
    public boolean isFinal() {
        return mapCompleted;
    }
    public boolean isNonFinal() {
        return !mapCompleted;
    }


    private Mono<Void> complete(MessageCreateEvent event) {
        // map completion check
        if (mapCompleted) {
            return simpleResponse(event, "Map is already flagged as complete.");
        } else {
            mapCompleted = true;
        }

        // todo: validity check (ensure grid fits w/in scope of map)

        // any code to customize the map should go here (if adding grid lines, labels etc)

        // create blank copy of map for clearing spaces
        blank = map.copy();

        // remaining object initialization
        units = new HashMap<>();
        spaceOccupation = new boolean[width][height];
        return simpleResponse(event, "Map has been succesfully finalized.");
    }

    private boolea

    private boolean moveUnit(Unit unit, int x, int y) {
        if (spaceOccupation[x][y]) {
            return false;
        } else {
            clearSquare(unit.x, unit.y);
            unit.setPos(x, y);
            drawUnit(unit);
            return true;
        }
    }

    private void clearSquare(int x, int y) {
        if (squareInBounds(x, y)) {
            int a = ulx + x * size, b = uly + y * size;
            map.drawImage(blank.subImage(a, b, size, size), a, b);
            spaceOccupation[x][y] = false;
        }
    }
    private void drawUnit(Unit unit) {
        if (squareInBounds(unit.x, unit.y)) {
            map.drawImage(unit.portrait, ulx + unit.x * size, uly + unit.y * size);
        }
    }
    private boolean squareInBounds(int x, int y) {
        return x >= 0 && y >= 0 && x < width && y < height;
    }
}
