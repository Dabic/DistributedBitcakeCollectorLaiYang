package servent;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketTimeoutException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import app.AppConfig;
import app.Cancellable;
import app.ServentInfo;
import app.snapshot_bitcake.LaiYangBitcakeManager;
import app.snapshot_bitcake.SnapshotCollector;
import app.snapshot_bitcake.customInfo.SnapshotInfo;
import app.snapshot_bitcake.customInfo.SnapshotInfoStatus;
import servent.handler.MessageHandler;
import servent.handler.NullHandler;
import servent.handler.TransactionHandler;
import servent.handler.snapshot.*;
import servent.message.Message;
import servent.message.MessageType;
import servent.message.snapshot.ParentMessage;
import servent.message.util.MessageUtil;

public class SimpleServentListener implements Runnable, Cancellable {

	private volatile boolean working = true;
	
	private SnapshotCollector snapshotCollector;
	
	public SimpleServentListener(SnapshotCollector snapshotCollector) {
		this.snapshotCollector = snapshotCollector;
	}

	/*
	 * Thread pool for executing the handlers. Each client will get it's own handler thread.
	 */
	private final ExecutorService threadPool = Executors.newWorkStealingPool();
	
	@Override
	public void run() {
		ServerSocket listenerSocket = null;
		try {
			listenerSocket = new ServerSocket(AppConfig.myServentInfo.getListenerPort(), 100);
			/*
			 * If there is no connection after 1s, wake up and see if we should terminate.
			 */
			listenerSocket.setSoTimeout(1000);
		} catch (IOException e) {
			AppConfig.timestampedErrorPrint("Couldn't open listener socket on: " + AppConfig.myServentInfo.getListenerPort());
			System.exit(0);
		}
		
		
		while (working) {
			try {
				Message clientMessage;
				
				/*
				 * Lai-Yang stuff. Process any red messages we got before we got the marker.
				 * The marker contains the collector id, so we need to process that as our first
				 * red message.
				/*
				 * This blocks for up to 1s, after which SocketTimeoutException is thrown.
				 */
				Socket clientSocket = listenerSocket.accept();

				//GOT A MESSAGE! <3
				clientMessage = MessageUtil.readMessage(clientSocket);
				LaiYangBitcakeManager lyFinancialManager = (LaiYangBitcakeManager)snapshotCollector.getBitcakeManager();

				synchronized (AppConfig.colorLock) {
					if (clientMessage.getMessageType() == MessageType.LY_MARKER) {
						/*
						* First check if I already have a parent
						* If I do have a parent, send back parent message with my parent id and current snapshot info
						*/
						ServentInfo master = lyFinancialManager.getMaster();
						if (master != null) {
							Message message = new ParentMessage(
									AppConfig.myServentInfo,
									clientMessage.getOriginalSenderInfo(),
									master.getId(),
									lyFinancialManager.getCurrentSnapshot()
							);
							MessageUtil.sendMessage(message);
						} else {
							/*
							* I dont have a parent. Now I have to check did I get a new snapshot to process.
							*/
							SnapshotInfoStatus senderSnapshotInfoStatus = clientMessage.getSnapshotInfoStatus();
							SnapshotInfo newSnapshotInfo = lyFinancialManager.getSnapshotIfDifferent(senderSnapshotInfoStatus);
							if (newSnapshotInfo != null) {
								/*
								* Got a different snapshot than mine, should check if it is a real snapshot
								*/
								SnapshotInfo currentSnapshotInfo = lyFinancialManager.getCurrentSnapshotInfoById(newSnapshotInfo.getInitiatorId());
								if (currentSnapshotInfo.getSequenceNo() < newSnapshotInfo.getSequenceNo()) {
									/*
									* This is a new snapshot for sure, I should process it
									* Set current snapshot to new snapshot and its sender to my new master
									* Than send a parent message telling that i dont have a parent
									* After that send markers to my other children (markerEvent)
									*/
									ServentInfo newMaster = clientMessage.getOriginalSenderInfo();
									lyFinancialManager.setMasterForSnapshot(newMaster, newSnapshotInfo);
									Message message = new ParentMessage(
											AppConfig.myServentInfo,
											clientMessage.getOriginalSenderInfo(),
											-1,
											newSnapshotInfo
									);
									MessageUtil.sendMessage(message);
									lyFinancialManager.markerEvent(newSnapshotInfo, snapshotCollector);
								}

							}
						}
					}
				}
				
				MessageHandler messageHandler = new NullHandler(clientMessage);
				
				/*
				 * Each message type has it's own handler.
				 * If we can get away with stateless handlers, we will,
				 * because that way is much simpler and less error prone.
				 */
				switch (clientMessage.getMessageType()) {
				case TRANSACTION:
					messageHandler = new TransactionHandler(clientMessage, snapshotCollector.getBitcakeManager());
					break;
				case LY_MARKER:
					messageHandler = new LYMarkerHandler();
					break;
				case LY_TELL:
					messageHandler = new LYTellHandler(clientMessage, snapshotCollector);
					break;
				case PARENT_MESSAGE:
					messageHandler = new ParentHandler(clientMessage, lyFinancialManager);
					break;
				case RESULT_MESSAGE:
					messageHandler = new ResultHandler(clientMessage, lyFinancialManager);
					break;
				case INITIATOR_MESSAGE:
					messageHandler = new InitiatorMessageHandler(clientMessage, lyFinancialManager);
					break;
				}
				
				threadPool.submit(messageHandler);
			} catch (SocketTimeoutException timeoutEx) {
				//Uncomment the next line to see that we are waking up every second.
//				AppConfig.timedStandardPrint("Waiting...");
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
	}

	@Override
	public void stop() {
		this.working = false;
	}

}
