package org.example.motoadvisor.agent;

import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import jade.core.Profile;
import jade.core.ProfileImpl;
import jade.core.Runtime;
import jade.wrapper.AgentContainer;
import jade.wrapper.AgentController;
import jade.wrapper.StaleProxyException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.motoadvisor.agent.agents.RecommendationAgent;
import org.example.motoadvisor.agent.agents.UserRequestAgent;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.net.ServerSocket;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Boots JADE main container and registers core agents on app startup.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class JadeManager {

    @Value("${app.jade.enabled:true}")
    private boolean jadeEnabled;

    @Value("${app.jade.host:localhost}")
    private String host;

    @Value("${app.jade.port:1099}")
    private int port;

    @Value("${app.jade.platform-id:moto-advisor-platform}")
    private String platformId;

    private final AtomicBoolean started = new AtomicBoolean(false);
    private final Map<String, AgentController> agents = new ConcurrentHashMap<>();

    private Runtime runtime;
    private AgentContainer mainContainer;
    private int boundPort;

    @PostConstruct
    public void start() {
        if (!jadeEnabled) {
            log.info("[JADE] Disabled by config app.jade.enabled=false");
            return;
        }

        try {
            runtime = Runtime.instance();
            boundPort = resolveAvailablePort(port);

            ProfileImpl profile = new ProfileImpl(host, boundPort, platformId);
            profile.setParameter(Profile.MAIN, "true");
            profile.setParameter(Profile.GUI, "false");

            mainContainer = runtime.createMainContainer(profile);

            register(AgentNames.USER_REQUEST_AGENT, UserRequestAgent.class, new Object[]{});
            register(AgentNames.RECOMMENDATION_AGENT, RecommendationAgent.class, new Object[]{});

            started.set(true);
            log.info("[JADE] Started platform='{}' host='{}' port={} with {} agents",
                    platformId, host, boundPort, agents.size());
        } catch (Exception e) {
            log.error("[JADE] Failed to start container: {}", e.getMessage(), e);
            started.set(false);
        }
    }

    public boolean isStarted() {
        return started.get();
    }

    public void sendO2A(String agentName, Object payload) {
        AgentController controller = agents.get(agentName);
        if (controller == null) {
            throw new IllegalStateException("Agent not registered: " + agentName);
        }
        try {
            controller.putO2AObject(payload, false);
        } catch (StaleProxyException e) {
            throw new IllegalStateException("Cannot deliver O2A message to " + agentName, e);
        }
    }

    @PreDestroy
    public void stop() {
        if (!started.get()) {
            return;
        }

        agents.values().forEach(agent -> {
            try {
                agent.kill();
            } catch (Exception ignored) {
            }
        });
        agents.clear();

        try {
            if (mainContainer != null) {
                mainContainer.kill();
            }
        } catch (Exception ignored) {
        }

        if (runtime != null) {
            runtime.shutDown();
        }

        started.set(false);
        log.info("[JADE] Shutdown complete");
    }

    private void register(String name, Class<?> clazz, Object[] args) throws StaleProxyException {
        AgentController controller = mainContainer.createNewAgent(name, clazz.getName(), args);
        controller.start();
        agents.put(name, controller);
        log.info("[JADE] Registered agent '{}' as {}", name, clazz.getSimpleName());
    }

    private int resolveAvailablePort(int preferredPort) {
        try (ServerSocket ignored = new ServerSocket(preferredPort)) {
            return preferredPort;
        } catch (Exception e) {
            try (ServerSocket fallback = new ServerSocket(0)) {
                int freePort = fallback.getLocalPort();
                log.warn("[JADE] Port {} is busy. Falling back to free port {}.", preferredPort, freePort);
                return freePort;
            } catch (Exception inner) {
                throw new IllegalStateException("Cannot allocate JADE port", inner);
            }
        }
    }
}
