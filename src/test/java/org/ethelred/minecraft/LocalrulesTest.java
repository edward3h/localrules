package org.ethelred.minecraft;

import io.lettuce.core.RedisURI;
import io.micronaut.context.ApplicationContext;
import io.micronaut.context.env.PropertySource;
import io.micronaut.http.HttpRequest;
import io.micronaut.http.HttpStatus;
import io.micronaut.http.client.HttpClient;
import io.micronaut.runtime.server.EmbeddedServer;
import org.junit.jupiter.api.*;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Testcontainers
class LocalrulesTest {

    public static final int REDIS_PORT = 6379;
    @Container
    public static GenericContainer redisContainer = new GenericContainer(DockerImageName.parse("redis:6")).withExposedPorts(REDIS_PORT);
    public static
    EmbeddedServer server;
    static List<String> commands = new ArrayList<>(){
        @Override
        public boolean add(String s) {
            System.out.printf("add %s%n", s);
            return super.add(s);
        }

        @Override
        public String get(int index) {
            System.out.println("get");
            return super.get(index);
        }

        @Override
        public void clear() {
            System.out.println("clear");
            super.clear();
        }
    };

    public static Map<String, Object> getProperties() {
        return Map.of(
                "redis.uri", "redis://%s:%d".formatted(redisContainer.getContainerIpAddress(), redisContainer.getMappedPort(6379))
        );
    }

    @BeforeAll
    static void startup() {
        var redisUri = RedisURI.create(redisContainer.getContainerIpAddress(), redisContainer.getMappedPort(6379));
        server = ApplicationContext.run(
                EmbeddedServer.class,
                PropertySource.of("test", Map.of("redis.uri", redisUri.toURI()))
        );
    }

    @AfterAll
    static void shutdown() {
        server.stop();
    }

    @BeforeEach
    void mocking() {
        server.getApplicationContext().registerSingleton(CommandRunner.class, mockCommandRunner());
        commands.clear();
    }

    @Test
    void testItWorks() {
        Assertions.assertTrue(server.isRunning());
    }

    CommandRunner mockCommandRunner() {
        return (command, token) -> {
            commands.add(command);
            return new CommandRunner.CommandResponse(200, "test");
        };
    }

    @Test
    void testTrigger() throws InterruptedException, MalformedURLException {
        var event = new MinecraftServerEvent(MinecraftServerEvent.Type.PLAYER_CONNECTED, "abcde", "test", "Foxcraft", "Steve123", "12345");
        var client = HttpClient.create(new URL("http://" + server.getHost() + ":" + server.getPort()));
        var response = client.toBlocking()
                .exchange(HttpRequest.POST("/webhook", event));
        Assertions.assertEquals(response.status(), HttpStatus.NO_CONTENT);
        Thread.sleep(1000); // crappy
        Assertions.assertEquals(1, commands.size());
        Assertions.assertEquals("give Steve123 diamond", commands.get(0));
    }

    @Test
    void testTrigger2() throws InterruptedException, MalformedURLException {
        var event = new MinecraftServerEvent(MinecraftServerEvent.Type.PLAYER_CONNECTED, "abcde", "test", "Foxcraft", "Steve4", "12345");
        var client = HttpClient.create(new URL("http://" + server.getHost() + ":" + server.getPort()));
        var response = client.toBlocking()
                .exchange(HttpRequest.POST("/webhook", event));
        Assertions.assertEquals(response.status(), HttpStatus.NO_CONTENT);
        response = client.toBlocking()
                .exchange(HttpRequest.POST("/webhook", event));
        Assertions.assertEquals(response.status(), HttpStatus.NO_CONTENT);
        Thread.sleep(1000); // crappy
        Assertions.assertEquals(1, commands.size());
    }
}
