package com.PlanAceBot.service;

import com.PlanAceBot.model.User;
import com.PlanAceBot.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class UserService {

    @Autowired
    private UserRepository userRepository;

    public void save(User user) {
        userRepository.save(user);
    }

    public boolean existByChatId(Long chatId) {
        return userRepository.existsByChatId(chatId);
    }

    public User findByChatId(Long chatId) {
        return userRepository.findByChatId(chatId);
    }

    public User getUserByChatId(String chatId) {
        return userRepository.findByChatId(Long.parseLong(chatId));
    }

    public String getUserTimezone(String chatId) {
        // Пример: Получение часового пояса из базы данных по chatId пользователя
        User user = userRepository.findByChatId(Long.parseLong(chatId));
        if (user != null) {
            return user.getTimezone(); // Предположим, что у пользователя есть поле timezone
        } else {
            // Если пользователя нет в базе данных или нет информации о часовом поясе
            return "UTC"; // Возвращаем UTC как значение по умолчанию
        }
    }
}