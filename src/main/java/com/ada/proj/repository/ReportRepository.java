package com.ada.proj.repository;

import com.ada.proj.entity.Report;
import com.ada.proj.enums.ReportStatus;
import com.ada.proj.enums.ReportType;
import java.util.Collection;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

@Repository
public interface ReportRepository extends JpaRepository<Report, Long>, JpaSpecificationExecutor<Report> {

    boolean existsByReporterUuidAndTargetUuidAndReportTypeAndStatusIn(
            String reporterUuid,
            String targetUuid,
            ReportType reportType,
            Collection<ReportStatus> statuses
    );
}
