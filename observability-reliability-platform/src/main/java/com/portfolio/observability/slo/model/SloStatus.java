package com.portfolio.observability.slo.model;

import java.time.Duration;
import java.time.Instant;
import java.util.UUID;

/**
 * Current status of an SLO including error budget.
 */
public record SloStatus(
    UUID sloId,
    String sloName,
    String service,
    SloType type,
    double target,
    double currentValue,          // Current SLI value
    double errorBudgetTotal,      // Total error budget (percentage)
    double errorBudgetRemaining,  // Remaining error budget (percentage)
    double errorBudgetConsumed,   // Consumed error budget (percentage)
    Duration windowRemaining,     // Time remaining in the window
    SloState state,
    Instant calculatedAt
) {
    
    public static SloStatus calculate(ServiceLevelObjective slo, double currentSli) {
        double errorBudgetTotal = 100.0 - slo.target();
        double errorUsed = slo.target() - currentSli;
        double errorBudgetConsumed = Math.max(0, Math.min(100, (errorUsed / errorBudgetTotal) * 100));
        double errorBudgetRemaining = 100.0 - errorBudgetConsumed;
        
        SloState state;
        if (currentSli >= slo.target()) {
            state = SloState.MET;
        } else if (errorBudgetRemaining > 25) {
            state = SloState.AT_RISK;
        } else if (errorBudgetRemaining > 0) {
            state = SloState.BREACHING;
        } else {
            state = SloState.BREACHED;
        }
        
        return new SloStatus(
            slo.id(),
            slo.name(),
            slo.service(),
            slo.type(),
            slo.target(),
            currentSli,
            errorBudgetTotal,
            errorBudgetRemaining,
            errorBudgetConsumed,
            slo.window(), // Simplified - in production this would track actual time remaining
            state,
            Instant.now()
        );
    }
    
    public boolean isHealthy() {
        return state == SloState.MET;
    }
    
    public boolean needsAttention() {
        return state == SloState.AT_RISK || state == SloState.BREACHING;
    }
    
    public boolean isBreached() {
        return state == SloState.BREACHED;
    }
}
