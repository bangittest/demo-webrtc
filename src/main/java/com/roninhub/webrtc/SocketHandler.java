package com.roninhub.webrtc;

import org.springframework.stereotype.Component;
import org.springframework.web.socket.*;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.JsonNode;

import java.util.HashMap;
import java.util.concurrent.ConcurrentHashMap;
import java.util.Map;
import java.io.IOException;

@Component
public class SocketHandler  implements WebSocketHandler {
    private final Map<String, WebSocketSession> streamers = new ConcurrentHashMap<>();
    private final Map<String, WebSocketSession> viewers = new ConcurrentHashMap<>();
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Override
    public void afterConnectionEstablished(WebSocketSession session) throws Exception {
        System.out.println("📡 New WebSocket connection: " + session.getId());
    }

    @Override
    public void handleMessage(WebSocketSession session, WebSocketMessage<?> message) throws Exception {
        String payload = message.getPayload().toString();
        System.out.println("📨 Received from " + session.getId() + ": " + payload.substring(0, Math.min(100, payload.length())));

        try {
            JsonNode json = objectMapper.readTree(payload);
            String event = json.get("event").asText();
            String role = json.get("role").asText();
            JsonNode data = json.get("data");

            switch (event) {
                case "join":
                    handleJoin(session, role);
                    break;
                case "offer":
                    handleOffer(session, data);
                    break;
                case "answer":
                    handleAnswer(session, data);
                    break;
                case "candidate":
                    handleCandidate(session, data, role);
                    break;
                case "request_offer":
                    for (WebSocketSession streamer : streamers.values()) {
                        if (streamer.isOpen()) {
                            // Gửi 1 message yêu cầu streamer tạo offer gửi lại
                            streamer.sendMessage(new TextMessage(
                                    objectMapper.writeValueAsString(Map.of(
                                            "event", "viewer_request",
                                            "data", Map.of("viewerId", session.getId())
                                    ))
                            ));
                        }
                    }
                    break;
                default:
                    System.out.println("❌ Unknown event: " + event);
            }
        } catch (Exception e) {
            System.err.println("❌ Error handling message: " + e.getMessage());
        }
    }

    private void handleJoin(WebSocketSession session, String role) throws IOException {
        System.out.println("👤 User joined as: " + role);

        if ("streamer".equals(role)) {
            streamers.put(session.getId(), session);
            System.out.println("📺 Streamer connected. Total streamers: " + streamers.size());
        } else if ("viewer".equals(role)) {
            viewers.put(session.getId(), session);
            System.out.println("👁️ Viewer connected. Total viewers: " + viewers.size());

            // YÊU CẦU STREAMER PHÁT OFFER
            for (WebSocketSession streamer : streamers.values()) {
                if (streamer.isOpen()) {
                    Map<String, Object> req = new HashMap<>();
                    req.put("event", "request-offer");
                    req.put("viewerId", session.getId()); // Optionally identify viewer
                    String message = objectMapper.writeValueAsString(req);
                    streamer.sendMessage(new TextMessage(message));
                    System.out.println("📤 Sent request-offer to streamer: " + streamer.getId());
                }
            }
        }
    }

    private void handleOffer(WebSocketSession session, JsonNode offer) throws IOException {
        System.out.println("📤 Relaying offer from streamer");

        String message = createMessage("offer", offer);

        if (offer.has("targetViewer")) {
            String viewerId = offer.get("targetViewer").asText();
            WebSocketSession viewer = viewers.get(viewerId);
            if (viewer != null && viewer.isOpen()) {
                viewer.sendMessage(new TextMessage(message));
                System.out.println("✅ Offer sent to viewer: " + viewerId);
            }
        } else {
            for (WebSocketSession viewer : viewers.values()) {
                if (viewer.isOpen()) {
                    viewer.sendMessage(new TextMessage(message));
                    System.out.println("✅ Offer sent to viewer: " + viewer.getId());
                }
            }
        }
    }


    private void handleAnswer(WebSocketSession session, JsonNode answer) throws IOException {
        System.out.println("📤 Relaying answer from viewer to streamers");

        String message = createMessage("answer", answer);

        // Gửi answer tới tất cả streamers
        for (WebSocketSession streamer : streamers.values()) {
            if (streamer.isOpen()) {
                try {
                    streamer.sendMessage(new TextMessage(message));
                    System.out.println("✅ Answer sent to streamer: " + streamer.getId());
                } catch (Exception e) {
                    System.err.println("❌ Failed to send answer to streamer: " + e.getMessage());
                }
            }
        }
    }

    private void handleCandidate(WebSocketSession session, JsonNode candidate, String role) throws IOException {
        System.out.println("🧊 Relaying ICE candidate from " + role);

        String message = createMessage("candidate", candidate);

        if ("streamer".equals(role)) {
            // Streamer gửi candidate tới viewers
            System.out.println("📤 Sending candidate from streamer to " + viewers.size() + " viewers");
            for (WebSocketSession viewer : viewers.values()) {
                if (viewer.isOpen()) {
                    try {
                        viewer.sendMessage(new TextMessage(message));
                        System.out.println("✅ Candidate sent to viewer: " + viewer.getId());
                    } catch (Exception e) {
                        System.err.println("❌ Failed to send candidate to viewer: " + e.getMessage());
                    }
                }
            }
        } else if ("viewer".equals(role)) {
            // Viewer gửi candidate tới streamers
            System.out.println("📤 Sending candidate from viewer to " + streamers.size() + " streamers");
            for (WebSocketSession streamer : streamers.values()) {
                if (streamer.isOpen()) {
                    try {
                        streamer.sendMessage(new TextMessage(message));
                        System.out.println("✅ Candidate sent to streamer: " + streamer.getId());
                    } catch (Exception e) {
                        System.err.println("❌ Failed to send candidate to streamer: " + e.getMessage());
                    }
                }
            }
        }
    }

    private String createMessage(String event, JsonNode data) throws IOException {
        Map<String, Object> message = new HashMap<>();
        message.put("event", event);
        message.put("data", data);
        return objectMapper.writeValueAsString(message);
    }

    @Override
    public void handleTransportError(WebSocketSession session, Throwable exception) throws Exception {
        System.err.println("❌ Transport error for session " + session.getId() + ": " + exception.getMessage());
        removeSession(session);
    }

    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus closeStatus) throws Exception {
        System.out.println("🔌 Connection closed: " + session.getId() + " - " + closeStatus);
        removeSession(session);
    }

    private void removeSession(WebSocketSession session) {
        streamers.remove(session.getId());
        viewers.remove(session.getId());
        System.out.println("🗑️ Session removed. Streamers: " + streamers.size() + ", Viewers: " + viewers.size());
    }

    @Override
    public boolean supportsPartialMessages() {
        return false;
    }
}