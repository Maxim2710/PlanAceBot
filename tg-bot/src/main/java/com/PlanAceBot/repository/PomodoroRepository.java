package com.PlanAceBot.repository;

import com.PlanAceBot.model.Pomodoro;
import com.PlanAceBot.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface PomodoroRepository extends JpaRepository<Pomodoro, Long> {
    Pomodoro findByUser_ChatIdAndSessionActiveTrue(Long chatId);

    Pomodoro findFirstByUserAndSessionActiveTrue(User user);
}

