package com.example.shopnow.shared;

import java.util.UUID;

import jakarta.persistence.*;

@Entity
public abstract class BaseEntity {
    
    @Id
    @GeneratedValue
    private UUID id;

}
