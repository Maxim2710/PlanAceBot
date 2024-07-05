package com.PlanAceBot.service;

import com.PlanAceBot.model.Reminder;
import com.PlanAceBot.repository.ReminderRepository;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.sql.Timestamp;
import java.util.List;
import java.util.Optional;

@Service
public class ReminderService {

    @Autowired
    private ReminderRepository reminderRepository;

    public void save(Reminder reminder) {
        reminderRepository.save(reminder);
    }

    public List<Reminder> findDueReminders() {
        return reminderRepository.findDueReminders(new Timestamp(System.currentTimeMillis()));
    }

    public Optional<Reminder> findReminderById(int reminderId) {
        return Optional.ofNullable(reminderRepository.findById((long) reminderId).orElse(null));
    }

    public void deleteById(int reminderId) {
        reminderRepository.deleteById((long) reminderId);
    }
}