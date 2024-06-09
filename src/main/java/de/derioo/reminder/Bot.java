package de.derioo.reminder;

import de.derioo.javautils.common.DateUtility;
import de.derioo.reminder.check.CheckThread;
import de.derioo.reminder.db.ReminderRepository;
import de.derioo.reminder.listener.CommandListener;
import eu.koboo.en2do.MongoManager;
import lombok.Getter;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.JDABuilder;
import net.dv8tion.jda.api.OnlineStatus;
import net.dv8tion.jda.api.entities.Activity;
import net.dv8tion.jda.api.interactions.commands.build.Commands;
import net.dv8tion.jda.api.requests.GatewayIntent;
import org.jetbrains.annotations.NotNull;

import java.util.Date;
import java.util.EnumSet;

@Getter
public class Bot {


    private final ReminderRepository repository;
    private final JDA jda;


    public Bot(@NotNull BotConfig config, MongoManager manager) throws InterruptedException {
        repository = manager.create(ReminderRepository.class);

        jda = JDABuilder.create(config.getToken(), EnumSet.of(GatewayIntent.GUILD_MESSAGES, GatewayIntent.DIRECT_MESSAGES, GatewayIntent.MESSAGE_CONTENT))
                .addEventListeners(new CommandListener(this, repository))
                .setActivity(Activity.listening("Deine Reminders"))
                .setStatus(OnlineStatus.ONLINE)
                .build();
        jda.awaitReady();

        jda.updateCommands().addCommands(
                Commands.message("remind"),
                Commands.message("remind-cron")
        ).queue();

        new CheckThread(this).start();

    }


    public static EmbedBuilder DEFAULT_BUILDER() {
        return new EmbedBuilder().setFooter("Gesendet am " + DateUtility.DATE_FORMAT.format(new Date()));
    }

}
