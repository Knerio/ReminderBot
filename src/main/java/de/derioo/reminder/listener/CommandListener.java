package de.derioo.reminder.listener;

import com.cronutils.model.Cron;
import com.cronutils.model.CronType;
import com.cronutils.model.definition.CronDefinition;
import com.cronutils.model.definition.CronDefinitionBuilder;
import com.cronutils.parser.CronParser;
import de.derioo.javautils.common.DateUtility;
import de.derioo.reminder.Bot;
import de.derioo.reminder.db.Reminder;
import de.derioo.reminder.db.ReminderRepository;
import eu.koboo.en2do.MongoManager;
import lombok.AllArgsConstructor;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.MessageEmbed;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import net.dv8tion.jda.api.interactions.commands.OptionMapping;
import org.bson.types.ObjectId;

import java.awt.*;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.List;
import java.util.stream.Collectors;

@AllArgsConstructor
public class CommandListener extends ListenerAdapter {

    private final Bot bot;
    private final ReminderRepository repository;

    @Override
    public void onMessageReceived(MessageReceivedEvent event) {
        if (!event.getMessage().getContentRaw().startsWith("<@" + bot.getJda().getSelfUser().getId() + ">")) return;
        String raw = event.getMessage().getContentRaw().replace("<@" + bot.getJda().getSelfUser().getId() + "> ", "");
        String[] split = raw.split(" ");

        switch (split[0].toLowerCase()) {
            case "list" -> {
                EmbedBuilder builder = Bot.DEFAULT_BUILDER().setTitle(":pen_ballpoint: Deine Reminders:").setColor(Color.GRAY);
                for (Reminder reminder : findRemindersByUser(event.getAuthor().getId())) {
                    builder.addField(new MessageEmbed.Field(":date: " +
                            DateUtility.DATE_FORMAT.format(new Date(reminder.getNextExecution())),
                            ":speech_left: `" + reminder.getMessage() + " `", false));
                }
                event.getMessage().replyEmbeds(builder.build()).queue();
            }
            case "create" -> {
                String dateString = split[1] + " " + split[2];
                Date date;
                try {
                    date = DateUtility.parseDynamic(dateString);
                } catch (ParseException e) {
                    event.getMessage().replyEmbeds(Bot.DEFAULT_BUILDER()
                            .setColor(Color.RED)
                            .setTitle(":x: Dies ist kein Valides Datum")
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
                        .setTitle(":white_check_mark: Saved successfully")
                        .setColor(Color.GREEN)
                        .setDescription("\n`" + reminder.getMessage() + "` \n\n:date: " +
                                DateUtility.DATE_FORMAT.format(new Date(reminder.getNextExecution()))).build()).queue();
            }
        }
    }

    public String join(int index, String[] split) {
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
