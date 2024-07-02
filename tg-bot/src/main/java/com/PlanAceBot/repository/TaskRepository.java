package com.PlanAceBot.repository;

import com.PlanAceBot.model.Task;
import com.PlanAceBot.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface TaskRepository extends JpaRepository<Task, Long> {

    int countByUser(User user);

}
