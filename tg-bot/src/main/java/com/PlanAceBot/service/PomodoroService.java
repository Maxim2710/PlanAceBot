package com.PlanAceBot.service;

import com.PlanAceBot.model.Pomodoro;
import com.PlanAceBot.model.User;
import com.PlanAceBot.repository.PomodoroRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class PomodoroService {

    @Autowired
    private PomodoroRepository pomodoroRepository;

    public void savePomodoroSession(Pomodoro pomodoro) {
        pomodoroRepository.save(pomodoro);
    }

    public Pomodoro getActivePomodoroSessionByChatId(String chatId) {
        return pomodoroRepository.findByUser_ChatIdAndSessionActiveTrue(Long.parseLong(chatId));
    }

    public List<Pomodoro> getAllPomodoroSessions() {
        return pomodoroRepository.findAll();
    }

    public void deletePomodoroSession(Pomodoro pomodoro) {
        pomodoroRepository.delete(pomodoro);
    }

    public Pomodoro getActivePomodoroSessionByUserId(User curUser) {
        return pomodoroRepository.findFirstByUserAndSessionActiveTrue(curUser);
    }


}
