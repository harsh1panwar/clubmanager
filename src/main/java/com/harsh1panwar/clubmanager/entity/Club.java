package com.harsh1panwar.clubmanager.entity;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;
import java.util.List;

@Entity
@Table(name = "clubs")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Club {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String name;

    @Column(length = 500)
    private String description;

    // Club ka owner kaun hai — User table se relation
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "owner_id", nullable = false)
    private User owner;

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    // Ek club ke multiple events ho sakte hain
    @OneToMany(mappedBy = "club", cascade = CascadeType.ALL)
    private List<Event> events;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
    }
}