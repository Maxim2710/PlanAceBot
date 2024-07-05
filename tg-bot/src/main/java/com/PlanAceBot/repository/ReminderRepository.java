package com.PlanAceBot.repository;

import com.PlanAceBot.model.Reminder;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.sql.Timestamp;
import java.util.List;

@Repository
public interface ReminderRepository extends JpaRepository<Reminder, Long> {

    @Query("SELECT r FROM Reminder r WHERE r.reminderTime <= :currentTime")
    List<Reminder> findDueReminders(@Param("currentTime") Timestamp currentTime);

    List<Reminder> findByUser_ChatId(Long userId);
}

