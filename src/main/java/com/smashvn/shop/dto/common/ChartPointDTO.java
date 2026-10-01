package com.smashvn.shop.dto.common;

import java.math.BigDecimal;

public class ChartPointDTO {
    private Integer year;
    private Integer month;
    private Integer day;
    private Integer hour;
    private BigDecimal revenue;

    public ChartPointDTO() {}

    // Hour grouping (e.g. for Today filter)
    public ChartPointDTO(Integer hour, BigDecimal revenue) {
        this.hour = hour;
        this.revenue = revenue != null ? revenue : BigDecimal.ZERO;
    }

    // Day grouping (e.g. for Weekly/Monthly filter)
    public ChartPointDTO(Integer year, Integer month, Integer day, BigDecimal revenue) {
        this.year = year;
        this.month = month;
        this.day = day;
        this.revenue = revenue != null ? revenue : BigDecimal.ZERO;
    }

    // Month grouping (e.g. for Yearly filter)
    public ChartPointDTO(Integer year, Integer month, BigDecimal revenue) {
        this.year = year;
        this.month = month;
        this.revenue = revenue != null ? revenue : BigDecimal.ZERO;
    }

    public Integer getYear() {
        return year;
    }

    public void setYear(Integer year) {
        this.year = year;
    }

    public Integer getMonth() {
        return month;
    }

    public void setMonth(Integer month) {
        this.month = month;
    }

    public Integer getDay() {
        return day;
    }

    public void setDay(Integer day) {
        this.day = day;
    }

    public Integer getHour() {
        return hour;
    }

    public void setHour(Integer hour) {
        this.hour = hour;
    }

    public BigDecimal getRevenue() {
        return revenue;
    }

    public void setRevenue(BigDecimal revenue) {
        this.revenue = revenue;
    }
}
