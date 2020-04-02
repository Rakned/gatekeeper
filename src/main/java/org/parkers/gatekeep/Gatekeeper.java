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

        commands.put("drawimage", event -> {
            try {
                BufferedImage one, two;
                URLConnection connection;

                connection = new URL("https://cdn.discordapp.com/attachments/686269264067690595/693937117587308574/cathS.png").openConnection();
                connection.setRequestProperty("User-Agent", "Gatekeeper");
                one = ImageIO.read(connection.getInputStream());

                connection = new URL("https://cdn.discordapp.com/attachments/686269264067690595/693937350857982073/toot.png").openConnection();
                connection.setRequestProperty("User-Agent", "Gatekeeper");
                two = ImageIO.read(connection.getInputStream());

                Graphics2D g2d = one.createGraphics();
                g2d.drawImage(two, null, 64, 64);
                g2d.dispose();

                g2d = two.createGraphics();
                g2d.scale(0.5, 0.5);
                g2d.dispose();

                g2d = one.createGraphics();
                g2d.drawImage(two, null, 512, 512);
                g2d.dispose();


                ByteArrayOutputStream out = new ByteArrayOutputStream();
                ImageIO.write(one, "png", out);

                return event
                        .getMessage()
                        .getChannel()
                        .flatMap(channel -> channel.createMessage(spec ->
                                spec.addFile("test.png", new ByteArrayInputStream(out.toByteArray()))))
                        .then();
            } catch (Exception ignored) { }
            return event.getMessage().getChannel().then();
        });
    }

    public static void main( String[] args ) {
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
