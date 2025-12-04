package com.ada.proj.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.ada.proj.entity.StudyGroupMember;

public interface StudyGroupMemberRepository extends JpaRepository<StudyGroupMember, Long> {

    boolean existsByGroup_GroupUuidAndUserUuid(String groupUuid, String userUuid);

    long countByGroup_GroupUuid(String groupUuid);

    List<StudyGroupMember> findByUserUuid(String userUuid);

    Optional<StudyGroupMember> findByGroup_GroupUuidAndUserUuid(String groupUuid, String userUuid);

    List<StudyGroupMember> findAllByGroup_GroupUuid(String groupUuid);
}
