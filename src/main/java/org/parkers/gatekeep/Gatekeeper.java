package org.parkers.gatekeep;

import discord4j.core.DiscordClient;
import discord4j.core.DiscordClientBuilder;
import discord4j.core.event.domain.message.MessageCreateEvent;
import discord4j.core.object.entity.Entity;
import discord4j.core.object.util.Snowflake;
import org.parkers.gatekeep.gamedata.GameMap;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.HashMap;
import java.util.Map;


public class Gatekeeper {
    // contains all valid command inputs
    private static final Map<String, Command> commands = new HashMap<>();

    // contains all maps created for use since the bot began operation
    private static final Map<Snowflake, GameMap> activeMaps = new HashMap<>();

    static {
        commands.put("ping", event -> event.getMessage()
                .getChannel()
                .flatMap(channel -> channel.createMessage("Pong!"))
                .then());

        // map creation command
        commands.put("newmap", event -> event.getMessage()
                .getChannel()
                .map(Entity::getId)
                .filter(snowflake -> !activeMaps.containsKey(snowflake))
                .doOnNext(snowflake -> activeMaps.put(snowflake, new GameMap()))
                .then(event.getMessage().getChannel())
                .flatMap(channel -> channel.createMessage("Blank map initialized for this channel."))
                .then());

        // access lambda for nonfinal map
        Command mapSetEvent = event -> event
                .getMessage()
                .getChannel()
                .filter(channel -> activeMaps.containsKey(channel.getId()))
                .map(channel -> activeMaps.get(channel.getId()))
                .filter(GameMap::isNonFinal)
                .flatMap(map -> map.doSomething(event))
                .then();

        // access lambda for finalized map
        Command mapGameEvent = event -> event
                .getMessage()
                .getChannel()
                .filter(channel -> activeMaps.containsKey(channel.getId()))
                .map(channel -> activeMaps.get(channel.getId()))
                .filter(GameMap::isFinal)
                .flatMap(map -> map.doSomething(event))
                .then();


        // set map data (map, x, y, width, height, size, all)
        commands.put("set", mapSetEvent); // generic

        // finalize the map and prepare it for play
        commands.put("finalize", mapSetEvent);


        // add units to existing map
        commands.put("addnew", mapSetEvent);
        commands.put("addmany", mapSetEvent);

        // move units onto/around board
        commands.put("move", mapGameEvent);
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
