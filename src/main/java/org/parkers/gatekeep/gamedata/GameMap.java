package org.parkers.gatekeep.gamedata;

import discord4j.core.event.domain.message.MessageCreateEvent;
import discord4j.core.object.entity.MessageChannel;
import reactor.core.publisher.Mono;

public class GameMap {
    private static final String SET_ERROR_BAD_ARG_ANSWER = "You need to include an argument describing what you wish to set.\n" +
            "Valid arguments are: `map`, `x`, `y`, `width`, `height`, `size`, `all`";

    public Mono<Void> doSomething(MessageCreateEvent event) {
        String[] args = getArgs(event);

        if (args.length == 0) {
            return event.getMessage().getChannel().flatMap(channel ->
                    channel.createMessage("Error reading message content.")).then();
        }

        if (isNonFinal()) {
            // command processing for setup commands
            switch (args[0]) {
                case "set":
                    return setAttribute(event, args);

                case "finalize":
                    try {
                        finalize();
                    } catch (Throwable throwable) {
                        throwable.printStackTrace();
                    }

                default:
                    // command miss event
                    return event.getMessage().getChannel().then();
            }
        } else {
            // command processing for gameplay commands
            switch (args[0]) {
                default:
                    // command miss event
                    return event.getMessage().getChannel().then();
            }
        }
    }

    private Mono<Void> doInit(MessageCreateEvent event, )

    private static String[] getArgs(MessageCreateEvent event) {
        String message = event.getMessage().getContent().orElse("");

        if (message.length() <= 2) {
            return new String[0];
        }

        message = message.substring(2);
        // todo more advanced regex (detect quotes)
        return message.split("\\s+");
    }

    private Mono<Void> setAttribute(MessageCreateEvent event, String[] args) {

    }





    private MyImage map, blank;

    private boolean mapCompleted = false;
    public boolean isFinal() {
        return mapCompleted;
    }
    public boolean isNonFinal() {
        return !mapCompleted;
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

    private boolean squareInBounds(int x, int y) {
        return x >= 0 && y >= 0 && x < width && y < height;
    }
}
