package org.example.motoadvisor.persistence.repository;

import org.example.motoadvisor.persistence.entity.AgentLog;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface AgentLogRepository extends JpaRepository<AgentLog, Long> {

    List<AgentLog> findTop100ByOrderByCreatedAtDesc();

    List<AgentLog> findByConversationIdOrderByCreatedAtAsc(String conversationId);

    /**
     * Helper method for storing one ACL log row.
     */
    default AgentLog saveAclLogRow(
            String sender,
            String receiver,
            String performative,
            String content,
            String conversationId
    ) {
        AgentLog row = AgentLog.builder()
                .sender(sender)
                .receiver(receiver)
                .performative(performative)
                .content(content)
                .conversationId(conversationId)
                .build();
        return save(row);
    }
}

