package servent.handler.snapshot;

import app.AppConfig;
import app.snapshot_bitcake.LYSnapshotResult;
import app.snapshot_bitcake.LaiYangBitcakeManager;
import app.snapshot_bitcake.customInfo.RegionResult;
import app.snapshot_bitcake.customInfo.SnapshotInfo;
import servent.handler.MessageHandler;
import servent.message.Message;
import servent.message.snapshot.InitiatorResultMessage;
import servent.message.util.MessageUtil;

import java.util.Map;

public class InitiatorMessageHandler implements MessageHandler {
    private InitiatorResultMessage clientMessage;
    private LaiYangBitcakeManager bitcakeManager;

    public InitiatorMessageHandler(Message clientMessage, LaiYangBitcakeManager bitcakeManager) {
        this.clientMessage = (InitiatorResultMessage)clientMessage;
        this.bitcakeManager = bitcakeManager;
    }

    @Override
    public void run() {
        synchronized (AppConfig.colorLock) {
            RegionResult other = clientMessage.getRegionResult();
            for (Map.Entry<Integer, LYSnapshotResult> result : other.getResults().entrySet()) {
                bitcakeManager.getRegionInfo().getRegionResult().getResults().put(result.getKey(), result.getValue());
            }
            for (SnapshotInfo snapshotInfo : other.getSnapshotInfos()) {
                if (snapshotInfo.getInitiatorId() != AppConfig.myServentInfo.getId()) {
                    bitcakeManager.getRegionInfo().getRegionResult().getSnapshotInfos().add(snapshotInfo);
                    if (!bitcakeManager.alreadyContains(snapshotInfo)) {
                        bitcakeManager.getAlreadyProcessed().add(snapshotInfo);
                    }
                }
            }
            synchronized (bitcakeManager.getOtherLock()) {
                bitcakeManager.getOtherInitiators().put(clientMessage.getOriginalSenderInfo().getId(), true);
                for (SnapshotInfo snapshotInfo : other.getSnapshotInfos()) {
                    if (snapshotInfo.getInitiatorId() != AppConfig.myServentInfo.getId()) {
                        bitcakeManager.getOtherInitiators().putIfAbsent(snapshotInfo.getInitiatorId(), false);
                    }
                }
            }
            if (clientMessage.isAsking()) {
                Message message = new InitiatorResultMessage(
                        AppConfig.myServentInfo,
                        clientMessage.getOriginalSenderInfo(),
                        bitcakeManager.getRegionInfo().getRegionResult(),
                        false
                );
                MessageUtil.sendMessage(message);
            }
        }
    }
}
