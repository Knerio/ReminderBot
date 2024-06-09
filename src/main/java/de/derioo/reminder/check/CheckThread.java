package de.derioo.reminder.check;

import com.fasterxml.jackson.databind.util.ArrayIterator;
import de.derioo.javautils.common.DateUtility;
import de.derioo.reminder.Bot;
import de.derioo.reminder.db.Reminder;
import lombok.AllArgsConstructor;
import lombok.extern.java.Log;
import net.dv8tion.jda.api.entities.User;
import net.dv8tion.jda.api.entities.channel.concrete.PrivateChannel;
import net.dv8tion.jda.api.requests.restaction.CacheRestAction;

import java.awt.*;
import java.util.ArrayList;
import java.util.Date;

@AllArgsConstructor
@Log
public class CheckThread extends Thread {

    private final Bot bot;

    @Override
    public void run() {

        log.info("Checking reminders");

        for (Reminder reminder : new ArrayList<>(bot.getRepository().findAll())) {
            if (reminder.getNextExecution() > System.currentTimeMillis()) continue;
            bot.getRepository().deleteById(reminder.getId());
            bot.getJda().retrieveUserById(reminder.getUserId()).queue(user -> {
                user.openPrivateChannel().flatMap(channel -> {
                    return channel.sendMessage("<@" + user.getId() + ">").addEmbeds(
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
            Thread.sleep(30_000);
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
        run();
    }
}
