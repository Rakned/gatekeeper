package org.parkers.gatekeep;

import discord4j.core.event.domain.message.MessageCreateEvent;
import reactor.core.publisher.Mono;

interface Command {
    Mono<Void> execute(MessageCreateEvent e);
}
