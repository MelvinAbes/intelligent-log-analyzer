package dev.loganalysis.statistics;

public enum StatisticsBucket {
    MINUTE("minute"),
    HOUR("hour"),
    DAY("day");

    private final String sqlUnit;

    StatisticsBucket(String sqlUnit) {
        this.sqlUnit = sqlUnit;
    }

    public String sqlUnit() {
        return sqlUnit;
    }
}
