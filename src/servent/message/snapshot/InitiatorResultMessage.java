package servent.message.snapshot;

import app.ServentInfo;
import app.snapshot_bitcake.customInfo.RegionResult;
import servent.message.BasicMessage;
import servent.message.MessageType;

public class InitiatorResultMessage extends BasicMessage {
    private static final long serialVersionUID = 3116394054726162318L;

    private RegionResult regionResult;
    private boolean isAsking;
    public InitiatorResultMessage(ServentInfo originalSenderInfo, ServentInfo receiverInfo, RegionResult regionResult, boolean isAsking) {
        super(MessageType.INITIATOR_MESSAGE, originalSenderInfo, receiverInfo);
        this.regionResult = regionResult;
        this.isAsking = isAsking;
    }

    public boolean isAsking() {
        return isAsking;
    }

    public RegionResult getRegionResult() {
        return regionResult;
    }
}
