package de.derioo.reminder.db;

import eu.koboo.en2do.repository.Collection;
import eu.koboo.en2do.repository.Repository;
import org.bson.types.ObjectId;

@Collection("reminder")
public interface ReminderRepository extends Repository<Reminder, ObjectId> {

}
