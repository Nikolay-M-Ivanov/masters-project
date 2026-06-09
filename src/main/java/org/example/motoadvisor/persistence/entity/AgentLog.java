package org.example.motoadvisor.persistence.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * Stores JADE ACL message logs exchanged between agents.
 */
@Entity
@Table(name = "agent_logs")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AgentLog {

    private static final DateTimeFormatter TS_FORMAT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String sender;

    @Column(nullable = false)
    private String receiver;

    @Column(nullable = false)
    private String performative;

    @Column(columnDefinition = "TEXT")
    private String content;

    @Column(name = "conversation_id")
    private String conversationId;

    @Column(name = "created_at", nullable = false)
    private String createdAt;

    @PrePersist
    void prePersist() {
        if (createdAt == null) {
            createdAt = LocalDateTime.now().format(TS_FORMAT);
        }
    }
}

