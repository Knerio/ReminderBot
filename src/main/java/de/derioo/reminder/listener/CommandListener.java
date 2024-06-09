package de.derioo.reminder.listener;

import com.cronutils.model.Cron;
import com.cronutils.model.CronType;
import com.cronutils.model.definition.CronDefinitionBuilder;
import com.cronutils.model.time.ExecutionTime;
import com.cronutils.parser.CronParser;
import de.derioo.javautils.common.DateUtility;
import de.derioo.reminder.Bot;
import de.derioo.reminder.db.Reminder;
import de.derioo.reminder.db.ReminderRepository;
import lombok.AllArgsConstructor;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.MessageEmbed;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import org.bson.types.ObjectId;
import org.jetbrains.annotations.NotNull;

import java.awt.*;
import java.text.ParseException;
import java.time.ZonedDateTime;
import java.util.Comparator;
import java.util.Date;
import java.util.List;
import java.util.stream.Collectors;

@AllArgsConstructor
public class CommandListener extends ListenerAdapter {

    private final Bot bot;
    private final ReminderRepository repository;

    @Override
    public void onMessageReceived(MessageReceivedEvent event) {
        try {
            execute(event);
        } catch (Exception e) {
            if (e instanceof IndexOutOfBoundsException) {
                event.getMessage().replyEmbeds(Bot.DEFAULT_BUILDER()
                        .setColor(Color.RED)
                        .setTitle(":x: Ein Fehler ist aufgetreten")
                        .setDescription("Du hast die falschen Argumente angegeben")
                        .build()).queue();
                return;
            }
            event.getMessage().replyEmbeds(Bot.DEFAULT_BUILDER()
                    .setColor(Color.RED)
                    .setTitle(":x: Ein Fehler ist aufgetreten")
                    .setDescription("```\n" + event.getMessage() + "\n```")
                    .build()).queue();
        }
    }

    private void execute(MessageReceivedEvent event) {
        if (!event.getMessage().getContentRaw().startsWith("<@" + bot.getJda().getSelfUser().getId() + ">")) return;
        String raw = event.getMessage().getContentRaw().replace("<@" + bot.getJda().getSelfUser().getId() + "> ", "");
        String[] split = raw.split(" ");

        switch (split[0].toLowerCase()) {
            case "list" -> {
                EmbedBuilder builder = Bot.DEFAULT_BUILDER().setTitle(":pen_ballpoint: Deine Reminders:").setColor(Color.GRAY);
                for (Reminder reminder : findRemindersByUser(event.getAuthor().getId())) {
                    builder.addField(new MessageEmbed.Field(":date: " + DateUtility.DATE_FORMAT.format(new Date(reminder.getNextExecution())),
                            ":speech_left: `" + reminder.getMessage() + " `\n" +
                                    (reminder.getCron() == null ? "" : ":safety_pin: " + reminder.getCron() + "\n")
                                    + ":id:" + reminder.getId().toString(),
                            false));
                }
                event.getMessage().replyEmbeds(builder.build()).queue();
            }
            case "delete" -> {
                Reminder reminder;
                try {
                    ObjectId id = new ObjectId(split[1]);
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
                        .setTitle(":white_check_mark: Erfolgreich gelöscht")
                        .setColor(Color.GREEN)
                        .setDescription(
                                "\n`" + reminder.getMessage() + "` \n" +
                                        ":date: " + DateUtility.DATE_FORMAT.format(new Date(reminder.getNextExecution()))).build()).queue();
            }
            case "create-cron" -> {
                String join = join(1, split);
                String cron = join.substring(0, join.indexOf(":"));
                String after = join.substring(join.indexOf(":") + 2);
                try {

                    Cron expression = new CronParser(CronDefinitionBuilder.instanceDefinitionFor(CronType.UNIX)).parse(cron);
                    Reminder reminder = Reminder.builder()
                            .id(new ObjectId())
                            .userId(event.getAuthor().getId())
                            .cron(cron)
                            .nextExecution(ExecutionTime.forCron(expression).nextExecution(ZonedDateTime.now()).get().toInstant().toEpochMilli())
                            .message(after)
                            .build();
                    repository.save(reminder);
                    event.getMessage().replyEmbeds(Bot.DEFAULT_BUILDER()
                            .setTitle(":white_check_mark: Erfolgreich gespeichert")
                            .setColor(Color.GREEN)
                            .setDescription("\n`" + reminder.getMessage() + "` \n\n:date: " +
                                    DateUtility.DATE_FORMAT.format(new Date(reminder.getNextExecution()))).build()).queue();
                } catch (Exception e) {
                    e.printStackTrace();
                    event.getMessage().replyEmbeds(Bot.DEFAULT_BUILDER()
                            .setColor(Color.RED)
                            .setTitle(":x: Dies ist kein Valides 'Cron'")
                            .setDescription("```\n" + cron + "\n``` (" + e.getMessage() + ")")
                            .build()).queue();
                }
            }
            case "create" -> {
                String dateString = split[1] + " " + split[2];
                Date date;
                try {
                    date = DateUtility.parseDynamic(dateString);
                } catch (ParseException e) {
                    event.getMessage().replyEmbeds(Bot.DEFAULT_BUILDER()
                            .setColor(Color.RED)
                            .setTitle(":x: Dies ist kein Valides Datum (" + e.getMessage() + ")")
                            .setDescription(dateString)
                            .build()).queue();
                    return;
                }
                Reminder reminder = Reminder.builder()
                        .id(new ObjectId())
                        .userId(event.getAuthor().getId())
                        .nextExecution(date.getTime())
                        .message(join(3 - (dateString.contains(",") ? 0 : 1), split))
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
        }
    }

    public String join(int index, String @NotNull [] split) {
        StringBuilder joinedString = new StringBuilder();

        for (int i = index; i < split.length; i++) {
            joinedString.append(split[i]);
            if (i < split.length - 1) {
                joinedString.append(" ");
            }
        }
        return joinedString.toString();
    }

    public List<Reminder> findRemindersByUser(String userId) {
        return repository.findAll()
                .stream().filter(reminder -> reminder.getUserId().equals(userId))
                .sorted(Comparator.comparing(Reminder::getNextExecution))
                .collect(Collectors.toList());
    }
}
