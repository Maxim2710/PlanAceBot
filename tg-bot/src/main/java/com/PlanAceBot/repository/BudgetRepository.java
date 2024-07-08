package com.PlanAceBot.repository;

import com.PlanAceBot.model.Budget;
import com.PlanAceBot.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;


@Repository
public interface BudgetRepository extends JpaRepository<Budget, Long> {
    Budget findByUserChatId(Long chatId);
}