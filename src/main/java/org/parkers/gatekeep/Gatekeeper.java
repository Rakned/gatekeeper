package org.parkers.gatekeep;

import discord4j.core.DiscordClient;
import discord4j.core.DiscordClientBuilder;
import discord4j.core.event.domain.message.MessageCreateEvent;
import discord4j.core.object.entity.Entity;
import discord4j.core.object.util.Snowflake;
import org.parkers.gatekeep.gamedata.MyImage;
import org.parkers.gatekeep.gamedata.NewMap;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.HashMap;
import java.util.Map;


public class Gatekeeper {
    // contains all valid command inputs
    private static final Map<String, Command> commands = new HashMap<>();

    // contains all maps created for use since the bot began operation
    private static final Map<Snowflake, NewMap> activeMaps = new HashMap<>();

    static {
        commands.put("ping", event -> event.getMessage()
                .getChannel()
                .flatMap(channel -> channel.createMessage("Pong!"))
                .then());

        commands.put("newmap", event -> event.getMessage()
                .getChannel()
                .map(Entity::getId)
                .filter(snowflake -> !activeMaps.containsKey(snowflake))
                .doOnNext(snowflake -> activeMaps.put(snowflake, new NewMap()))
                .then(event.getMessage().getChannel())
                .flatMap(channel -> channel.createMessage("Blank map initialized for this channel."))
                .then());


        Command mapEvent = event -> event
                .getMessage()
                .getChannel()
                .filter(channel -> activeMaps.containsKey(channel.getId()))
                .map(channel -> activeMaps.get(channel.getId()))
                .flatMap(map -> map.doSomething(event))
                .then();



        // remember to test thoroughly as you implement!
        commands.put("map", mapEvent);

        commands.put("move", mapEvent);

        commands.put("game", mapEvent);
    }


    // demo code
    private static NewMap map;
    static {
        commands.put("demoSetup", event -> {
            map = new NewMap();
            map.ulx = 128;
            map.uly = 128;
            map.width = 6;
            map.height = 3;
            map.size = 256;

            try {
                MyImage img = MyImage.readUrl
                        ("https://cdn.discordapp.com/attachments/686269264067690595/693937117587308574/cathS.png", "cathS.png");
                map.setMapImage(img);

                img = MyImage.readUrl
                        ("https://cdn.discordapp.com/attachments/686269264067690595/693937350857982073/toot.png", "toot.png");
                map.setUnitImage(img);

                map.complete();

                return event.getMessage().getChannel().flatMap(channel -> channel.createMessage("Map instantiated successfully!")).then();
            } catch (Exception e) {
                e.printStackTrace();
            }
            return event.getMessage().getChannel().flatMap(channel -> channel.createMessage("Image loading error.")).then();
        });

        commands.put("demoRender", event -> event.getMessage()
                .getChannel()
                .flatMap(channel -> channel.createMessage(map::printMap))
                .then());

        commands.put("demoUnit", event -> event.getMessage()
                .getChannel()
                .flatMap(channel -> channel.createMessage(map::printUnit))
                .then());

        commands.put("demoDraw", event -> {
            String[] args = event.getMessage().getContent().orElse("").split("\\s+");
            if (args.length < 3) {
                return event.getMessage()
                        .getChannel()
                        .flatMap(channel -> channel.createMessage("Bad argument count!"))
                        .then();
            }

            int x, y;
            x = Integer.parseInt(args[1]);
            y = Integer.parseInt(args[2]);

            map.addSquare(x, y);

            return event.getMessage()
                    .getChannel()
                    .flatMap(channel -> channel.createMessage("Unit should be drawn at (" + x + ", " + y + ")"))
                    .then();
        });

        commands.put("demoClear", event -> {
            String[] args = event.getMessage().getContent().orElse("").split("\\s+");
            if (args.length < 3) {
                return event.getMessage()
                        .getChannel()
                        .flatMap(channel -> channel.createMessage("Bad argument count!"))
                        .then();
            }

            int x, y;
            x = Integer.parseInt(args[1]);
            y = Integer.parseInt(args[2]);

            map.clearSquare(x, y);

            return event.getMessage()
                    .getChannel()
                    .flatMap(channel -> channel.createMessage("Space at (" + x + ", " + y + ") should be clear."))
                    .then();
        });
    }


    public static void main(String[] args) {
        final DiscordClient client = new DiscordClientBuilder(args[0]).build();

        // matches message with bot command, if extant
        client.getEventDispatcher().on(MessageCreateEvent.class)
                .filter(event -> event.getMessage().getAuthor().map(author -> !author.isBot()).orElse(false))
                .flatMap(
                        event -> Mono.justOrEmpty(event.getMessage().getContent())
                        .flatMap(
                                content -> Flux.fromIterable(commands.entrySet())
                                .filter(entry -> content.startsWith("g!" + entry.getKey()))
                                .flatMap(entry -> entry.getValue().execute(event))
                                .next() ) ).subscribe();

        //client.getEventDispatcher().on(MessageCreateEvent.class)

        client.login().block();

        client.logout();
    }
}
