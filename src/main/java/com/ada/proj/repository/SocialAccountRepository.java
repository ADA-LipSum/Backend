package com.ada.proj.repository;

import java.util.Optional;
import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import com.ada.proj.entity.SocialAccount;

public interface SocialAccountRepository extends JpaRepository<SocialAccount, Long> {

    Optional<SocialAccount> findByProviderAndProviderId(String provider, String providerId);

    List<SocialAccount> findByUserUuid(String userUuid);

    void deleteByUserUuidAndProvider(String userUuid, String provider);
}
