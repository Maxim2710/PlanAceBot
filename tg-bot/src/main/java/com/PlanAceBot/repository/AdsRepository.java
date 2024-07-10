package com.PlanAceBot.repository;

import com.PlanAceBot.model.Ads;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface AdsRepository extends JpaRepository<Ads, Long> {
    void deleteById(Long id);
}
