package org.ethelred.minecraft;

import io.lettuce.core.SetArgs;
import io.lettuce.core.api.StatefulRedisConnection;
import io.micronaut.context.annotation.Property;
import io.micronaut.http.HttpResponse;
import io.micronaut.http.annotation.Controller;
import io.micronaut.http.annotation.Post;
import io.micronaut.scheduling.TaskScheduler;
import io.micronaut.scheduling.annotation.Async;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Duration;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;

@Controller
@Debug
public class EventEndpoint {
    private final static Logger LOGGER = LoggerFactory.getLogger(EventEndpoint.class);
    private final StatefulRedisConnection<String, String> redisConnection;
    private final CommandRunner commandRunner;
    private final String token;
    private final TaskScheduler taskScheduler;
    private final Duration delay;

    public EventEndpoint(StatefulRedisConnection<String, String> redisConnection, TaskScheduler taskScheduler,
                         CommandRunner commandRunner, @Property(name = "command.token") String token,
                         @Property(name = "command.delay") Duration delay) {
        this.redisConnection = redisConnection;
        this.taskScheduler = taskScheduler;
        this.commandRunner = commandRunner;
        this.token = token;
        this.delay = delay;
    }

    @Post("/webhook")
    public HttpResponse<?> handleEvent(MinecraftServerEvent event) {
        doStuffWithEvent(event);
        return HttpResponse.noContent();
    }

    public void doStuffWithEvent(MinecraftServerEvent event) {
        if (
                MinecraftServerEvent.Type.PLAYER_CONNECTED == event.type()
                        && "Foxcraft".equals(event.worldName())) {
            var redisCommands = redisConnection.sync();
            var r = redisCommands.set(event.playerName(), "true", SetArgs.Builder.exAt(expiry()).nx());
            LOGGER.debug("r = {}", r);
            if ("OK".equals(r)) {
                taskScheduler.schedule(delay, () ->
                    commandRunner.runCommand(
                            "give %s diamond".formatted(event.playerName()),
                            token
                    )
                );
            }

        }
    }

    private Instant expiry() {
        var now = LocalDateTime.now();
        var d = now.toLocalDate();
        if (now.getHour() >= 5) {
            d = d.plusDays(1);
        }
        return d.atTime(5, 0)
                .atZone(ZoneId.systemDefault())
                .toInstant();
    }

    @Override
    public String toString() {
        return "EventEndpoint[" +
                "redisConnection=" + redisConnection + ", " +
                "commandRunner=" + commandRunner + ", " +
                "token=" + token + ']';
    }

}
