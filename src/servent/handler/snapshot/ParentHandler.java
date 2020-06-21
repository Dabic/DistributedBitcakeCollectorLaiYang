package servent.handler.snapshot;

import app.AppConfig;
import app.ServentInfo;
import app.snapshot_bitcake.LaiYangBitcakeManager;
import servent.handler.MessageHandler;
import servent.message.Message;
import servent.message.snapshot.ParentMessage;

public class ParentHandler implements MessageHandler {

    private ParentMessage clientMessage;
    private LaiYangBitcakeManager bitcakeManager;

    public ParentHandler(Message clientMessage, LaiYangBitcakeManager bitcakeManager) {
        this.clientMessage = (ParentMessage)clientMessage;
        this.bitcakeManager = bitcakeManager;
    }

    /*
    * Got the message from a child after sending marker message
    * If the childes parent id is different from -1, that the child already has a parent
    * Remove that child from children list
    * If I dont have any children that means I am a LEAF and that I can return result to my parent
    * */
    @Override
    public void run() {
        synchronized (AppConfig.colorLock) {
            int parent = Integer.parseInt(clientMessage.getMessageText());
            if (parent != -1) {
                AppConfig.timestampedErrorPrint(clientMessage.getOriginalSenderInfo() + " HAS PARENT " + parent + " for snapshot " + clientMessage.getSnapshotInfo());
                bitcakeManager.removeChild(clientMessage.getOriginalSenderInfo());
                AppConfig.timestampedErrorPrint("NOVI SNAPSHOT INFO " + clientMessage.getSnapshotInfo());
                bitcakeManager.getRegionInfo().getRegionResult().addSnapshotInfo(clientMessage.getSnapshotInfo());
                AppConfig.timestampedErrorPrint("My new children are: " + bitcakeManager.getChildren());
                if (bitcakeManager.getChildren().size() == 0) {
                    bitcakeManager.sendResult();
                    AppConfig.timestampedErrorPrint("saljem result parentu list sam");
                } else if (bitcakeManager.getRegionInfo().getGotResultCount() == bitcakeManager.getRegionInfo().getChildren().size()) {
                    bitcakeManager.sendResult();
                    AppConfig.timestampedErrorPrint("saljem result parentu 2");
                }
            }
        }
    }
}
