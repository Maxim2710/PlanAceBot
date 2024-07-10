package com.PlanAceBot.service;

import com.PlanAceBot.model.NinetyThirty;
import com.PlanAceBot.model.User;
import com.PlanAceBot.repository.NinetyThirtyRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class NinetyThirtyService {

    @Autowired
    private NinetyThirtyRepository ninetyThirtyRepository;

    public NinetyThirty getActiveNinetyThirtySessionByChatId(String chatId) {
        return ninetyThirtyRepository.findByUserChatIdAndSessionActive(Long.valueOf(chatId), true);
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
