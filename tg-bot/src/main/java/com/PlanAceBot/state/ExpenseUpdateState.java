package com.PlanAceBot.state;

import com.PlanAceBot.model.Expense;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class ExpenseUpdateState {
    private Long expenseId;
    private Expense originalExpense;
    private String fieldToUpdate;
    private double originalAmount;
    private double newAmount;

    public ExpenseUpdateState(Long expenseId, Expense originalExpense, String fieldToUpdate) {
        this.expenseId = expenseId;
        this.originalExpense = originalExpense;
        this.fieldToUpdate = fieldToUpdate;
    }
}
