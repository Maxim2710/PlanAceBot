package com.PlanAceBot.repository;

import com.PlanAceBot.model.Income;
import com.PlanAceBot.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface IncomeRepository extends JpaRepository<Income, Long> {

    List<Income> findByUser_ChatId(Long chatId);

    int countByUser(User user);
}
