package com.PlanAceBot.state;

import lombok.Getter;
import lombok.Setter;

import java.sql.Timestamp;

@Getter
@Setter
public class IncomeCreationState {
    private String title;
    private double amount;
    private Timestamp date;
    private String description;
    private String category;
    private IncomeState state = IncomeState.ENTER_TITLE;
}
