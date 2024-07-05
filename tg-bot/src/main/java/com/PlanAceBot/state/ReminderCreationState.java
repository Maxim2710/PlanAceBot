package com.PlanAceBot.state;

import lombok.Getter;
import lombok.Setter;

import java.sql.Timestamp;

@Getter
@Setter
public class ReminderCreationState {
    private String message;
    private Timestamp reminderTime;
    private ReminderState state = ReminderState.ENTER_MESSAGE;

}

