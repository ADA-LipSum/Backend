package com.ada.proj.repository;

import com.ada.proj.entity.StudyGroupJoinRequest;
import com.ada.proj.enums.JoinRequestStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface StudyGroupJoinRequestRepository extends JpaRepository<StudyGroupJoinRequest, Long> {

    Optional<StudyGroupJoinRequest> findByGroup_GroupUuidAndUserUuid(String groupUuid, String userUuid);

    boolean existsByGroup_GroupUuidAndUserUuidAndStatus(String groupUuid, String userUuid, JoinRequestStatus status);

    List<StudyGroupJoinRequest> findAllByGroup_GroupUuidAndStatusOrderByRequestedAtDesc(String groupUuid, JoinRequestStatus status);
}
