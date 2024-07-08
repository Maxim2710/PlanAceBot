package com.PlanAceBot.state;

import com.PlanAceBot.model.Budget;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class BudgetUpdateState {
    private Long budgetId;
    private String fieldToUpdate;
    private Budget originalBudget;

    public BudgetUpdateState(Long budgetId, String fieldToUpdate, Budget originalBudget) {
        this.budgetId = budgetId;
        this.fieldToUpdate = fieldToUpdate;
        this.originalBudget = originalBudget;
    }
}
