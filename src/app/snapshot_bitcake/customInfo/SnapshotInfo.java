package app.snapshot_bitcake.customInfo;

import java.io.Serializable;
import java.util.Objects;

public class SnapshotInfo implements Serializable {
    private static final long serialVersionUID = 8939516333227254439L;

    private int initiatorId;
    private int sequenceNo;

    public SnapshotInfo(int initiatorId, int sequenceNo) {
        this.initiatorId = initiatorId;
        this.sequenceNo = sequenceNo;
    }

    public int getSequenceNo() {
        return sequenceNo;
    }

    public int getInitiatorId() {
        return initiatorId;
    }

    public void setSequenceNo(int sequenceNo) {
        this.sequenceNo = sequenceNo;
    }

    @Override
    public boolean equals(Object obj) {
        if (obj instanceof SnapshotInfo) {
            SnapshotInfo other = (SnapshotInfo)obj;
            return getInitiatorId() == other.getInitiatorId() && getSequenceNo() == other.getSequenceNo();
        }
        return false;
    }

    @Override
    public int hashCode() {
        return Objects.hash(initiatorId, sequenceNo);
    }

    @Override
    public String toString() {
        return "SnapshotInfo[id: " + initiatorId + ", " + sequenceNo + "]";
    }
}
