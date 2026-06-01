package com.wd.custapi.service;

import com.google.firebase.FirebaseApp;
import com.google.firebase.messaging.BatchResponse;
import com.google.firebase.messaging.FirebaseMessaging;
import com.google.firebase.messaging.Message;
import com.google.firebase.messaging.MulticastMessage;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Unit tests for {@link PushNotificationService}.
 *
 * <p>The service has no injectable dependencies — it talks to Firebase entirely
 * through the static facades {@link FirebaseApp#getApps()} and
 * {@link FirebaseMessaging#getInstance()}. We therefore drive the branches with
 * Mockito {@code mockStatic} (the inline mock-maker is the default under
 * Spring Boot 3.5 / Mockito 5, so no extra dependency is required).
 *
 * <p>The {@code Message} / {@code MulticastMessage} / {@code Notification}
 * builders are plain (non-static) APIs and are exercised for real.
 */
class PushNotificationServiceTest {

    private final PushNotificationService service = new PushNotificationService();

    // Makes FirebaseApp.getApps() report an initialised app so isFirebaseReady() == true.
    private MockedStatic<FirebaseApp> firebaseReady(MockedStatic<FirebaseApp> appStatic) {
        appStatic.when(FirebaseApp::getApps).thenReturn(List.of(mock(FirebaseApp.class)));
        return appStatic;
    }

    // ─── sendToToken ────────────────────────────────────────────────────────────

    @Test
    void sendToToken_noOp_whenFirebaseNotInitialised() {
        try (MockedStatic<FirebaseApp> appStatic = mockStatic(FirebaseApp.class);
             MockedStatic<FirebaseMessaging> msgStatic = mockStatic(FirebaseMessaging.class)) {
            // No initialised apps -> isFirebaseReady() == false
            appStatic.when(FirebaseApp::getApps).thenReturn(List.of());

            service.sendToToken("token", "Title", "Body", Map.of("k", "v"));

            // FirebaseMessaging is never even resolved when not ready
            msgStatic.verifyNoInteractions();
        }
    }

    @Test
    void sendToToken_noOp_whenTokenNull() {
        try (MockedStatic<FirebaseApp> appStatic = mockStatic(FirebaseApp.class);
             MockedStatic<FirebaseMessaging> msgStatic = mockStatic(FirebaseMessaging.class)) {
            firebaseReady(appStatic);

            service.sendToToken(null, "Title", "Body", Map.of());

            msgStatic.verifyNoInteractions();
        }
    }

    @Test
    void sendToToken_noOp_whenTokenBlank() {
        try (MockedStatic<FirebaseApp> appStatic = mockStatic(FirebaseApp.class);
             MockedStatic<FirebaseMessaging> msgStatic = mockStatic(FirebaseMessaging.class)) {
            firebaseReady(appStatic);

            service.sendToToken("   ", "Title", "Body", Map.of());

            msgStatic.verifyNoInteractions();
        }
    }

    @Test
    void sendToToken_sendsMessage_whenReadyWithData() throws Exception {
        FirebaseMessaging messaging = mock(FirebaseMessaging.class);
        when(messaging.send(any(Message.class))).thenReturn("msg-id-1");

        try (MockedStatic<FirebaseApp> appStatic = mockStatic(FirebaseApp.class);
             MockedStatic<FirebaseMessaging> msgStatic = mockStatic(FirebaseMessaging.class)) {
            firebaseReady(appStatic);
            msgStatic.when(FirebaseMessaging::getInstance).thenReturn(messaging);

            service.sendToToken("token-1", "Hello", "World", Map.of("type", "PAYMENT"));

            verify(messaging).send(any(Message.class));
        }
    }

    @Test
    void sendToToken_sendsMessage_whenReadyWithNullData() throws Exception {
        FirebaseMessaging messaging = mock(FirebaseMessaging.class);
        when(messaging.send(any(Message.class))).thenReturn("msg-id-2");

        try (MockedStatic<FirebaseApp> appStatic = mockStatic(FirebaseApp.class);
             MockedStatic<FirebaseMessaging> msgStatic = mockStatic(FirebaseMessaging.class)) {
            firebaseReady(appStatic);
            msgStatic.when(FirebaseMessaging::getInstance).thenReturn(messaging);

            // null data -> putAllData branch skipped
            service.sendToToken("token-2", "Hi", "There", null);

            verify(messaging).send(any(Message.class));
        }
    }

    @Test
    void sendToToken_sendsMessage_whenReadyWithEmptyData() throws Exception {
        FirebaseMessaging messaging = mock(FirebaseMessaging.class);
        when(messaging.send(any(Message.class))).thenReturn("msg-id-3");

        try (MockedStatic<FirebaseApp> appStatic = mockStatic(FirebaseApp.class);
             MockedStatic<FirebaseMessaging> msgStatic = mockStatic(FirebaseMessaging.class)) {
            firebaseReady(appStatic);
            msgStatic.when(FirebaseMessaging::getInstance).thenReturn(messaging);

            // empty data -> putAllData branch skipped
            service.sendToToken("token-3", "Hi", "There", Map.of());

            verify(messaging).send(any(Message.class));
        }
    }

    @Test
    void sendToToken_swallowsException_whenSendThrows() throws Exception {
        FirebaseMessaging messaging = mock(FirebaseMessaging.class);
        // Generic exception exercises the catch(Exception) arm; must NOT propagate
        when(messaging.send(any(Message.class))).thenThrow(new RuntimeException("network down"));

        try (MockedStatic<FirebaseApp> appStatic = mockStatic(FirebaseApp.class);
             MockedStatic<FirebaseMessaging> msgStatic = mockStatic(FirebaseMessaging.class)) {
            firebaseReady(appStatic);
            msgStatic.when(FirebaseMessaging::getInstance).thenReturn(messaging);

            // Fire-and-forget: no exception escapes
            service.sendToToken("token-err", "T", "B", Map.of("a", "b"));

            verify(messaging).send(any(Message.class));
        }
    }

    @Test
    void sendToToken_swallowsException_whenFirebaseReadyCheckThrows() {
        try (MockedStatic<FirebaseApp> appStatic = mockStatic(FirebaseApp.class);
             MockedStatic<FirebaseMessaging> msgStatic = mockStatic(FirebaseMessaging.class)) {
            // isFirebaseReady() internally catches this and returns false
            appStatic.when(FirebaseApp::getApps).thenThrow(new IllegalStateException("not loaded"));

            service.sendToToken("token", "T", "B", Map.of());

            msgStatic.verifyNoInteractions();
        }
    }

    // ─── sendToTokens ───────────────────────────────────────────────────────────

    @Test
    void sendToTokens_noOp_whenFirebaseNotInitialised() {
        try (MockedStatic<FirebaseApp> appStatic = mockStatic(FirebaseApp.class);
             MockedStatic<FirebaseMessaging> msgStatic = mockStatic(FirebaseMessaging.class)) {
            appStatic.when(FirebaseApp::getApps).thenReturn(List.of());

            service.sendToTokens(List.of("a", "b"), "T", "B", Map.of());

            msgStatic.verifyNoInteractions();
        }
    }

    @Test
    void sendToTokens_noOp_whenTokensNull() {
        try (MockedStatic<FirebaseApp> appStatic = mockStatic(FirebaseApp.class);
             MockedStatic<FirebaseMessaging> msgStatic = mockStatic(FirebaseMessaging.class)) {
            firebaseReady(appStatic);

            service.sendToTokens(null, "T", "B", Map.of());

            msgStatic.verifyNoInteractions();
        }
    }

    @Test
    void sendToTokens_noOp_whenTokensEmpty() {
        try (MockedStatic<FirebaseApp> appStatic = mockStatic(FirebaseApp.class);
             MockedStatic<FirebaseMessaging> msgStatic = mockStatic(FirebaseMessaging.class)) {
            firebaseReady(appStatic);

            service.sendToTokens(List.of(), "T", "B", Map.of());

            msgStatic.verifyNoInteractions();
        }
    }

    @Test
    void sendToTokens_noOp_whenAllTokensBlankOrNull() {
        List<String> tokens = new ArrayList<>();
        tokens.add(null);
        tokens.add("");
        tokens.add("   ");

        try (MockedStatic<FirebaseApp> appStatic = mockStatic(FirebaseApp.class);
             MockedStatic<FirebaseMessaging> msgStatic = mockStatic(FirebaseMessaging.class)) {
            firebaseReady(appStatic);

            // After filtering, validTokens is empty -> returns before getInstance()
            service.sendToTokens(tokens, "T", "B", Map.of());

            msgStatic.verifyNoInteractions();
        }
    }

    @Test
    void sendToTokens_sendsSingleMulticast_forSmallValidList() throws Exception {
        FirebaseMessaging messaging = mock(FirebaseMessaging.class);
        BatchResponse batch = mock(BatchResponse.class);
        when(batch.getSuccessCount()).thenReturn(2);
        when(batch.getFailureCount()).thenReturn(0);
        when(messaging.sendEachForMulticast(any(MulticastMessage.class))).thenReturn(batch);

        try (MockedStatic<FirebaseApp> appStatic = mockStatic(FirebaseApp.class);
             MockedStatic<FirebaseMessaging> msgStatic = mockStatic(FirebaseMessaging.class)) {
            firebaseReady(appStatic);
            msgStatic.when(FirebaseMessaging::getInstance).thenReturn(messaging);

            service.sendToTokens(List.of("t1", "t2"), "Title", "Body", Map.of("k", "v"));

            verify(messaging, times(1)).sendEachForMulticast(any(MulticastMessage.class));
            verify(messaging, never()).send(any(Message.class));
        }
    }

    @Test
    void sendToTokens_deduplicatesTokensBeforeSending() throws Exception {
        FirebaseMessaging messaging = mock(FirebaseMessaging.class);
        BatchResponse batch = mock(BatchResponse.class);
        when(messaging.sendEachForMulticast(any(MulticastMessage.class))).thenReturn(batch);

        try (MockedStatic<FirebaseApp> appStatic = mockStatic(FirebaseApp.class);
             MockedStatic<FirebaseMessaging> msgStatic = mockStatic(FirebaseMessaging.class)) {
            firebaseReady(appStatic);
            msgStatic.when(FirebaseMessaging::getInstance).thenReturn(messaging);

            // 3 entries but only 2 distinct, plus a blank that gets filtered
            service.sendToTokens(List.of("dup", "dup", "unique", "  "), "T", "B", null);

            // Still a single multicast call (1 chunk, well under the 500 limit)
            verify(messaging, times(1)).sendEachForMulticast(any(MulticastMessage.class));
        }
    }

    @Test
    void sendToTokens_partitionsIntoChunksOf500_whenOver500Tokens() throws Exception {
        FirebaseMessaging messaging = mock(FirebaseMessaging.class);
        BatchResponse batch = mock(BatchResponse.class);
        when(messaging.sendEachForMulticast(any(MulticastMessage.class))).thenReturn(batch);

        // 1100 distinct tokens -> 3 chunks (500 + 500 + 100)
        List<String> tokens = new ArrayList<>();
        for (int i = 0; i < 1100; i++) {
            tokens.add("token-" + i);
        }

        try (MockedStatic<FirebaseApp> appStatic = mockStatic(FirebaseApp.class);
             MockedStatic<FirebaseMessaging> msgStatic = mockStatic(FirebaseMessaging.class)) {
            firebaseReady(appStatic);
            msgStatic.when(FirebaseMessaging::getInstance).thenReturn(messaging);

            service.sendToTokens(tokens, "Title", "Body", Map.of("a", "b"));

            verify(messaging, times(3)).sendEachForMulticast(any(MulticastMessage.class));
        }
    }

    @Test
    void sendToTokens_swallowsException_whenMulticastThrows() throws Exception {
        FirebaseMessaging messaging = mock(FirebaseMessaging.class);
        // catch(Exception) arm in sendMulticast must absorb this — fire-and-forget
        when(messaging.sendEachForMulticast(any(MulticastMessage.class)))
                .thenThrow(new RuntimeException("FCM down"));

        try (MockedStatic<FirebaseApp> appStatic = mockStatic(FirebaseApp.class);
             MockedStatic<FirebaseMessaging> msgStatic = mockStatic(FirebaseMessaging.class)) {
            firebaseReady(appStatic);
            msgStatic.when(FirebaseMessaging::getInstance).thenReturn(messaging);

            // No exception escapes
            service.sendToTokens(List.of("t1"), "T", "B", Map.of("k", "v"));

            verify(messaging).sendEachForMulticast(any(MulticastMessage.class));
        }
    }
}
