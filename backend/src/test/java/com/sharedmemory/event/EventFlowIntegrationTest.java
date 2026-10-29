package com.sharedmemory.event;

import static org.assertj.core.api.Assertions.assertThat;

import com.sharedmemory.BaseIntegrationTest;
import com.sharedmemory.event.dto.CreateEventRequest;
import com.sharedmemory.event.dto.EventResponse;
import com.sharedmemory.identity.dto.AuthResponse;
import com.sharedmemory.identity.dto.RegisterRequest;
import com.sharedmemory.membership.dto.EventMemberResponse;
import com.sharedmemory.membership.dto.InvitationPreviewResponse;
import com.sharedmemory.membership.dto.InvitationResponse;
import com.sharedmemory.membership.dto.JoinEventRequest;
import java.time.LocalDate;
import org.junit.jupiter.api.Test;

class EventFlowIntegrationTest extends BaseIntegrationTest {

    @Test
    void twoUsersCanCreateInviteAndJoinAnEvent() throws Exception {
        AuthResponse organizer = register("aditya@test.com", "password123", "Aditya");
        AuthResponse invitee = register("rahul@test.com", "password123", "Rahul");

        var createResponse = post(
                "/api/events",
                organizer.token(),
                new CreateEventRequest(
                        "Goa Trip 2026", "Friends trip", LocalDate.of(2026, 9, 1), LocalDate.of(2026, 9, 5)),
                EventResponse.class);
        assertThat(createResponse.status()).isEqualTo(201);
        EventResponse event = createResponse.body();
        assertThat(event).isNotNull();

        var invitationResponse = post(
                "/api/events/" + event.id() + "/invitations", organizer.token(), null, InvitationResponse.class);
        assertThat(invitationResponse.status()).isEqualTo(201);
        String code = invitationResponse.body().code();

        var preview = get("/api/invitations/" + code, null, InvitationPreviewResponse.class);
        assertThat(preview.status()).isEqualTo(200);
        assertThat(preview.body().eventName()).isEqualTo("Goa Trip 2026");
        assertThat(preview.body().memberCount()).isEqualTo(1);

        var joinResponse = post(
                "/api/events/join", invitee.token(), new JoinEventRequest(code, "Rahul"), EventMemberResponse.class);
        assertThat(joinResponse.status()).isEqualTo(200);
        assertThat(joinResponse.body().role()).isEqualTo("MEMBER");

        var members = get("/api/events/" + event.id() + "/members", organizer.token(), EventMemberResponse[].class);
        assertThat(members.body()).hasSize(2);

        var leaveResponse = post("/api/events/" + event.id() + "/leave", invitee.token(), null, Void.class);
        assertThat(leaveResponse.status()).isEqualTo(204);

        var membersAfterLeave =
                get("/api/events/" + event.id() + "/members", organizer.token(), EventMemberResponse[].class);
        assertThat(membersAfterLeave.body()).hasSize(1);
        assertThat(membersAfterLeave.body()[0].role()).isEqualTo("ORGANIZER");
    }

    private AuthResponse register(String email, String password, String displayName) throws Exception {
        var response = post(
                "/api/auth/register", null, new RegisterRequest(email, password, displayName), AuthResponse.class);
        assertThat(response.status()).isEqualTo(201);
        return response.body();
    }
}
