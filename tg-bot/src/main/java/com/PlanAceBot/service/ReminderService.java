package com.PlanAceBot.service;

import com.PlanAceBot.model.Reminder;
import com.PlanAceBot.repository.ReminderRepository;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class ReminderService {

    @Autowired
    private ReminderRepository reminderRepository;

    public void save(Reminder reminder) {
        reminderRepository.save(reminder);
    }
}
