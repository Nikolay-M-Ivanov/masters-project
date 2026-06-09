package org.example.motoadvisor.agent.agents;

import jade.core.Agent;
import jade.core.behaviours.CyclicBehaviour;
import jade.lang.acl.ACLMessage;
import jade.lang.acl.MessageTemplate;
import lombok.extern.slf4j.Slf4j;
import org.example.motoadvisor.agent.AclEnvelope;

import java.util.List;
import java.util.Map;

/**
 * Enrichment agent for recommendation specs.
 * Adds derived specification metadata to recommendation rows.
 */
@Slf4j
public class SpecAgent extends Agent {

    @Override
    protected void setup() {
        addBehaviour(new CyclicBehaviour() {
            @Override
            public void action() {
                ACLMessage in = receive(MessageTemplate.MatchPerformative(ACLMessage.REQUEST));
                if (in == null) {
                    block(150);
                    return;
                }

                AclEnvelope req;
                try {
                    req = AclEnvelope.fromJson(in.getContent());
                } catch (Exception e) {
                    replyError(in, "SPEC_ENRICH_RESPONSE", "Invalid request JSON");
                    return;
                }

                if (!"SPEC_ENRICH_REQUEST".equals(req.getType())) {
                    replyError(in, "SPEC_ENRICH_RESPONSE", "Unsupported type: " + req.getType());
                    return;
                }

                List<Map<String, Object>> rows = req.payloadList("recommendations");
                List<Map<String, Object>> enriched = rows.stream().map(SpecAgent.this::enrich).toList();

                AclEnvelope resp = AclEnvelope.ok(
                        req.getRequestId(),
                        "SPEC_ENRICH_RESPONSE",
                        Map.of("recommendations", enriched)
                );

                ACLMessage out = in.createReply();
                out.setPerformative(ACLMessage.INFORM);
                out.setContent(resp.toJson());
                send(out);
            }
        });

        log.info("[JADE] SpecAgent started");
    }

    private Map<String, Object> enrich(Map<String, Object> row) {
        int engine = toInt(row.get("engineSizeCc"));
        String engineClass = engine >= 900 ? "LARGE" : (engine >= 600 ? "MID" : "SMALL");
        row.put("engineClass", engineClass);
        row.put("availability", "IN_STOCK");
        return row;
    }

    private int toInt(Object value) {
        if (value instanceof Number n) {
            return n.intValue();
        }
        try {
            return Integer.parseInt(String.valueOf(value));
        } catch (Exception e) {
            return 0;
        }
    }

    private void replyError(ACLMessage in, String type, String msg) {
        ACLMessage out = in.createReply();
        out.setPerformative(ACLMessage.FAILURE);
        out.setContent(AclEnvelope.error(in.getConversationId(), type, msg).toJson());
        send(out);
    }
}
