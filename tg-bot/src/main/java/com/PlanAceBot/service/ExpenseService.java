package com.PlanAceBot.service;

import com.PlanAceBot.model.Expense;
import com.PlanAceBot.repository.ExpenseRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class ExpenseService {

    @Autowired
    private ExpenseRepository expenseRepository;

    public void save(Expense expense) {
        expenseRepository.save(expense);
    }

}
