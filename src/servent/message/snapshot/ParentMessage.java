package servent.message.snapshot;

import app.ServentInfo;
import app.snapshot_bitcake.customInfo.SnapshotInfo;
import servent.message.BasicMessage;
import servent.message.MessageType;

public class ParentMessage extends BasicMessage {
    private static final long serialVersionUID = 3116394054726162318L;

    private SnapshotInfo snapshotInfo;

    public ParentMessage(ServentInfo originalSenderInfo, ServentInfo receiverInfo, int parent, SnapshotInfo snapshotInfo) {
        super(MessageType.PARENT_MESSAGE, originalSenderInfo, receiverInfo, String.valueOf(parent));
        this.snapshotInfo = snapshotInfo;
    }

    public SnapshotInfo getSnapshotInfo() {
        return snapshotInfo;
    }
}
