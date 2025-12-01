package com.ada.proj.entity;

import com.ada.proj.enums.ReportStatus;
import com.ada.proj.enums.ReportType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import java.time.LocalDateTime;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "reports")
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Report {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "reporter_uuid", nullable = false, length = 36)
    private String reporterUuid;

    @Column(name = "target_uuid", nullable = false, length = 36)
    private String targetUuid;

    @Enumerated(EnumType.STRING)
    @Column(name = "report_type", nullable = false, length = 20)
    private ReportType reportType;

    @Column(nullable = false, length = 255)
    private String reason;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private ReportStatus status;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @Column(name = "resolved_at")
    private LocalDateTime resolvedAt;

    @Column(name = "resolved_by", length = 36)
    private String resolvedBy;

    @PrePersist
    public void onCreate() {
        if (status == null) {
            status = ReportStatus.PENDING;
        }
        if (createdAt == null) {
            createdAt = LocalDateTime.now();
        }
    }

    public void updateStatus(ReportStatus nextStatus, String resolverUuid) {
        status = nextStatus;
        if (nextStatus == ReportStatus.RESOLVED) {
            resolvedAt = LocalDateTime.now();
            resolvedBy = resolverUuid;
        } else {
            resolvedAt = null;
            resolvedBy = null;
        }
    }
}
