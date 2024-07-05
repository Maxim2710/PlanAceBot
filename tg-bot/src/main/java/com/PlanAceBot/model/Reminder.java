package com.PlanAceBot.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.sql.Timestamp;

@Getter
@Setter
@Entity
@Table(name = "reminders")
@NoArgsConstructor
public class Reminder {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Long id;

    @ManyToOne
    @JoinColumn(name = "user_chat_id", referencedColumnName = "chat_id")
    private User user;

    @Column(name = "reminder_time")
    private Timestamp reminderTime;

    @Column(name = "message")
    private String message;
}
