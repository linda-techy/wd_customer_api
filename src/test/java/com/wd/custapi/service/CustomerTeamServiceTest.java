package com.wd.custapi.service;

import com.wd.custapi.dto.TeamContactDto;
import com.wd.custapi.model.ProjectMember;
import com.wd.custapi.repository.ProjectMemberRepository;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class CustomerTeamServiceTest {

    private final ProjectMemberRepository repo = Mockito.mock(ProjectMemberRepository.class);
    private final PortalUserLookup lookup = Mockito.mock(PortalUserLookup.class);
    private final CustomerTeamService service = new CustomerTeamService(repo, lookup);

    @Test
    void nullsPhoneAndEmailWhenShareWithCustomerFalse() {
        ProjectMember member = newMember(1L, 99L, "PROJECT_MANAGER", false);
        Mockito.when(repo.findVisibleStaffByProject(Mockito.eq(10L), Mockito.anyList()))
                .thenReturn(List.of(member));
        Mockito.when(lookup.lookup(99L)).thenReturn(
                new PortalUserLookup.View(99L, "Ramesh", "+91-9999", "r@x.com", "/p.png"));

        List<TeamContactDto> result = service.getTeamForProject(10L);

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getName()).isEqualTo("Ramesh");
        assertThat(result.get(0).getPhone()).isNull();
        assertThat(result.get(0).getEmail()).isNull();
        assertThat(result.get(0).getPhotoUrl()).isEqualTo("/p.png");
    }

    @Test
    void includesPhoneAndEmailWhenShareWithCustomerTrue() {
        ProjectMember member = newMember(1L, 99L, "SITE_ENGINEER", true);
        Mockito.when(repo.findVisibleStaffByProject(Mockito.eq(10L), Mockito.anyList()))
                .thenReturn(List.of(member));
        Mockito.when(lookup.lookup(99L)).thenReturn(
                new PortalUserLookup.View(99L, "Suresh", "+91-8888", "s@x.com", null));

        List<TeamContactDto> result = service.getTeamForProject(10L);

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getPhone()).isEqualTo("+91-8888");
        assertThat(result.get(0).getEmail()).isEqualTo("s@x.com");
    }

    @Test
    void emptyWhenNoStaff() {
        Mockito.when(repo.findVisibleStaffByProject(Mockito.eq(10L), Mockito.anyList()))
                .thenReturn(List.of());

        assertThat(service.getTeamForProject(10L)).isEmpty();
    }

    private ProjectMember newMember(Long memberId, Long portalUserId, String role, boolean share) {
        ProjectMember m = new ProjectMember() {
            @Override public Long getId() { return memberId; }
            @Override public Long getPortalUserId() { return portalUserId; }
            @Override public String getRoleInProject() { return role; }
            @Override public Boolean getShareWithCustomer() { return share; }
        };
        return m;
    }
}
