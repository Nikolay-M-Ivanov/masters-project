package org.example.motoadvisor.dto;

import lombok.*;

import java.time.LocalDateTime;

/**
 * DTO used to display one JADE-like message in the UI.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AgentMessage {

    private String sender;
    private String receiver;
    private String performative;
    private String content;
    private String conversationId;
    private LocalDateTime createdAt;
}

