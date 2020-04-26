package org.parkers.gatekeep.gamedata;

import discord4j.core.event.domain.message.MessageCreateEvent;
import discord4j.core.object.entity.Message;
import reactor.core.publisher.Mono;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

public class GameMap {
    private static final String SET_ERROR_NO_ARG_RESPONSE = "You need to include an argument describing what you wish to set.\n" +
            "Valid arguments are: `map`, `x`, `y`, `width`, `height`, `size`, `all`",
            SET_ERROR_BAD_ARG_COUNT_RESPONSE = "You need to include one value to set as the new attribute value.",
            SET_ERROR_BAD_INPUT_RESPONSE = "Sorry, but the value you provided was not within the attribute's bounds.  Please try again.",
            ERROR_PARSE_FAIL = "Trying to parse your arguments threw an error!  Are you sure they are in the correct format?";


    public Mono<Void> doSomething(MessageCreateEvent event) {
        String[] args = getArgs(event);

        if (args.length == 0) {
            return event.getMessage().getChannel().flatMap(channel ->
                    channel.createMessage("Error reading message content.")).then();
        }

        // choose switch interface based on if the map is complete
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
            case "addunit":
                return addOneUnit(event, args);

            case "checkunit":
                return unitCheck(event, args);

            case "move":
                return moveUnit(event, args);

            case "remove":
                return removeUnit(event, args);

            case "view":
                return printMap(event);

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

        // todo replace with more advanced regex (detect quotes)?
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

        if (args.length < 3) {
            return simpleResponse(event, SET_ERROR_BAD_ARG_COUNT_RESPONSE);
        }

        // todo implement
        if (args[1].equalsIgnoreCase("all")) {
            int i = args.length;
            int j = 0;
            String reply = "";
            if (i > 7) {
                i = 7;
            }

            switch (i) {
                case 7:
                    i = getNum(args[6]);
                    if (i > 0) {
                        size = i;
                        reply = "size set to `" + i + "`";
                        j++;
                    }
                case 6:
                    i = getNum(args[5]);
                    if (i > 0) {
                        height = i;
                        reply = "grid height set to `" + i + "`\n" + reply;
                        j++;
                    }
                case 5:
                    i = getNum(args[4]);
                    if (i > 0) {
                        width = i;
                        reply = "grid width set to `" + i + "`\n" + reply;
                        j++;
                    }
                case 4:
                    i = getNum(args[3]);
                    if (i > 0) {
                        uly = i;
                        reply = "origin y set to `" + i + "`\n" + reply;
                        j++;
                    }
                case 3:
                    i = getNum(args[2]);
                    if (i > 0) {
                        ulx = i;
                        reply = "origin x set to `" + i + "`\n" + reply;
                        j++;
                    }
                    break;
            }
            if (setMap(event)) {
                reply = "Map updated!\n\n" + reply;
            } else if (j == 0) {
                reply = "No valid update information found.";
            }

            return simpleResponse(event, reply);
        }

        int value = getNum(args[2]);
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
            MyImage image = MyImage.readImage(event);
            if (image != null) {
                map = image;
                return true;
            }
        } catch (Exception ignored) {}

        return false;
    }

    private Mono<Void> addOneUnit(MessageCreateEvent event, String[] args) {
        if (args.length != 2) {
            return simpleResponse(event, "Message must include a single-token name for your unit.");
        }
        if (units.containsKey(args[1])) {
            return simpleResponse(event, "This game already contains a unit named `" + args[1] + "`.");
        }

        // todo put image code into "doOnNext" for latency decrease?
        MyImage image = MyImage.readImage(event);
        if (image == null) {
            return simpleResponse(event, "I could not find an image attached to your request.  Please try again.");
        }

        units.put(args[1], new Unit(image, size));
        return simpleResponse(event, "Successfully created unit `" + args[1] + "`.");
    }
    private Mono<Void> unitCheck(MessageCreateEvent event, String[] args) {
        if (args.length != 2) {
            return simpleResponse(event, "Please include only the single-token name of the unit you'd like information on.");
        }

        if (!units.containsKey(args[1])) {
            return errUnitNotFound(event, args[1]);
        }

        return event.getMessage().getChannel().flatMap(channel -> channel.createMessage(spec -> {
            Unit unit = units.get(args[1]);
            String message = "`" + args[1] + "` is located at `(" + unit.x + ", " + unit.y + ")`";
            try {
                unit.portrait.attach(spec);
            } catch (IOException error) {
                message += "\nIO error encountered when retrieving unit data.";
            }
            spec.setContent(message);
        })).then();
    }
    private Mono<Void> moveUnit(MessageCreateEvent event, String[] args) {
        if (args.length != 4) {
            return simpleResponse(event, "Proper format: `g!move <unit> <tx> <ty>");
        }
        if (!units.containsKey(args[1])) {
            return errUnitNotFound(event, args[1]);
        }

        int x, y;
        try {
            x = getNum(args[2]);
            y = getNum(args[3]);
        } catch (Exception e) {
            return simpleResponse(event, ERROR_PARSE_FAIL);
        }

        if (moveUnit(units.get(args[1]), x, y)) {
            return simpleResponse(event, "Unit moved successfully!");
        } else {
            return simpleResponse(event, "Unit could not be moved...");
        }
    }
    private Mono<Void> removeUnit(MessageCreateEvent event, String[] args) {
        if (args.length != 2) {
            return simpleResponse(event, "Message must include the single-token name of the unit to remove.");
        }
        if (!units.containsKey(args[1])) {
            return errUnitNotFound(event, args[1]);
        }

        dropUnit(units.get(args[1]));
        return simpleResponse(event, "Unit `" + args[1] + "` removed from board.");
    }

    private Mono<Void> simpleResponse(MessageCreateEvent event, String message) {
        return response(event, message).then();
    }
    private Mono<Message> response(MessageCreateEvent event, String message) {
        return event.getMessage().getChannel().flatMap(messageChannel -> messageChannel.createMessage(message));
    }
    private Mono<Void> errUnitNotFound(MessageCreateEvent event, String unitName) {
        return simpleResponse(event, "Unit `" + unitName + "` was not found.  Please check your spelling and try again.");
    }




    private MyImage map, blank;
    private int ulx, uly, width, height, size;
    private Map<String, Unit> units = null;
    private boolean[][] spaceOccupation = null;

    private boolean mapCompleted = false;


    private Mono<Void> complete(MessageCreateEvent event) {
        // map validity check
        if (mapCompleted) {
            return simpleResponse(event, "Map is already flagged as complete.");
        } else if ((ulx + width*size) > map.getWidth()) {
            return simpleResponse(event, "Error: Grid width exceeds image width!");
        } else if ((uly + height*size) > map.getHeight()) {
            return simpleResponse(event, "Error: Grid height exceeds image height!");
        } else {
            mapCompleted = true;
        }

        // any code to customize the map should go here (if adding grid lines, labels etc)

        // create blank copy of map used to clear spaces
        blank = map.copy();

        // remaining object initialization
        units = new HashMap<>();
        spaceOccupation = new boolean[width][height];
        return simpleResponse(event, "Map has been succesfully finalized.");
    }

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
    private void dropUnit(Unit unit) {
        clearSquare(unit.x, unit.y);
        unit.setPos(-1, -1);
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
            spaceOccupation[unit.x][unit.y] = true;
        }
    }
    private Mono<Void> printMap(MessageCreateEvent event) {
        return event.getMessage().getChannel().flatMap(ch -> ch.createMessage(spec -> {
            try {
                map.attach(spec);
            } catch (IOException e) {
                spec.setContent("IO error encountered drawing map.");
            }
        })).then();
    }

    public boolean isFinal() {
        return mapCompleted;
    }
    public boolean isNonFinal() {
        return !mapCompleted;
    }
    private boolean squareInBounds(int x, int y) {
        return x >= 0 && y >= 0 && x < width && y < height;
    }

    private static int getNum(String str) {
        int i = -1;
        try {
            i = Integer.parseInt(str);
        } catch (Exception ignored) {}
        return i;
    }
}
