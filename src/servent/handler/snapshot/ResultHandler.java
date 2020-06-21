package servent.handler.snapshot;

import app.AppConfig;
import app.snapshot_bitcake.LYSnapshotResult;
import app.snapshot_bitcake.LaiYangBitcakeManager;
import app.snapshot_bitcake.customInfo.RegionResult;
import app.snapshot_bitcake.customInfo.SnapshotInfo;
import servent.handler.MessageHandler;
import servent.message.Message;
import servent.message.snapshot.ResultMessage;

import java.util.Map;

public class ResultHandler implements MessageHandler {

    private ResultMessage message;
    private LaiYangBitcakeManager bitcakeManager;

    public ResultHandler(Message message, LaiYangBitcakeManager bitcakeManager) {
        this.message = (ResultMessage) message;
        this.bitcakeManager = bitcakeManager;
    }

    @Override
    public void run() {
        synchronized (AppConfig.colorLock) {
            RegionResult regionResult = message.getRegionResult();
            AppConfig.timestampedErrorPrint("dobio sam result od " + message.getOriginalSenderInfo());
            for (Map.Entry<Integer, LYSnapshotResult> result : regionResult.getResults().entrySet()) {
                bitcakeManager.getRegionInfo().getRegionResult().addResult(result.getKey(), result.getValue());
            }
            SnapshotInfo currentSnapshot = bitcakeManager.getCurrentSnapshot();
            for (SnapshotInfo snapshotInfo : regionResult.getSnapshotInfos()) {
                if (currentSnapshot.getInitiatorId() != snapshotInfo.getInitiatorId()) {
                    bitcakeManager.getRegionInfo().getRegionResult().addSnapshotInfo(snapshotInfo);
                }
            }
            bitcakeManager.getRegionInfo().incrementGotResult();
            if (bitcakeManager.getRegionInfo().getGotResultCount() == bitcakeManager.getRegionInfo().getChildren().size()) {
                AppConfig.timestampedErrorPrint("saljem result parentu");
                bitcakeManager.sendResult();
            }
        }
    }
}
