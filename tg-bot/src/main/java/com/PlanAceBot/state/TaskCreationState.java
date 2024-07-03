package com.PlanAceBot.state;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class TaskCreationState {
    private TaskState state;
    private String title;
    private String description;
    private Integer priority;

    public TaskCreationState() {
        this.state = TaskState.ENTER_TITLE;
    }
}
