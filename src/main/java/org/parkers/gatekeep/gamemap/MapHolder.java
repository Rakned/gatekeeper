package org.parkers.gatekeep.gamemap;

import discord4j.core.event.domain.message.MessageCreateEvent;
import reactor.core.publisher.Mono;

public class MapHolder {
    Mono<Void> doSomething(MessageCreateEvent event) {

        return event.getMessage().getChannel().then();
    }
}
