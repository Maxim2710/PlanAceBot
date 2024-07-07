package com.PlanAceBot.service;

import com.PlanAceBot.model.Income;
import com.PlanAceBot.repository.IncomeRepository;
import jakarta.transaction.Transactional;
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

    @Transactional
    public void delete(Integer incomeId) {
        if (incomeRepository.existsById(Long.valueOf(incomeId))) {
            incomeRepository.deleteById(Long.valueOf(incomeId));
        } else {
            throw new IllegalArgumentException("Income with id " + incomeId + " does not exist.");
        }
    }

}
