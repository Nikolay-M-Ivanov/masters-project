package org.example.motoadvisor.agent.agents;

import jade.core.AID;
import jade.core.Agent;
import jade.core.behaviours.CyclicBehaviour;
import jade.lang.acl.ACLMessage;
import jade.lang.acl.MessageTemplate;
import lombok.extern.slf4j.Slf4j;
import org.example.motoadvisor.agent.AclEnvelope;
import org.example.motoadvisor.agent.AgentBridge;
import org.example.motoadvisor.agent.AgentNames;

/**
 * Entry point agent for Spring requests (received via O2A).
 *
 * <p>Chain:
 * Spring -> UserRequestAgent -> RecommendationAgent -> UserRequestAgent -> Spring
 */
@Slf4j
public class UserRequestAgent extends Agent {

    @Override
    protected void setup() {
        setEnabledO2ACommunication(true, 0);

        addBehaviour(new CyclicBehaviour() {
            @Override
            public void action() {
                Object o2a = getO2AObject();
                if (o2a == null) {
                    block(150);
                    return;
                }

                AclEnvelope request = coerceEnvelope(o2a);
                if (request == null) {
                    return;
                }

                try {
                    ACLMessage msg = new ACLMessage(ACLMessage.REQUEST);
                    msg.addReceiver(new AID(AgentNames.RECOMMENDATION_AGENT, AID.ISLOCALNAME));
                    msg.setConversationId(request.getRequestId());
                    msg.setContent(request.toJson());
                    send(msg);

                    MessageTemplate mt = MessageTemplate.MatchConversationId(request.getRequestId());
                    ACLMessage reply = blockingReceive(mt, 9000);

                    AclEnvelope response;
                    if (reply == null) {
                        response = AclEnvelope.error(
                                request.getRequestId(),
                                "SEARCH_RESPONSE",
                                "RecommendationAgent timeout"
                        );
                    } else {
                        response = AclEnvelope.fromJson(reply.getContent());
                    }

                    AgentBridge.completeFromAgent(response);
                } catch (Exception e) {
                    AgentBridge.completeFromAgent(
                            AclEnvelope.error(request.getRequestId(), "SEARCH_RESPONSE", e.getMessage())
                    );
                }
            }
        });

        log.info("[JADE] UserRequestAgent started");
    }

    private AclEnvelope coerceEnvelope(Object obj) {
        try {
            if (obj instanceof AclEnvelope env) {
                return env;
            }
            if (obj instanceof String json) {
                return AclEnvelope.fromJson(json);
            }
            return null;
        } catch (Exception ex) {
            return null;
        }
    }
}

