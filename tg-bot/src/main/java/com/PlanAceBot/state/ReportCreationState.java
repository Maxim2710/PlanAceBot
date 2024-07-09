package com.PlanAceBot.state;

import lombok.Getter;
import lombok.Setter;

import java.sql.Timestamp;

@Getter
@Setter
public class ReportCreationState {
    private Timestamp startDate;
    private Timestamp endDate;
    private ReportState state;
}
