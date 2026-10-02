package com.portfolio.observability.slo.model;

/**
 * State of an SLO.
 */
public enum SloState {
    MET,        // SLO target is being met
    AT_RISK,    // Error budget is being consumed faster than expected
    BREACHING,  // SLO is below target but budget remains
    BREACHED    // Error budget is exhausted
}
