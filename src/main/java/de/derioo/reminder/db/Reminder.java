package de.derioo.reminder.db;

import com.cronutils.model.CronType;
import com.cronutils.model.definition.CronDefinitionBuilder;
import com.cronutils.model.time.ExecutionTime;
import com.cronutils.parser.CronParser;
import de.derioo.javautils.common.DateUtility;
import de.derioo.reminder.Bot;
import eu.koboo.en2do.repository.entity.Id;
import eu.koboo.en2do.repository.entity.Transient;
import lombok.*;
import lombok.experimental.FieldDefaults;
import lombok.extern.java.Log;
import net.dv8tion.jda.api.JDA;
import org.bson.types.ObjectId;
import org.jetbrains.annotations.NotNull;

import java.awt.*;
import java.time.ZonedDateTime;
import java.util.Date;
import java.util.Timer;
import java.util.TimerTask;

@Getter
@Setter
@ToString
@FieldDefaults(level = AccessLevel.PRIVATE)
@Builder
@Log
public class Reminder {

    @Transient
    @Setter
    static Bot bot;

    @Id
    ObjectId id;

    String userId;

    String cron;

    long nextExecution;

    String message;

    public boolean exists() {
        return bot.getRepository().existsById(id);
    }

    public Reminder(ObjectId id, String userId, String cron, long nextExecution, String message) {
        this.id = id;
        this.userId = userId;
        this.cron = cron;
        this.nextExecution = nextExecution;
        this.message = message;
        log.info("scheduling");
        schedule();
    }

    @SuppressWarnings("unused")
    public Reminder() {
        new Timer().schedule(new TimerTask() {
            @Override
            public void run() {
                log.info("scheduling");
                schedule();
            }
        }, 20);
    }

    private void schedule() {
        Timer timer = new Timer();
        timer.schedule(new TimerTask() {
            @Override
            public void run() {
                if (!exists()) {
                    cancel();
                    return;
                }
                execute();
                delete();
                schedule();
            }
        }, new Date(nextExecution));
    }

    private void delete() {
        log.info("Deleting " + getId() + "  -> " + this);
        if (getCron() == null) {
            bot.getRepository().deleteById(getId());
        } else {
            try {
                setNextExecution(ExecutionTime.forCron(new CronParser(CronDefinitionBuilder.instanceDefinitionFor(CronType.UNIX)).parse(getCron())).nextExecution(ZonedDateTime.now()).get().toInstant().toEpochMilli());
                bot.getRepository().save(this);
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        }
    }

    private void execute() {
        bot.getJda().retrieveUserById(getUserId()).queue(user -> {
            user.openPrivateChannel().flatMap(channel -> {
                return channel.sendMessage("<@" + user.getId() + "> " + getMessage()).addEmbeds(
                        Bot.DEFAULT_BUILDER()
                                .setColor(Color.GREEN)
                                .setTitle(":alarm_clock: Deine Reminder ist stattgefunden")
                                .setDescription(":speech_left: `" + getMessage() + "`\n:date: " + DateUtility.DATE_FORMAT.format(new Date(getNextExecution())))
                                .build()
                );
            }).queue();
        });
    }

}
