package com.PlanAceBot.repository;

import com.PlanAceBot.model.Expense;
import com.PlanAceBot.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ExpenseRepository extends JpaRepository<Expense, Long> {

    List<Expense> findByUser_ChatId(Long userId);

    int countByUser(User user);
}
