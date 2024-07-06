package com.PlanAceBot.state;

import lombok.Getter;
import lombok.Setter;

import java.sql.Timestamp;

@Getter
@Setter
public class ExpenseCreationState {
    private String title;
    private double amount;
    private Timestamp date;
    private String description;
    private String category;
    private ExpenseState state = ExpenseState.ENTER_TITLE;
}
