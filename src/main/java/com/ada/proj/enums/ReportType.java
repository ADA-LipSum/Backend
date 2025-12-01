package com.ada.proj.enums;

/**
 * 신고 대상 유형.
 */
public enum ReportType {
    USER,
    POST,
    COMMENT;

    public static ReportType from(String value) {
        if (value == null) {
            throw new IllegalArgumentException("reportType is required");
        }
        try {
            return ReportType.valueOf(value.trim().toUpperCase());
        } catch (IllegalArgumentException ex) {
            throw new IllegalArgumentException("지원하지 않는 신고 유형입니다: " + value);
        }
    }
}
