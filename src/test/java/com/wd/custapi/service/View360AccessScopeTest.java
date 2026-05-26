package com.wd.custapi.service;

import com.wd.custapi.model.CustomerUser;
import com.wd.custapi.model.Project;
import com.wd.custapi.model.View360;
import com.wd.custapi.repository.CustomerUserRepository;
import com.wd.custapi.repository.ProjectRepository;
import com.wd.custapi.repository.View360Repository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * Characterization / regression-lock tests for View360Service.incrementViewCount.
 *
 * Audit Card 4.5: 360-view access must be project-scoped. A view that belongs to
 * project 999 must never be mutated (view-count incremented) when the caller
 * supplies project 50. The service must throw "360 view not found" — using the
 * same message as the not-found case to prevent cross-project enumeration — and
 * must NOT persist any change.
 */
@ExtendWith(MockitoExtension.class)
class View360AccessScopeTest {

    @Mock private View360Repository view360Repository;
    @Mock private ProjectRepository projectRepository;
    @Mock private CustomerUserRepository userRepository;

    @InjectMocks
    private View360Service view360Service;

    // ---------------------------------------------------------------------------
    // Helpers
    // ---------------------------------------------------------------------------

    private View360 makeView(Long viewId, Long ownerProjectId) {
        Project project = new Project();
        project.setId(ownerProjectId);

        CustomerUser uploader = new CustomerUser();
        uploader.setId(1L);
        uploader.setFirstName("Test");
        uploader.setLastName("User");

        View360 view = new View360();
        view.setId(viewId);
        view.setProject(project);
        view.setTitle("Test View");
        view.setViewUrl("https://example.com/view");
        view.setViewCount(0);
        view.setIsActive(true);
        view.setUploadedBy(uploader);
        return view;
    }

    // ---------------------------------------------------------------------------
    // Test 1 — IDOR guard: view belongs to project 999, caller claims project 50
    // ---------------------------------------------------------------------------

    @Test
    void incrementViewCount_viewBelongsToAnotherProject_throwsNotFound() {
        long viewId = 42L;
        View360 wrongProjectView = makeView(viewId, 999L);
        when(view360Repository.findById(viewId)).thenReturn(Optional.of(wrongProjectView));

        // Must throw with a "not found" message (same wording as missing-entity path)
        assertThatThrownBy(() -> view360Service.incrementViewCount(viewId, 50L))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("not found");

        // Save must NEVER be called — no cross-project side-effect
        verify(view360Repository, never()).save(any());
    }

    // ---------------------------------------------------------------------------
    // Test 2 — Happy path: view belongs to the caller's project, save IS called
    // ---------------------------------------------------------------------------

    @Test
    void incrementViewCount_viewBelongsToProject_savesAndReturnsDto() {
        long viewId = 42L;
        View360 ownView = makeView(viewId, 50L);
        when(view360Repository.findById(viewId)).thenReturn(Optional.of(ownView));
        when(view360Repository.save(ownView)).thenReturn(ownView);

        var dto = view360Service.incrementViewCount(viewId, 50L);

        verify(view360Repository).save(ownView);
        // DTO must reflect the incremented view count
        assertThat(dto).isNotNull();
        assertThat(dto.viewCount()).isEqualTo(1);
    }
}
