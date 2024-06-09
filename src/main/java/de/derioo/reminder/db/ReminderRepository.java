package de.derioo.reminder.db;

import eu.koboo.en2do.repository.Collection;
import eu.koboo.en2do.repository.Repository;
import org.bson.types.ObjectId;

import java.util.Comparator;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Collection("reminder")
public interface ReminderRepository extends Repository<Reminder, ObjectId> {

}
