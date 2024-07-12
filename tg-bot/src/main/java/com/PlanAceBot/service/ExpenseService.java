package com.PlanAceBot.service;

import com.PlanAceBot.model.Expense;
import com.PlanAceBot.model.User;
import com.PlanAceBot.repository.ExpenseRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.sql.Timestamp;
import java.util.List;
import java.util.Optional;

@Service
@Transactional
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

    public void delete(Long id) {
        Optional<Expense> expenseOptional = expenseRepository.findById(id);
        if (expenseOptional.isPresent()) {
            expenseRepository.delete(expenseOptional.get());
        } else {
            throw new RuntimeException("Expense with id " + id + " not found.");
        }
    }

    public int countByUser(User user) {
        return expenseRepository.countByUser(user);
    }

    public List<Expense> findExpensesByUserAndDateBetween(User user, Timestamp startDate, Timestamp endDate) {
        return expenseRepository.findByUserAndDateBetween(user, startDate, endDate);
    }

    public List<Expense> findExpensesByUserChatId(Long chatId) {
        return expenseRepository.findByUserChatId(chatId);
    }

}
