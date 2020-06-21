package app.snapshot_bitcake.customInfo;

import app.snapshot_bitcake.LYSnapshotResult;

import java.io.Serializable;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class RegionResult implements Serializable {
    private static final long serialVersionUID = 8939516333227254439L;

    private Map<Integer, LYSnapshotResult> results;
    private Set<SnapshotInfo> snapshotInfos;

    public RegionResult() {
        this.results = new ConcurrentHashMap<>();
        this.snapshotInfos = new HashSet<>();
    }

    public void addResult(int id, LYSnapshotResult result) {
        results.put(id, result);
    }

    public Set<SnapshotInfo> getSnapshotInfos() {
        return snapshotInfos;
    }

    public void addSnapshotInfo(SnapshotInfo snapshotInfo) {
        snapshotInfos.add(snapshotInfo);
    }

    public Map<Integer, LYSnapshotResult> getResults() {
        return results;
    }

    @Override
    public String toString() {
        return "RegionResult{" +
                "results=" + results +
                '}';
    }
}
