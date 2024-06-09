package de.derioo.reminder;

import com.fasterxml.jackson.databind.module.SimpleModule;
import de.derioo.shadow.jackson.core.JsonGenerator;
import de.derioo.shadow.jackson.databind.ObjectMapper;
import eu.koboo.en2do.Credentials;
import eu.koboo.en2do.MongoManager;
import lombok.extern.java.Log;

import java.io.File;
import java.io.IOException;

@Log
public class Main {
    public static void main(String[] args) {
        ObjectMapper mapper = new ObjectMapper();

        BotConfig config = null;
        try {
            config = mapper.readValue(new File("credentials.json"), BotConfig.class);
        } catch (IOException ignored) {}
        if (config == null) {
            log.warning("Credentials.json not found, trying envs");
            config = BotConfig.builder()
                    .token(System.getenv("BOT_TOKEN"))
                    .connectionString(System.getenv("CONNECTION_STRING"))
                    .db(System.getenv("MONGO_DB"))
                    .build();
        }
        MongoManager manager = new MongoManager(Credentials.of(config.getConnectionString(), config.getDb()));
        try {
            new Bot(config, manager);
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
    }
}