package app.snapshot_bitcake;

import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

import app.AppConfig;
import app.snapshot_bitcake.customInfo.RegionResult;
import app.snapshot_bitcake.customInfo.SnapshotInfo;

/**
 * Main snapshot collector class. Has support for Naive, Chandy-Lamport
 * and Lai-Yang snapshot algorithms.
 * 
 * @author bmilojkovic
 *
 */
public class SnapshotCollectorWorker implements SnapshotCollector {

	private volatile boolean working = true;
	
	private AtomicBoolean collecting = new AtomicBoolean(false);
	private AtomicInteger snapshotNo = new AtomicInteger(0);
	
	private RegionResult regionResult = null;
	
	private BitcakeManager bitcakeManager;

	public SnapshotCollectorWorker() {
		bitcakeManager = new LaiYangBitcakeManager();
	}
	
	@Override
	public BitcakeManager getBitcakeManager() {
		return bitcakeManager;
	}
	boolean waiting = true;
	@Override
	public void run() {
		while(working) {
			
			/*
			 * Not collecting yet - just sleep until we start actual work, or finish
			 */
			while (collecting.get() == false) {
				try {
					Thread.sleep(1000);
				} catch (InterruptedException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
				
				if (working == false) {
					return;
				}
			}
			
			/*
			 * Collecting is done in three stages:
			 * 1. Send messages asking for values
			 * 2. Wait for all the responses
			 * 3. Print result
			 */

			SnapshotInfo newSnapshotInfo = new SnapshotInfo(AppConfig.myServentInfo.getId(), snapshotNo.get());
			//1 send asks
			((LaiYangBitcakeManager)bitcakeManager).markerEvent(newSnapshotInfo, this);

			waiting = true;
			//2 wait for responses or finish
			while (waiting) {
				if (regionResult != null) {
					waiting = false;
				}
				
				try {
					Thread.sleep(1000);
				} catch (InterruptedException e) {
					e.printStackTrace();
				}
				
				if (working == false) {
					return;
				}
			}
			
			//print
			int sum;
			sum = 0;
			for (Entry<Integer, LYSnapshotResult> nodeResult : regionResult.getResults().entrySet()) {
				sum += nodeResult.getValue().getRecordedAmount();
				AppConfig.timestampedStandardPrint(
						"Recorded bitcake amount for " + nodeResult.getKey() + " = " + nodeResult.getValue().getRecordedAmount());
			}
			for(int i = 0; i < AppConfig.getServentCount(); i++) {
				for (int j = 0; j < AppConfig.getServentCount(); j++) {
					if (i != j) {
						if (AppConfig.getInfoById(i).getNeighbors().contains(j) &&
							AppConfig.getInfoById(j).getNeighbors().contains(i)) {
							int ijAmount = regionResult.getResults().get(i).getGiveHistory().get(j);
							int jiAmount = regionResult.getResults().get(j).getGetHistory().get(i);

							if (ijAmount != jiAmount) {
								String outputString = String.format(
										"Unreceived bitcake amount: %d from servent %d to servent %d",
										ijAmount - jiAmount, i, j);
								AppConfig.timestampedStandardPrint(outputString);
								sum += ijAmount - jiAmount;
							}
						}
					}
				}
			}

			AppConfig.timestampedStandardPrint("System bitcake count: " + sum);

			regionResult = null; //reset for next invocation
			collecting.set(false);
		}

	}
	
	@Override
	public void addLYSnapshotInfo(int id, LYSnapshotResult lySnapshotResult) {

	}
	
	@Override
	public void startCollecting() {
		if (collecting.get()) {
			AppConfig.timestampedErrorPrint("Tried to start collecting before finished with previous.");
		} else {
			snapshotNo.getAndIncrement();
			collecting.set(true);
		}
	}
	
	@Override
	public void stop() {
		working = false;
	}

	@Override
	public void setRegionResult(RegionResult regionResult) {
		this.regionResult = regionResult;
	}

	@Override
	public void endCollecting() {
		collecting.set(false);
		waiting = false;
	}
}
