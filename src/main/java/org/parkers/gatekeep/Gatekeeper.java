package org.parkers.gatekeep;

import discord4j.core.DiscordClient;
import discord4j.core.DiscordClientBuilder;
import discord4j.core.event.domain.message.MessageCreateEvent;
import discord4j.core.object.entity.Attachment;
import discord4j.core.object.entity.Entity;
import discord4j.core.object.entity.Message;
import discord4j.core.object.util.Snowflake;
import discord4j.core.spec.MessageCreateSpec;
import org.parkers.gatekeep.gamedata.GameMap;
import org.parkers.gatekeep.gamedata.MyImage;
import org.parkers.gatekeep.gamedata.NewMap;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.net.URL;
import java.net.URLConnection;
import java.util.HashMap;
import java.util.Map;


public class Gatekeeper {
    // contains all valid command inputs
    private static final Map<String, Command> commands = new HashMap<>();
    private static final Map<Snowflake, GameMap> gameMaps = new HashMap<>();

    private static Attachment globalFile;
    private static NewMap map;
    private static NewMap demoMap;

    static {
        commands.put("ping", event -> event.getMessage()
                .getChannel()
                .flatMap(channel -> channel.createMessage("Pong!"))
                .then());

        commands.put("newmap", event -> event.getMessage()
                .getChannel()
                .map(Entity::getId)
                .filter(snowflake -> !gameMaps.containsKey(snowflake))
                .doOnNext(snowflake -> gameMaps.put(snowflake, new GameMap()))
                .then(event.getMessage().getChannel())
                .flatMap(channel -> channel.createMessage("Blank map initialized for this channel."))
                .then());


        Command mapcom = event -> event.getMessage()
                .getChannel()
                .filter(channel -> gameMaps.containsKey(channel.getId()))
                .flatMap(channel -> channel.createMessage(spec -> gameMaps.get(channel.getId()).command(event, spec)))
                .then();

        // remember to test thoroughly as you implement!
        commands.put("map", mapcom);

        commands.put("move", mapcom);

        commands.put("game", mapcom);

        commands.put("drawimage",
                event -> event.getMessage()
                .getChannel()
                .flatMap(channel -> channel.createMessage(GameMap::imageResponseTest))
                .then());

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

        commands.put("demoSubImage", event -> {
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

            return event.getMessage()
                    .getChannel()
                    .flatMap(channel -> channel.createMessage(spec -> map.printSquareBcg(x, y, spec)))
                    .then();
        });
    }

    public static void main(String[] args) {
        final DiscordClient client = new DiscordClientBuilder(args[0]).build();

        try {
            demoMap = new NewMap();
            demoMap.ulx = 128;
            demoMap.uly = 128;
            demoMap.width = 6;
            demoMap.height = 3;
            demoMap.size = 256;

            MyImage img = MyImage.readUrl
                    ("https://cdn.discordapp.com/attachments/686269264067690595/693937117587308574/cathS.png", "cathS.png");
            demoMap.setMapImage(img);

            img = MyImage.readUrl
                    ("https://cdn.discordapp.com/attachments/686269264067690595/693937350857982073/toot.png", "toot.png");
            demoMap.setUnitImage(img);

            demoMap.complete();
        } catch (Exception e) {
            e.printStackTrace();
            demoMap = null;
        }

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

    private static void saveFile( Attachment attach ) {
        globalFile = attach;
    }

    private static void createMessage(MessageCreateSpec spec) {
        if (globalFile == null) {
            spec.setContent("Error: no saved file detected!");
        } else {
            String url = globalFile.getUrl();
            try {
                URLConnection connection = new URL(url).openConnection();
                connection.setRequestProperty("User-Agent", "Gatekeeper");
                spec.addFile(globalFile.getFilename(), connection.getInputStream());
            } catch (Exception e) {
                spec.setContent("Exception encountered while accessing URL <"+url+">");
            }
        }
    }

    private static Attachment getAttachment(Message m) {
        return m.getAttachments().iterator().next();
    }

    private static Mono<Void> newMap(MessageCreateEvent e) {


        return null;
    }
}
