package com.PlanAceBot.state;

import com.PlanAceBot.model.Task;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class TaskUpdateState {
    private int taskId;
    private String fieldToUpdate;
    private Task originalTask;

    public TaskUpdateState(int taskId, String fieldToUpdate, Task originalTask) {
        this.taskId = taskId;
        this.fieldToUpdate = fieldToUpdate;
        this.originalTask = originalTask;
    }

}