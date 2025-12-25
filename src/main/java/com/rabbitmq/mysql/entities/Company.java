package com.rabbitmq.mysql.entities;

import jakarta.persistence.*;

@Entity
public class Company {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "`Id`")
    private Long id;

    private String serial;

    public Long getId() {
        return id;
    }

    public String getSerial() {
        return serial;
    }
}
