/**
 * JADE multi-agent infrastructure.
 *
 * <p>Core components:
 * <ul>
 *   <li>{@code JadeManager} - starts container and registers agents</li>
 *   <li>{@code AgentBridge} - Spring-to-JADE O2A bridge</li>
 *   <li>{@code UserRequestAgent} - receives O2A requests and initiates ACL flow</li>
 *   <li>{@code RecommendationAgent} - computes recommendations and orchestrates enrichments</li>
 *   <li>{@code SpecAgent} - enriches recommendation rows with spec metadata</li>
 *   <li>{@code AclEnvelope} - standard JSON contract: requestId/type/payload/errors</li>
 * </ul>
 */
package org.example.motoadvisor.agent;
