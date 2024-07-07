package com.PlanAceBot.service;

import com.PlanAceBot.model.Expense;
import com.PlanAceBot.repository.ExpenseRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class ExpenseService {

    @Autowired
    private ExpenseRepository expenseRepository;

    public void save(Expense expense) {
        expenseRepository.save(expense);
    }

    public Expense findById(Long id) {
        return expenseRepository.findById(id).orElse(null);
    }

    public List<Expense> findExpensesByUserId(Long userId) {
        return expenseRepository.findByUser_ChatId(userId);
    }

}
