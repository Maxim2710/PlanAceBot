package com.PlanAceBot.service;

import com.PlanAceBot.model.Budget;
import com.PlanAceBot.model.User;
import com.PlanAceBot.repository.BudgetRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;

import java.sql.Timestamp;
import java.util.List;

@Service
public class BudgetService {

    @Autowired
    private BudgetRepository budgetRepository;

    public void save(Budget budget) {
        budgetRepository.save(budget);
    }

    public Budget findByUserChatId(Long chatId) {
        return budgetRepository.findByUserChatId(chatId);
    }

    public List<Budget> findBudgetsByUserId(Long userId) {
        return budgetRepository.findByUser_ChatId(userId);
    }

    public Budget findById(Long budgetId) {
        return budgetRepository.findById(budgetId).orElse(null);
    }

    public void deleteById(Long budgetId) {
        budgetRepository.deleteById(budgetId);
    }

    public List<Budget> findBudgetsByUser(User user) {
        return budgetRepository.findByUser(user);
    }

}
