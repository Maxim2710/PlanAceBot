package com.PlanAceBot.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.sql.Timestamp;
import java.time.LocalDateTime;

@Getter
@Setter
@Entity
@Table(name = "tasks")
public class Task {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Long id;

    @ManyToOne
    @JoinColumn(name = "user_chat_id")
    private User user;

    @Column(name = "title")
    private String title;

    @Column(name = "description")
    private String description;

    @Column(name = "completed")
    private boolean completed;

    @Column(name = "creation_timestamp")
    private Timestamp creationTimestamp;

    @Column(name = "priority")
    private Integer priority;

    @Column(name = "deadline")
    private LocalDateTime deadline;

    @Column(name = "notified_three_days")
    private boolean notifiedThreeDays;

    @Column(name = "notified_one_day")
    private boolean notifiedOneDay;
}
