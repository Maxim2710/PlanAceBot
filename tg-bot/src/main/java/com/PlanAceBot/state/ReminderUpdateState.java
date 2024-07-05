package com.PlanAceBot.state;

import com.PlanAceBot.model.Reminder;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class ReminderUpdateState {
    private Long reminderId; // Идентификатор напоминания, которое обновляется
    private String fieldToUpdate; // Поле, которое обновляется (например, "message" или "reminderTime")
    private Reminder originalReminder; // Оригинальное напоминание, до начала обновления

    public ReminderUpdateState(Long reminderId, String fieldToUpdate, Reminder originalReminder) {
        this.reminderId = reminderId;
        this.fieldToUpdate = fieldToUpdate;
        this.originalReminder = originalReminder;
    }
}

