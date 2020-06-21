package servent.message.snapshot;

import app.ServentInfo;
import app.snapshot_bitcake.customInfo.SnapshotInfo;
import app.snapshot_bitcake.customInfo.SnapshotInfoStatus;
import servent.message.BasicMessage;
import servent.message.MessageType;

public class LYMarkerMessage extends BasicMessage {

	private static final long serialVersionUID = 388942509576636228L;

	public LYMarkerMessage(ServentInfo sender, ServentInfo receiver, int collectorId, SnapshotInfoStatus snapshotInfoStatus) {
		super(MessageType.LY_MARKER, sender, receiver, String.valueOf(collectorId), snapshotInfoStatus);
	}
}
