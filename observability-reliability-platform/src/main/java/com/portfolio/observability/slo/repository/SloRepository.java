package com.portfolio.observability.slo.repository;

import com.portfolio.observability.slo.model.ServiceLevelObjective;
import com.portfolio.observability.slo.model.SloState;
import com.portfolio.observability.slo.model.SloStatus;
import com.portfolio.observability.slo.model.SloType;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

/**
 * In-memory repository for Service Level Objectives (SLOs) and their status tracking.
 * Supports SLO lifecycle management and compliance monitoring.
 */
@Repository
public class SloRepository {

    private final Map<UUID, ServiceLevelObjective> slos = new ConcurrentHashMap<>();
    private final Map<UUID, List<SloStatus>> sloStatusHistory = new ConcurrentHashMap<>();

    // SLO Definition operations

    public ServiceLevelObjective save(ServiceLevelObjective slo) {
        slos.put(slo.id(), slo);
        return slo;
    }

    public Optional<ServiceLevelObjective> findById(UUID id) {
        return Optional.ofNullable(slos.get(id));
    }

    public Optional<ServiceLevelObjective> findByName(String name) {
        return slos.values().stream()
                .filter(s -> name.equals(s.name()))
                .findFirst();
    }

    public List<ServiceLevelObjective> findAll() {
        return new ArrayList<>(slos.values());
    }

    public List<ServiceLevelObjective> findByService(String serviceName) {
        return slos.values().stream()
                .filter(s -> serviceName.equals(s.service()))
                .collect(Collectors.toList());
    }

    public List<ServiceLevelObjective> findByType(SloType type) {
        return slos.values().stream()
                .filter(s -> s.type() == type)
                .collect(Collectors.toList());
    }

    public List<ServiceLevelObjective> findEnabled() {
        return slos.values().stream()
                .filter(ServiceLevelObjective::enabled)
                .collect(Collectors.toList());
    }

    public void deleteById(UUID id) {
        slos.remove(id);
        sloStatusHistory.remove(id);
    }

    public boolean existsByName(String name) {
        return slos.values().stream()
                .anyMatch(s -> name.equals(s.name()));
    }

    // SLO Status operations

    public SloStatus saveStatus(SloStatus status) {
        sloStatusHistory.computeIfAbsent(status.sloId(), k -> 
            Collections.synchronizedList(new ArrayList<>())
        ).add(status);
        return status;
    }

    public Optional<SloStatus> findLatestStatus(UUID sloId) {
        return sloStatusHistory.getOrDefault(sloId, Collections.emptyList()).stream()
                .max(Comparator.comparing(SloStatus::calculatedAt));
    }

    public List<SloStatus> findStatusHistory(UUID sloId, Instant start, Instant end) {
        return sloStatusHistory.getOrDefault(sloId, Collections.emptyList()).stream()
                .filter(s -> !s.calculatedAt().isBefore(start) && !s.calculatedAt().isAfter(end))
                .sorted(Comparator.comparing(SloStatus::calculatedAt))
                .collect(Collectors.toList());
    }

    public List<SloStatus> findAllLatestStatuses() {
        return slos.keySet().stream()
                .map(this::findLatestStatus)
                .filter(Optional::isPresent)
                .map(Optional::get)
                .collect(Collectors.toList());
    }

    public List<SloStatus> findBreachedSlos() {
        return findAllLatestStatuses().stream()
                .filter(s -> s.state() == SloState.BREACHED)
                .collect(Collectors.toList());
    }

    public List<SloStatus> findAtRiskSlos() {
        return findAllLatestStatuses().stream()
                .filter(s -> s.state() == SloState.AT_RISK || s.state() == SloState.BREACHING)
                .collect(Collectors.toList());
    }

    public Map<SloState, Long> getStateDistribution() {
        return findAllLatestStatuses().stream()
                .collect(Collectors.groupingBy(SloStatus::state, Collectors.counting()));
    }

    public double calculateOverallCompliance() {
        List<SloStatus> statuses = findAllLatestStatuses();
        if (statuses.isEmpty()) {
            return 100.0;
        }
        long healthy = statuses.stream()
                .filter(s -> s.state() == SloState.MET)
                .count();
        return (double) healthy / statuses.size() * 100.0;
    }

    public double getAverageErrorBudgetRemaining() {
        List<SloStatus> statuses = findAllLatestStatuses();
        if (statuses.isEmpty()) {
            return 100.0;
        }
        return statuses.stream()
                .mapToDouble(SloStatus::errorBudgetRemaining)
                .average()
                .orElse(100.0);
    }

    public void deleteStatusOlderThan(UUID sloId, Instant threshold) {
        List<SloStatus> history = sloStatusHistory.get(sloId);
        if (history != null) {
            history.removeIf(s -> s.calculatedAt().isBefore(threshold));
        }
    }

    public void clear() {
        slos.clear();
        sloStatusHistory.clear();
    }
}
