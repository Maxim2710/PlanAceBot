package com.PlanAceBot.repository;

import com.PlanAceBot.model.NinetyThirty;
import com.PlanAceBot.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface NinetyThirtyRepository extends JpaRepository<NinetyThirty, Long> {
    NinetyThirty findByUserChatIdAndSessionActive(Long user_chatId, boolean sessionActive);
    NinetyThirty findByUserAndSessionActive(User user, boolean sessionActive);
}
