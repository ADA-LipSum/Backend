package com.ada.proj.service;

import java.util.Arrays;
import java.util.List;
import java.util.Objects;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.annotation.Lazy;
import org.springframework.context.event.ContextClosedEvent;
import org.springframework.context.event.EventListener;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
public class AutoIncrementMaintenanceService {

    private final JdbcTemplate jdbcTemplate;
    private final boolean enabled;
    private final List<String> tables;
    private final boolean resequenceEnabled;
    private final String orderColumn;
    private final boolean resequencePrimaryEnabled;
    private final List<String> resequencePrimaryTables;
    private final AutoIncrementMaintenanceService self;
    private final Object maintainMutex = new Object();

    public AutoIncrementMaintenanceService(
        JdbcTemplate jdbcTemplate,
        @Value("${app.auto-increment.maintain.enabled:true}") boolean enabled,
        @Value("${app.auto-increment.maintain.tables:users}") String tablesCsv,
        @Value("${app.auto-increment.maintain.resequence-order:true}") boolean resequenceEnabled,
        @Value("${app.auto-increment.maintain.order-column:order_no}") String orderColumn,
        @Value("${app.auto-increment.maintain.resequence-primary.enabled:false}") boolean resequencePrimaryEnabled,
        @Value("${app.auto-increment.maintain.resequence-primary.tables:users}") String reseqPkTablesCsv,
        @Lazy AutoIncrementMaintenanceService self
    ) {
        this.jdbcTemplate = jdbcTemplate;
        this.enabled = enabled;
        this.resequenceEnabled = resequenceEnabled;
        this.orderColumn = orderColumn;
        this.tables = Arrays.stream(tablesCsv.split(","))
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .toList();
    this.resequencePrimaryEnabled = resequencePrimaryEnabled;
    this.resequencePrimaryTables = Arrays.stream(reseqPkTablesCsv.split(","))
        .map(String::trim)
        .filter(s -> !s.isEmpty())
        .toList();
    this.self = self;
    }

    @EventListener(ApplicationReadyEvent.class)
    public void onStartup() {
        if (!enabled) return;
        safeMaintain("startup");
    }

    @EventListener(ContextClosedEvent.class)
    public void onShutdown() {
        if (!enabled) return;
        safeMaintain("shutdown");
    }

    @Scheduled(fixedDelayString = "PT5M")
    public void periodic() {
        if (!enabled) return;
        safeMaintain("scheduled");
    }

    private void safeMaintain(String phase) {
        synchronized (maintainMutex) {
            for (String table : tables) {
                try {
                    maintainTable(table);
                } catch (Exception e) {
                    log.warn("[AI-MAINTAIN] 단계={}, 테이블={} 실패: {}", phase, table, e.getMessage());
                }
            }
        }
    }

    private void maintainTable(String table) {
            String aiColumn = getAutoIncrementColumn(table);
            if (aiColumn == null) {
                log.debug("[AI-MAINTAIN] 테이블={} 자동증가 컬럼 없음 – 건너뜀", table);
                return;
            }

            Long maxVal = jdbcTemplate.queryForObject(
                "SELECT MAX(`" + aiColumn + "`) FROM `" + table + "`",
                Long.class
            );
            long next = (maxVal == null || maxVal <= 0) ? 1 : maxVal + 1;
            jdbcTemplate.execute("ALTER TABLE `" + table + "` AUTO_INCREMENT = " + next);
            log.info("[AI-MAINTAIN] 테이블={}, AI컬럼={}, 최대값={}, AUTO_INCREMENT={}로 설정", table, aiColumn, maxVal, next);
        
        if (resequenceEnabled) {
            try {
                ensureOrderColumnExists(table);
                resequenceOrderColumn(table);
            } catch (Exception e) {
                log.warn("[AI-RESEQUENCE] 테이블={} 실패: {}", table, e.getMessage());
            }
        }

        if (resequencePrimaryEnabled && resequencePrimaryTables.contains(table)) {
            try {
                self.resequencePrimaryKey(table);
            } catch (Exception e) {
                log.warn("[AI-RESEQUENCE-PK] 테이블={} 실패: {}", table, e.getMessage());
            }
        }
    }

    private void ensureOrderColumnExists(String table) {
        if (!columnExists(table, orderColumn)) {
            String sql = "ALTER TABLE `" + table + "` ADD COLUMN `" + orderColumn + "` BIGINT NOT NULL DEFAULT 0";
            jdbcTemplate.execute(sql);
        }
    }

    private void resequenceOrderColumn(String table) {
        String pk = getPrimaryKeyColumn(table);
        boolean hasCreatedAt = columnExists(table, "created_at");
        String orderBy = hasCreatedAt ? "`created_at`, `" + pk + "`" : "`" + pk + "`";

    String sql = "UPDATE `" + table + "` t " +
        "JOIN (SELECT `" + pk + "` AS k, ROW_NUMBER() OVER (ORDER BY " + orderBy + ") AS rn FROM `" + table + "`) o " +
        "ON t.`" + pk + "` = o.k " +
        "SET t.`" + orderColumn + "` = o.rn";
    jdbcTemplate.execute(sql);
    log.info("[AI-RESEQUENCE] 테이블={} 컬럼={} 재정렬 완료, 기준={}", table, orderColumn, orderBy);
    }

    private String getPrimaryKeyColumn(String table) {
        String sql = "SELECT k.COLUMN_NAME" +
                " FROM information_schema.TABLE_CONSTRAINTS t" +
                " JOIN information_schema.KEY_COLUMN_USAGE k ON t.CONSTRAINT_NAME = k.CONSTRAINT_NAME" +
                " AND t.TABLE_SCHEMA = k.TABLE_SCHEMA AND t.TABLE_NAME = k.TABLE_NAME" +
                " WHERE t.CONSTRAINT_TYPE = 'PRIMARY KEY'" +
                " AND t.TABLE_SCHEMA = DATABASE() AND t.TABLE_NAME = ?";
        return jdbcTemplate.queryForObject(sql, (rs, rowNum) -> rs.getString(1), table);
    }

    private String getAutoIncrementColumn(String table) {
        String sql = "SELECT COLUMN_NAME FROM information_schema.COLUMNS " +
                "WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = ? AND EXTRA LIKE '%auto_increment%'";
        List<String> cols = jdbcTemplate.query(sql, (rs, rn) -> rs.getString(1), table);
        return cols.isEmpty() ? null : cols.get(0);
    }

    private boolean columnExists(String table, String column) {
        String sql = "SELECT COUNT(*) FROM information_schema.COLUMNS" +
                " WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = ? AND COLUMN_NAME = ?";
        Integer n = jdbcTemplate.queryForObject(sql, Integer.class, table, column);
        return n != null && n > 0;
    }

    @Transactional
    protected void resequencePrimaryKey(String table) {
        final String UPDATE_JOIN_REF_TEMPLATE = "UPDATE `%s` r JOIN `%s` m ON r.`%s` = m.old_pk SET r.`%s` = m.new_pk";
        final String UPDATE_JOIN_BASE_TEMPLATE = "UPDATE `%s` t JOIN `%s` m ON t.`%s` = m.old_pk SET t.`%s` = m.new_pk";
        final String UPDATE_MAP_ADD_OFFSET_TEMPLATE = "UPDATE `%s` SET new_pk = new_pk + %d";
        final String UPDATE_MAP_TO_FINAL_TEMPLATE = "UPDATE `%s` SET old_pk = new_pk, new_pk = new_pk - %d";

        String pk = getPrimaryKeyColumn(table);
        boolean hasCreatedAt = columnExists(table, "created_at");
        String orderBy = hasCreatedAt ? "`created_at`, `" + pk + "`" : "`" + pk + "`";

        final String map = "tmp_pk_map_" + table;
        final long offset = 1_000_000_000L;

        jdbcTemplate.execute("DROP TEMPORARY TABLE IF EXISTS `" + map + "`");
        jdbcTemplate.execute("CREATE TEMPORARY TABLE `" + map + "` (old_pk BIGINT PRIMARY KEY, new_pk BIGINT NOT NULL)");
        jdbcTemplate.execute("INSERT INTO `" + map + "` (old_pk, new_pk) " +
                "SELECT `" + pk + "`, ROW_NUMBER() OVER (ORDER BY " + orderBy + ") FROM `" + table + "`");

        jdbcTemplate.execute("SET FOREIGN_KEY_CHECKS=0");

        jdbcTemplate.execute(Objects.requireNonNull(String.format(UPDATE_MAP_ADD_OFFSET_TEMPLATE, map, offset)));

        jdbcTemplate.execute(Objects.requireNonNull(String.format(UPDATE_JOIN_BASE_TEMPLATE, table, map, pk, pk)));

        for (FkRef fk : getReferencingFks(table, pk)) {
            jdbcTemplate.execute(Objects.requireNonNull(String.format(UPDATE_JOIN_REF_TEMPLATE, fk.table, map, fk.column, fk.column)));
        }

        jdbcTemplate.execute(Objects.requireNonNull(String.format(UPDATE_MAP_TO_FINAL_TEMPLATE, map, offset)));

        jdbcTemplate.execute(Objects.requireNonNull(String.format(UPDATE_JOIN_BASE_TEMPLATE, table, map, pk, pk)));

        for (FkRef fk : getReferencingFks(table, pk)) {
            jdbcTemplate.execute(Objects.requireNonNull(String.format(UPDATE_JOIN_REF_TEMPLATE, fk.table, map, fk.column, fk.column)));
        }

        jdbcTemplate.execute("SET FOREIGN_KEY_CHECKS=1");

        log.info("[AI-RESEQUENCE-PK] 테이블={} 기본키 {} 재시퀀싱 완료 (시작=1)", table, pk);
    }

    private record FkRef(String table, String column) {}

    private List<FkRef> getReferencingFks(String referencedTable, String referencedColumn) {
        String sql = "SELECT TABLE_NAME, COLUMN_NAME FROM information_schema.KEY_COLUMN_USAGE " +
                "WHERE TABLE_SCHEMA = DATABASE() AND REFERENCED_TABLE_NAME = ? AND REFERENCED_COLUMN_NAME = ?";
        return jdbcTemplate.query(sql, (rs, rn) -> new FkRef(rs.getString(1), rs.getString(2)), referencedTable, referencedColumn);
    }

    public void triggerResequencePrimary(String table) {
        self.resequencePrimaryKey(table);
    }
}
