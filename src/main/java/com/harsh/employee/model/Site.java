package com.harsh.employee.model;

import jakarta.persistence.*;
import lombok.Getter;

@Entity
@Table(name = "sites")
@Getter
public class Site {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "site_name", nullable = false, length = 150)
    private String siteName;

    @Column(nullable = false, length = 255)
    private String location;

    @Column(name = "is_active", nullable = false)
    private boolean isActive = true;

    // Getters, Setters, Constructors
}
