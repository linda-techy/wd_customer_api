package com.wd.custapi.service;

import com.wd.custapi.dto.ProjectModuleDtos.GalleryImageDto;
import com.wd.custapi.dto.ProjectModuleDtos.GalleryUploadRequest;
import com.wd.custapi.model.CustomerUser;
import com.wd.custapi.model.GalleryImage;
import com.wd.custapi.model.Project;
import com.wd.custapi.repository.CustomerUserRepository;
import com.wd.custapi.repository.GalleryImageRepository;
import com.wd.custapi.repository.ProjectRepository;
import com.wd.custapi.repository.SiteReportRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class GalleryServiceTest {

    @Mock
    private GalleryImageRepository galleryImageRepository;

    @Mock
    private ProjectRepository projectRepository;

    @Mock
    private CustomerUserRepository userRepository;

    @Mock
    private SiteReportRepository siteReportRepository;

    @Mock
    private FileStorageService fileStorageService;

    @InjectMocks
    private GalleryService galleryService;

    private Project project;
    private CustomerUser user;

    @BeforeEach
    void setUp() {
        project = new Project();
        project.setId(1L);
        project.setName("Test Project");

        user = new CustomerUser();
        user.setId(2L);
        user.setFirstName("Jane");
        user.setLastName("Smith");
    }

    // Helper: build a realistic GalleryImage for use as repository return value
    private GalleryImage buildImage(Long id, LocalDate takenDate) {
        GalleryImage img = new GalleryImage();
        img.setProject(project);
        img.setImagePath("projects/1/gallery/img-" + id + ".jpg");
        img.setCaption("Caption " + id);
        img.setTakenDate(takenDate);
        img.setUploadedBy(user);
        img.setUploadedAt(LocalDateTime.now());
        img.setLocationTag("Foundation");
        // Inject id via reflection since there is no setter
        org.springframework.test.util.ReflectionTestUtils.setField(img, "id", id);
        return img;
    }

    // ── getProjectImages ──────────────────────────────────────────────────────

    @Test
    void getProjectImages_returnsImagesOrderedByTakenDateDesc() {
        LocalDate today = LocalDate.now();
        GalleryImage older = buildImage(1L, today.minusDays(5));
        GalleryImage newer = buildImage(2L, today);

        // Repository already returns them in DESC order (by query name convention)
        when(galleryImageRepository.findByProjectIdOrderByTakenDateDesc(1L))
                .thenReturn(List.of(newer, older));

        List<GalleryImageDto> results = galleryService.getProjectImages(1L);

        assertEquals(2, results.size());
        assertEquals(2L, results.get(0).id());   // newer first
        assertEquals(1L, results.get(1).id());   // older second
    }

    @Test
    void getProjectImages_emptyForNonexistentProject() {
        when(galleryImageRepository.findByProjectIdOrderByTakenDateDesc(999L))
                .thenReturn(List.of());

        List<GalleryImageDto> results = galleryService.getProjectImages(999L);

        assertNotNull(results);
        assertTrue(results.isEmpty());
    }

    // ── uploadImage ───────────────────────────────────────────────────────────

    @Test
    void uploadImage_savesWithCorrectProjectAndUser() {
        MultipartFile file = mock(MultipartFile.class);
        GalleryUploadRequest request = new GalleryUploadRequest(
                "Roof progress", LocalDate.now(), null, "Rooftop", List.of("roof", "progress"));

        when(projectRepository.findById(1L)).thenReturn(Optional.of(project));
        when(userRepository.findById(2L)).thenReturn(Optional.of(user));
        when(fileStorageService.storeFile(file, "projects/1/gallery"))
                .thenReturn("projects/1/gallery/new-image.jpg");

        GalleryImage savedImage = buildImage(10L, LocalDate.now());
        savedImage.setCaption("Roof progress");
        savedImage.setLocationTag("Rooftop");
        when(galleryImageRepository.save(any(GalleryImage.class))).thenReturn(savedImage);

        GalleryImageDto result = galleryService.uploadImage(1L, file, request, 2L);

        ArgumentCaptor<GalleryImage> captor = ArgumentCaptor.forClass(GalleryImage.class);
        verify(galleryImageRepository).save(captor.capture());

        GalleryImage captured = captor.getValue();
        assertEquals(project, captured.getProject());
        assertEquals(user, captured.getUploadedBy());
        assertEquals("projects/1/gallery/new-image.jpg", captured.getImagePath());
        assertEquals("Roof progress", captured.getCaption());
        assertEquals("Rooftop", captured.getLocationTag());
        assertNotNull(result);
    }

    @Test
    void uploadImage_projectNotFound_throwsRuntimeException() {
        MultipartFile file = mock(MultipartFile.class);
        GalleryUploadRequest request = new GalleryUploadRequest(
                "Caption", null, null, null, null);

        when(projectRepository.findById(999L)).thenReturn(Optional.empty());

        assertThrows(RuntimeException.class,
                () -> galleryService.uploadImage(999L, file, request, 2L));
        verify(galleryImageRepository, never()).save(any());
    }

    @Test
    void uploadImage_userNotFound_throwsRuntimeException() {
        MultipartFile file = mock(MultipartFile.class);
        GalleryUploadRequest request = new GalleryUploadRequest(
                "Caption", null, null, null, null);

        when(projectRepository.findById(1L)).thenReturn(Optional.of(project));
        when(userRepository.findById(999L)).thenReturn(Optional.empty());
        when(fileStorageService.storeFile(any(), any())).thenReturn("some/path.jpg");

        assertThrows(RuntimeException.class,
                () -> galleryService.uploadImage(1L, file, request, 999L));
        verify(galleryImageRepository, never()).save(any());
    }
}
