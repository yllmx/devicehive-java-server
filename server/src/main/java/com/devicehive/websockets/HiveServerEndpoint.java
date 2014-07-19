package com.devicehive.websockets;


import com.devicehive.messages.subscriptions.SubscriptionManager;
import com.devicehive.util.LogExecutionTime;
import com.devicehive.util.ThreadLocalVariablesKeeper;
import com.devicehive.websockets.converters.JsonEncoder;
import com.devicehive.websockets.converters.JsonMessageBuilder;
import com.devicehive.websockets.handlers.WebsocketExecutor;
import com.devicehive.websockets.util.SessionMonitor;
import com.google.common.net.HttpHeaders;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import com.google.gson.JsonParser;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import javax.inject.Singleton;
import javax.servlet.http.HttpServletResponse;
import javax.websocket.*;
import javax.websocket.server.HandshakeRequest;
import javax.websocket.server.PathParam;
import javax.websocket.server.ServerEndpoint;
import javax.websocket.server.ServerEndpointConfig;
import java.io.Reader;
import java.util.UUID;

@ServerEndpoint(value = "/websocket/{endpoint}", encoders = {JsonEncoder.class})
@LogExecutionTime
public class HiveServerEndpoint {

    protected static final long MAX_MESSAGE_SIZE = 1024 * 1024;

    private static final Logger logger = LoggerFactory.getLogger(HiveServerEndpoint.class);

    @Inject
    private SessionMonitor sessionMonitor;

    @Inject
    private SubscriptionManager subscriptionManager;

    @Inject
    private WebsocketExecutor executor;


    @OnOpen
    public void onOpen(Session session, @PathParam("endpoint") String endpoint) {
        logger.debug("[onOpen] session id {} ", session.getId());
        session.getUserProperties().put(HiveWebsocketSessionState.KEY, new HiveWebsocketSessionState());
        sessionMonitor.registerSession(session);
    }

    @OnMessage(maxMessageSize = MAX_MESSAGE_SIZE)
    public JsonObject onMessage(Reader reader, Session session) {
        try {
            logger.debug("[onMessage] session id {} ", session.getId());
            JsonObject request = new JsonParser().parse(reader).getAsJsonObject();
            logger.debug("[onMessage] request is parsed correctly");
            return executor.execute(request, session);
        } catch (JsonParseException ex) {
            logger.error("[onMessage] Incorrect message syntax ", ex);
            return JsonMessageBuilder
                    .createErrorResponseBuilder(HttpServletResponse.SC_BAD_REQUEST, "Incorrect JSON syntax")
                    .build();
        } catch (Exception ex) {
            return JsonMessageBuilder
                    .createErrorResponseBuilder(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "Internal server error")
                    .build();
        }
    }

    @OnClose
    public void onClose(Session session, CloseReason closeReason) {
        logger.debug("[onClose] session id {}, close reason is {} ", session.getId(), closeReason);
        HiveWebsocketSessionState state = HiveWebsocketSessionState.get(session);
        for (UUID subId : state.getCommandSubscriptions()) {
            subscriptionManager.getCommandSubscriptionStorage().removeBySubscriptionId(subId);
        }
        for (UUID subId : state.getNotificationSubscriptions()) {
            subscriptionManager.getNotificationSubscriptionStorage().removeBySubscriptionId(subId);
        }
    }

    @OnError
    public void onError(Throwable exception, Session session) {
        logger.error("[onError] ", exception);
    }


}
