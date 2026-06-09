package org.example.motoadvisor.agent;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.annotation.DirtiesContext;

import java.time.Duration;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@DirtiesContext(classMode = DirtiesContext.ClassMode.BEFORE_CLASS)
class AgentAclIntegrationTests {

    @Autowired
    private AgentBridge agentBridge;

    @Test
    void searchRequestReturnsAclEnvelopeWithPayload() {
        assertTrue(agentBridge.isReady(), "AgentBridge should be ready during integration test");

        AclEnvelope response = agentBridge.submitSearch(
                Map.of(
                        "experienceLevel", "BEGINNER",
                        "preferredCategory", "NAKED",
                        "minEngineSizeCc", 300,
                        "maxEngineSizeCc", 800,
                        "maxBudgetEur", 9000,
                        "preferredBrand", ""
                ),
                Duration.ofSeconds(10)
        );

        assertNotNull(response);
        assertNotNull(response.getRequestId());
        assertEquals("SEARCH_RESPONSE", response.getType());

        List<Map<String, Object>> rows = response.payloadList("recommendations");
        assertFalse(rows.isEmpty(), "Expected non-empty recommendations payload");

        Map<String, Object> first = rows.getFirst();
        assertTrue(first.containsKey("score"));
        assertTrue(first.containsKey("reason"));
    }
}

