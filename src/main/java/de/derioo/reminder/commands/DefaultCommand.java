package de.derioo.reminder.commands;

import com.cronutils.model.Cron;
import com.cronutils.model.time.ExecutionTime;
import de.derioo.javautils.common.DateUtility;
import de.derioo.javautils.discord.command.annotations.Argument;
import de.derioo.javautils.discord.command.annotations.Prefix;
import de.derioo.javautils.discord.command.annotations.SubCommand;
import de.derioo.reminder.Bot;
import de.derioo.reminder.db.Reminder;
import de.derioo.reminder.db.ReminderRepository;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.MessageEmbed;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;
import org.bson.types.ObjectId;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;

import java.awt.*;
import java.time.ZonedDateTime;
import java.util.Arrays;
import java.util.Comparator;
import java.util.Date;
import java.util.List;
import java.util.stream.Collectors;

@Prefix("<@1249334347513729094>")
public class DefaultCommand {

    private final Bot bot;

    private final ReminderRepository repository;

    @Contract(pure = true)
    public DefaultCommand(@NotNull Bot bot) {
        this.bot = bot;
        this.repository = bot.getRepository();
    }


    public void defaultCommand(@NotNull MessageReceivedEvent event, @NotNull Throwable throwable) {
        if (event.getAuthor().isBot()) return;
        event.getMessage().replyEmbeds(Bot.DEFAULT_BUILDER()
                .setColor(Color.RED)
                .setTitle(":x: Ein Fehler ist aufgetreten")
                .setDescription(Arrays.stream(throwable.getStackTrace())
                        .map(StackTraceElement::toString)
                        .collect(Collectors.joining("\n")))
                .addField(new MessageEmbed.Field("Nachricht", throwable.getMessage(), false))
                .build()).queue();
    }



    @SubCommand
    public void create(
            @Argument(type = Argument.ArgumentType.RAW, value = "create") String tmp,
            @Argument(type = Argument.ArgumentType.DATE) @NotNull Date date,
            @Argument(type = Argument.ArgumentType.GREEDY_STRING) String message,
            @NotNull MessageReceivedEvent event
    ) {
        Reminder reminder = Reminder.builder()
                .id(new ObjectId())
                .userId(event.getAuthor().getId())
                .nextExecution(date.getTime())
                .message(message)
                .build();
        repository.save(reminder);
        event.getMessage().replyEmbeds(Bot.DEFAULT_BUILDER()
                .setTitle(":white_check_mark: Erfolgreich gespeichert")
                .setColor(Color.GREEN)
                .setDescription(
                        "\n`" + reminder.getMessage() + "` \n" +
                                ":date: " + DateUtility.DATE_FORMAT.format(new Date(reminder.getNextExecution()))
                ).build()).queue();
    }

    @SubCommand
    public void createCron(
            @Argument(type = Argument.ArgumentType.RAW, value = "create") String tmp,
            @Argument(type = Argument.ArgumentType.CRON_JOB) @NotNull Cron cron,
            @Argument(type = Argument.ArgumentType.GREEDY_STRING) String message,
            @NotNull MessageReceivedEvent event
    ) {
        Reminder reminder = Reminder.builder()
                .id(new ObjectId())
                .userId(event.getAuthor().getId())
                .cron(cron.asString())
                .nextExecution(ExecutionTime.forCron(cron).nextExecution(ZonedDateTime.now()).get().toInstant().toEpochMilli())
                .message(message)
                .build();
        repository.save(reminder);
        event.getMessage().replyEmbeds(Bot.DEFAULT_BUILDER()
                .setTitle(":white_check_mark: Erfolgreich gespeichert")
                .setColor(Color.GREEN)
                .setDescription("\n`" + reminder.getMessage() + "` \n\n:date: " +
                        DateUtility.DATE_FORMAT.format(new Date(reminder.getNextExecution()))).build()).queue();
    }

    @SubCommand
    public void list(
            @Argument(type = Argument.ArgumentType.RAW, value = "list") String s,
            MessageReceivedEvent event
    ) {
        EmbedBuilder builder = Bot.DEFAULT_BUILDER().setTitle(":pen_ballpoint: Deine Reminders:").setColor(Color.GRAY);
        for (Reminder reminder : findRemindersByUser(event.getAuthor().getId())) {
            builder.addField(new MessageEmbed.Field(":date: " + DateUtility.DATE_FORMAT.format(new Date(reminder.getNextExecution())),
                    ":speech_left: `" + reminder.getMessage() + " `\n" +
                            (reminder.getCron() == null ? "" : ":safety_pin: " + reminder.getCron() + "\n")
                            + ":id: " + reminder.getId().toString(),
                    false));
        }
        event.getMessage().replyEmbeds(builder.build()).queue();
    }

    @SubCommand
    public void delete(
            @Argument(type = Argument.ArgumentType.RAW, value = "delete") String s,
            @Argument(type = Argument.ArgumentType.STRING) String idString,
            MessageReceivedEvent event
    ) {
        Reminder reminder;
        try {
            ObjectId id = new ObjectId(idString);
            reminder = repository.findFirstById(id);
            repository.deleteById(id);
            if (reminder == null) throw new IllegalArgumentException("Reminder doesnt exist");
        } catch (Exception e) {
            event.getMessage().replyEmbeds(Bot.DEFAULT_BUILDER()
                    .setColor(Color.RED)
                    .setTitle(":x: Ein Fehler ist aufgetreten")
                    .setDescription("```\n" + e.getMessage() + "\n```")
                    .build()).queue();
            return;
        }
        event.getMessage().replyEmbeds(Bot.DEFAULT_BUILDER()
                .setTitle(":white_check_mark: Erfolgreich entfernt")
                .setColor(Color.GREEN)
                .setDescription(
                        "\n`" + reminder.getMessage() + "` \n" +
                                ":date: " + DateUtility.DATE_FORMAT.format(new Date(reminder.getNextExecution()))).build()).queue();
    }

    public List<Reminder> findRemindersByUser(String userId) {
        return repository.findAll()
                .stream().filter(reminder -> reminder.getUserId().equals(userId))
                .sorted(Comparator.comparing(Reminder::getNextExecution))
                .collect(Collectors.toList());
    }


}
