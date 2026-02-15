package com.ada.proj.config;

import java.util.List;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Component
@ConfigurationProperties(prefix = "app.auto-increment")
public class AutoIncrementProperties {

    private Maintain maintain = new Maintain();

    public Maintain getMaintain() {
        return maintain;
    }

    public void setMaintain(Maintain maintain) {
        this.maintain = maintain;
    }

    public static class Maintain {

        private boolean enabled = false;
        private List<String> tables;
        private boolean resequenceOrder = false;
        private String orderColumn;
        private ResequencePrimary resequencePrimary = new ResequencePrimary();

        public boolean isEnabled() {
            return enabled;
        }

        public void setEnabled(boolean enabled) {
            this.enabled = enabled;
        }

        public List<String> getTables() {
            return tables;
        }

        public void setTables(List<String> tables) {
            this.tables = tables;
        }

        public boolean isResequenceOrder() {
            return resequenceOrder;
        }

        public void setResequenceOrder(boolean resequenceOrder) {
            this.resequenceOrder = resequenceOrder;
        }

        public String getOrderColumn() {
            return orderColumn;
        }

        public void setOrderColumn(String orderColumn) {
            this.orderColumn = orderColumn;
        }

        public ResequencePrimary getResequencePrimary() {
            return resequencePrimary;
        }

        public void setResequencePrimary(ResequencePrimary resequencePrimary) {
            this.resequencePrimary = resequencePrimary;
        }

        public static class ResequencePrimary {

            private boolean enabled = false;
            private List<String> tables;

            public boolean isEnabled() {
                return enabled;
            }

            public void setEnabled(boolean enabled) {
                this.enabled = enabled;
            }

            public List<String> getTables() {
                return tables;
            }

            public void setTables(List<String> tables) {
                this.tables = tables;
            }
        }
    }
}
