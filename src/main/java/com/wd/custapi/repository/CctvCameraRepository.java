package com.wd.custapi.repository;

import com.wd.custapi.model.CctvCamera;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface CctvCameraRepository extends JpaRepository<CctvCamera, Long> {

    List<CctvCamera> findByProjectIdAndIsActiveTrueOrderByDisplayOrder(Long projectId);

    /**
     * Fetch a single active camera that belongs to the given project.
     * Returns {@link Optional#empty()} when the camera is inactive, does not
     * belong to {@code projectId}, or does not exist — all three cases are
     * treated as "not found" by the controller.
     */
    Optional<CctvCamera> findByIdAndProjectIdAndIsActiveTrue(Long id, Long projectId);
}
