package com.PlanAceBot.service;

import com.PlanAceBot.model.Budget;
import com.PlanAceBot.model.User;
import com.PlanAceBot.repository.BudgetRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;

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

}
