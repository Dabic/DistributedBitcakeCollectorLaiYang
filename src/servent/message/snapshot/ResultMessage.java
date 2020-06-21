package servent.message.snapshot;

import app.ServentInfo;
import app.snapshot_bitcake.LYSnapshotResult;
import app.snapshot_bitcake.customInfo.RegionResult;
import app.snapshot_bitcake.customInfo.SnapshotInfo;
import servent.message.BasicMessage;
import servent.message.MessageType;

import java.util.Map;
import java.util.Set;

public class ResultMessage extends BasicMessage {
    private static final long serialVersionUID = 3116394054726162318L;
    private RegionResult regionResult;
    public ResultMessage(ServentInfo originalSenderInfo, ServentInfo receiverInfo, RegionResult regionResult) {
        super(MessageType.RESULT_MESSAGE, originalSenderInfo, receiverInfo);
        this.regionResult = regionResult;
    }

    public RegionResult getRegionResult() {
        return regionResult;
    }
}
