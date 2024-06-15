package de.derioo.reminder;

import de.derioo.javautils.common.DateUtility;
import de.derioo.javautils.discord.command.CommandManager;
import de.derioo.reminder.commands.DefaultCommand;
import de.derioo.reminder.db.Reminder;
import de.derioo.reminder.db.ReminderRepository;
import eu.koboo.en2do.MongoManager;
import lombok.Getter;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.JDABuilder;
import net.dv8tion.jda.api.OnlineStatus;
import net.dv8tion.jda.api.entities.Activity;
import net.dv8tion.jda.api.requests.GatewayIntent;
import org.jetbrains.annotations.NotNull;

import javax.security.auth.login.LoginException;
import java.util.Calendar;
import java.util.Date;
import java.util.EnumSet;

@Getter
public class Bot {


    private final ReminderRepository repository;
    private final JDA jda;


    public Bot(@NotNull BotConfig config, @NotNull MongoManager manager) throws InterruptedException, LoginException {
        repository = manager.create(ReminderRepository.class);
        Reminder.setBot(this);

        jda = JDABuilder.create(config.getToken(), EnumSet.of(GatewayIntent.GUILD_MESSAGES, GatewayIntent.DIRECT_MESSAGES, GatewayIntent.MESSAGE_CONTENT))
                .setActivity(Activity.listening("Deine Reminders"))
                .setStatus(OnlineStatus.ONLINE)
                .build();
        jda.awaitReady();

        new CommandManager(jda).registerCommand(new DefaultCommand(this));

        repository.findAll(); //load all to call the constructor in the reminder
    }


    public static EmbedBuilder DEFAULT_BUILDER() {
        return new EmbedBuilder().setFooter("Gesendet am " + DateUtility.DATE_FORMAT.format(new Date(Calendar.getInstance().getTimeInMillis())));
    }

}
