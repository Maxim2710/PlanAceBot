package com.PlanAceBot.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.sql.Timestamp;
import java.time.LocalDateTime;

@Getter
@Setter
@Entity
@Table(name = "budgets")
public class Budget {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Long id;

    @ManyToOne
    @JoinColumn(name = "user_id")
    private User user;

    @Column(name = "name")
    private String name;

    @Column(name = "amount")
    private Double amount;

    @Column(name = "initial_amount")
    private Double initialAmount;

    @Column(name = "start_date")
    private Timestamp startDate;

    @Column(name = "end_date")
    private Timestamp endDate;

    @Column(name = "category")
    private String category;

    @Column(name = "warning_threshold")
    private Double warningThreshold;

    @Column(name = "notification_sent")
    private boolean notificationSent;

    @Column(name = "last_notification_sent_time")
    private LocalDateTime lastNotificationSentTime;
}