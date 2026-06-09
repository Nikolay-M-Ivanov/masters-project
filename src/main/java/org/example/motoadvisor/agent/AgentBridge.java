package org.example.motoadvisor.agent;

import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.motoadvisor.persistence.repository.AgentLogRepository;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.*;

/**
 * Spring-to-JADE bridge based on O2A objects.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class AgentBridge {

    private static volatile AgentBridge INSTANCE;

    private final JadeManager jadeManager;
    private final AgentLogRepository agentLogRepository;

    private final Map<String, CompletableFuture<AclEnvelope>> pending = new ConcurrentHashMap<>();

    @PostConstruct
    void init() {
        INSTANCE = this;
    }

    public boolean isReady() {
        return jadeManager.isStarted();
    }

    public AclEnvelope submitSearch(Map<String, Object> criteria) {
        return submitSearch(criteria, Duration.ofSeconds(12));
    }

    public AclEnvelope submitSearch(Map<String, Object> criteria, Duration timeout) {
        if (!isReady()) {
            return AclEnvelope.error("n/a", "SEARCH_RESPONSE", "JADE manager is not started");
        }

        String requestId = UUID.randomUUID().toString();
        AclEnvelope req = AclEnvelope.builder()
                .requestId(requestId)
                .type("SEARCH_REQUEST")
                .payload(Map.of("criteria", criteria))
                .build();

        CompletableFuture<AclEnvelope> future = new CompletableFuture<>();
        pending.put(requestId, future);

        agentLogRepository.saveAclLogRow(
                "Spring",
                AgentNames.USER_REQUEST_AGENT,
                "O2A",
                "SEARCH_REQUEST",
                requestId
        );

        try {
            jadeManager.sendO2A(AgentNames.USER_REQUEST_AGENT, req);
            AclEnvelope response = future.get(timeout.toMillis(), TimeUnit.MILLISECONDS);
            persistResponseLog(response);
            return response;
        } catch (TimeoutException e) {
            pending.remove(requestId);
            AclEnvelope timeoutResponse = AclEnvelope.error(requestId, "SEARCH_RESPONSE", "Timeout waiting for agent response");
            persistResponseLog(timeoutResponse);
            return timeoutResponse;
        } catch (Exception e) {
            pending.remove(requestId);
            AclEnvelope err = AclEnvelope.error(requestId, "SEARCH_RESPONSE", "Bridge error: " + e.getMessage());
            persistResponseLog(err);
            return err;
        }
    }

    public static void completeFromAgent(AclEnvelope envelope) {
        AgentBridge bridge = INSTANCE;
        if (bridge == null || envelope == null || envelope.getRequestId() == null) {
            return;
        }

        CompletableFuture<AclEnvelope> future = bridge.pending.remove(envelope.getRequestId());
        if (future != null) {
            future.complete(envelope);
        } else {
            log.debug("No pending future found for requestId={}", envelope.getRequestId());
        }
    }

    private void persistResponseLog(AclEnvelope response) {
        boolean failure = response.getErrors() != null && !response.getErrors().isEmpty();
        String summary = failure ? String.join("; ", response.getErrors()) : "SEARCH_RESPONSE";

        agentLogRepository.saveAclLogRow(
                AgentNames.USER_REQUEST_AGENT,
                "Spring",
                failure ? "FAILURE" : "INFORM",
                summary,
                response.getRequestId()
        );
    }
}
