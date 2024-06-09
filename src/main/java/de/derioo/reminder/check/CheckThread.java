package de.derioo.reminder.check;

import com.cronutils.model.CronType;
import com.cronutils.model.definition.CronDefinitionBuilder;
import com.cronutils.model.time.ExecutionTime;
import com.cronutils.parser.CronParser;
import de.derioo.javautils.common.DateUtility;
import de.derioo.reminder.Bot;
import de.derioo.reminder.db.Reminder;
import lombok.AllArgsConstructor;
import lombok.extern.java.Log;

import java.awt.*;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.TimeZone;

@AllArgsConstructor
@Log
public class CheckThread extends Thread {

    private final Bot bot;

    @Override
    public void run() {

        log.info("Checking reminders");

        for (Reminder reminder : new ArrayList<>(bot.getRepository().findAll())) {
            if (reminder.getNextExecution() > Calendar.getInstance().getTimeInMillis()) continue;
            if (reminder.getCron() == null) {
                bot.getRepository().deleteById(reminder.getId());
            } else {
                try {
                    reminder.setNextExecution(ExecutionTime.forCron(new CronParser(CronDefinitionBuilder.instanceDefinitionFor(CronType.UNIX)).parse(reminder.getCron())).nextExecution(ZonedDateTime.now()).get().toInstant().toEpochMilli());
                    bot.getRepository().save(reminder);
                } catch (Exception e) {
                    log.severe("Error occured");
                    e.printStackTrace();
                    continue;
                }
            }
            bot.getJda().retrieveUserById(reminder.getUserId()).queue(user -> {
                user.openPrivateChannel().flatMap(channel -> {
                    return channel.sendMessage("<@" + user.getId() + "> " + reminder.getMessage()).addEmbeds(
                            Bot.DEFAULT_BUILDER()
                                    .setColor(Color.GREEN)
                                    .setTitle(":alarm_clock: Deine Reminder ist stattgefunden")
                                    .setDescription(":speech_left: `" + reminder.getMessage() + "`\n:date: " + DateUtility.DATE_FORMAT.format(new Date(reminder.getNextExecution())))
                                    .build()
                    );
                }).queue();
            });
        }

        try {
            Thread.sleep(10_000);
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
        run();
    }
}
