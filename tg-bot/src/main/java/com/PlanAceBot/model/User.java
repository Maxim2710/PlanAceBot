package com.PlanAceBot.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.sql.Timestamp;

@Getter
@Setter
@Entity
@Table(name = "users")
@NoArgsConstructor
public class User {

    @Id
    @Column(name = "chatId")
    private Long chatId;

    @Column(name = "firstName")
    private String firstName;

    @Column(name = "firstName")
    private String lastName;

    @Column(name = "lastName")
    private String username;

    @Column(name = "registeredAt")
    private Timestamp registeredAt;
}
