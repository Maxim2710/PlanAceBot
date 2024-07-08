package com.PlanAceBot.state;

import lombok.Getter;
import lombok.Setter;

import java.sql.Timestamp;

@Getter
@Setter
public class BudgetCreationState {
    private BudgetState state;
    private String name;
    private double amount;
    private Timestamp startDate;
    private Timestamp endDate;
    private String category;
    private double warningThreshold;
}
