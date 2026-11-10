package dev.routecleaner.core;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

public record ScanReport(List<ConnectionReport> connections, Set<Point3> targets) {
    public ScanReport {
        connections = List.copyOf(connections);
        targets = Set.copyOf(targets);
    }
    public long count(ConnectionReport.Status status) {
        return connections.stream().filter(c -> c.status() == status).count();
    }
    public String summary() {
        return connections.size() + " links; " + targets.size() + " targets; "
            + count(ConnectionReport.Status.BLOCKED) + " blocked; "
            + count(ConnectionReport.Status.PROTECTED) + " protected; "
            + count(ConnectionReport.Status.UNLOADED) + " unloaded";
    }
}
