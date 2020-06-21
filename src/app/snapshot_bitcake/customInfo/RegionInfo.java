package app.snapshot_bitcake.customInfo;

import app.ServentInfo;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;

public class RegionInfo implements Serializable {
    private static final long serialVersionUID = 8939516333227254439L;
    private ServentInfo master;
    private SnapshotInfo currentSnapshot;
    private List<ServentInfo> children;
    private AtomicInteger gotResultCount;
    private RegionResult regionResult;

    public RegionInfo() {
        master = null;
        currentSnapshot = null;
        children = new ArrayList<>();
        regionResult = new RegionResult();
        gotResultCount = new AtomicInteger(0);
    }

    public int getGotResultCount() {
        return gotResultCount.get();
    }

    public void incrementGotResult() {
        gotResultCount.getAndIncrement();
    }

    public RegionResult getRegionResult() {
        return regionResult;
    }

    public ServentInfo getMaster() {
        return master;
    }

    public void setMaster(ServentInfo master) {
        this.master = master;
    }

    public SnapshotInfo getCurrentSnapshot() {
        return currentSnapshot;
    }

    public void setCurrentSnapshot(SnapshotInfo snapshotInfo) {
        this.currentSnapshot = snapshotInfo;
    }

    public List<ServentInfo> getChildren() {
        return children;
    }

    public void removeChild(ServentInfo serventInfo) {
        for (ServentInfo info : children) {
            if (info.getId() == serventInfo.getId()) {
                children.remove(info);
                break;
            }
        }
    }

    public void addChild(ServentInfo serventInfo) {
        children.add(serventInfo);
    }

    @Override
    public String toString() {
        return "RegionInfo[" +
                "master=" + master +
                ", currentSnapshot=" + currentSnapshot +
                ", children=" + children +
                ']';
    }

}
