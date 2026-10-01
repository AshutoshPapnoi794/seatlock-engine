package com.example.seatlock.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.ArrayList;
import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "venues")
public class Venue {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false,length = 150)
    private String name;

    @Column(nullable = false,length = 150)
    private String city;

    @Column(nullable = false)
    private Integer TotalCapacity;

    @OneToMany(mappedBy = "venue", cascade = CascadeType.ALL,orphanRemoval = true)
    private List<ShowEvent> shows = new ArrayList<>();
}
