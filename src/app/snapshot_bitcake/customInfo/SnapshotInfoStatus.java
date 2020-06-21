package app.snapshot_bitcake.customInfo;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public class SnapshotInfoStatus implements Serializable {
    private static final long serialVersionUID = 8939516333227254439L;

    private final List<SnapshotInfo> snapshotInfos;

    public SnapshotInfoStatus(List<SnapshotInfo> snapshotInfos) {
        this.snapshotInfos = snapshotInfos;
    }

    public List<SnapshotInfo> getSnapshotInfos() {
        return snapshotInfos;
    }

    public SnapshotInfoStatus changeSnapshotStatus(SnapshotInfo snapshotInfo) {
        List<SnapshotInfo> temp = new ArrayList<>();
        for (SnapshotInfo snpInfo : snapshotInfos) {
            if (snpInfo.getInitiatorId() != snapshotInfo.getInitiatorId()) {
                temp.add(snpInfo);
            } else {
                temp.add(snapshotInfo);
            }
        }
        return new SnapshotInfoStatus(temp);
    }


    @Override
    public int hashCode() {
        return Objects.hash(snapshotInfos);
    }
}
