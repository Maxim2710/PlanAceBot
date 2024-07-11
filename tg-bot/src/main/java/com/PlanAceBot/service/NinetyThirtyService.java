package com.PlanAceBot.service;

import com.PlanAceBot.model.NinetyThirty;
import com.PlanAceBot.model.User;
import com.PlanAceBot.repository.NinetyThirtyRepository;
import com.PlanAceBot.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class NinetyThirtyService {

    @Autowired
    private NinetyThirtyRepository ninetyThirtyRepository;

    @Autowired
    private UserService userService;

    public NinetyThirty getNinetyThirtySessionByChatId(String chatId) {
        User user = userService.getUserByChatId(chatId);
        return ninetyThirtyRepository.findTopByUserOrderByStartTimeDesc(user);
    }

    public NinetyThirty getActiveNinetyThirtySessionByUserId(User user) {
        return ninetyThirtyRepository.findByUserAndSessionActive(user, true);
    }

    public List<NinetyThirty> getAllNinetyThirtySessions() {
        return ninetyThirtyRepository.findAll();
    }

    public void saveNinetyThirtySession(NinetyThirty ninetyThirty) {
        ninetyThirtyRepository.save(ninetyThirty);
    }

    public void deleteNinetyThirtySession(NinetyThirty ninetyThirty) {
        ninetyThirtyRepository.delete(ninetyThirty);
    }
}
