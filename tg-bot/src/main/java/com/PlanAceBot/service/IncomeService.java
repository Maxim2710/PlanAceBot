package com.PlanAceBot.service;

import com.PlanAceBot.model.Income;
import com.PlanAceBot.repository.IncomeRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class IncomeService {

    @Autowired
    private IncomeRepository incomeRepository;

    public void save(Income income) {
        incomeRepository.save(income);
    }

    public Income findById(Long id) {
        return incomeRepository.findById(id).orElse(null);
    }

    public List<Income> findIncomesByUserId(Long userId) {
        return incomeRepository.findByUser_ChatId(userId);
    }

}
