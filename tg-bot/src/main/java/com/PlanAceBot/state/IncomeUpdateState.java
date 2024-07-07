package com.PlanAceBot.state;

import com.PlanAceBot.model.Income;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class IncomeUpdateState {
    private Long incomeId;
    private String fieldToUpdate;
    private Income originalIncome;

    public IncomeUpdateState(Long incomeId, String fieldToUpdate, Income originalIncome) {
        this.incomeId = incomeId;
        this.fieldToUpdate = fieldToUpdate;
        this.originalIncome = originalIncome;
    }
}
