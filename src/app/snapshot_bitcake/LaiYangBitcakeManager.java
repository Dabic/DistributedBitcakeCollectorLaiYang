package app.snapshot_bitcake;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.BiFunction;

import app.AppConfig;
import app.ServentInfo;
import app.snapshot_bitcake.customInfo.*;
import servent.message.Message;
import servent.message.snapshot.InitiatorResultMessage;
import servent.message.snapshot.LYMarkerMessage;
import servent.message.snapshot.ResultMessage;
import servent.message.util.MessageUtil;

public class LaiYangBitcakeManager implements BitcakeManager {

    private final AtomicInteger currentAmount = new AtomicInteger(1000);

    public void takeSomeBitcakes(int amount) {
        currentAmount.getAndAdd(-amount);
    }

    public void addSomeBitcakes(int amount) {
        currentAmount.getAndAdd(amount);
    }

    public int getCurrentBitcakeAmount() {
        return currentAmount.get();
    }

    private Map<Integer, Integer> giveHistory = new ConcurrentHashMap<>();
    private Map<Integer, Integer> getHistory = new ConcurrentHashMap<>();

    private RegionInfo regionInfo;
    private SnapshotInfoStatus snapshotInfoStatus;
    private SnapshotCollector snapshotCollector;
    private Map<Integer, Boolean> otherInitiators = new ConcurrentHashMap<>();
    private Set<SnapshotInfo> alreadyProcessed = new HashSet<>();
    private final Object otherLock = new Object();

    public LaiYangBitcakeManager() {
        /*
         * Every bitcake manager has unique regionInfo
         * Every bitcake manager has its own snapshotInfoStatus
         * Every bitcake manager has its own give and get history
         */
        regionInfo = new RegionInfo();
        List<SnapshotInfo> snapshotInfos = new ArrayList<>();
        AppConfig.getInitiators()
                .forEach(serventInfo -> snapshotInfos.add(new SnapshotInfo(serventInfo.getId(), 0)));
        snapshotInfoStatus = new SnapshotInfoStatus(snapshotInfos);

        for (Integer neighbor : AppConfig.myServentInfo.getNeighbors()) {
            giveHistory.put(neighbor, 0);
            getHistory.put(neighbor, 0);
        }
    }

    public void markerEvent(SnapshotInfo snapshotInfo, SnapshotCollector snapshotCollector) {
        synchronized (AppConfig.colorLock) {
            this.snapshotCollector = snapshotCollector;
            if (snapshotInfo.getInitiatorId() == AppConfig.myServentInfo.getId()) {
                AppConfig.timestampedErrorPrint("new master " + AppConfig.myServentInfo);
                regionInfo.setMaster(AppConfig.myServentInfo);
                regionInfo.setCurrentSnapshot(snapshotInfo);
                snapshotInfoStatus = snapshotInfoStatus.changeSnapshotStatus(snapshotInfo);
            }
            for (Integer neighbor : AppConfig.myServentInfo.getNeighbors()) {
                if (neighbor == getMaster().getId())
                    continue;
                regionInfo.addChild(AppConfig.getInfoById(neighbor));
                Message clMarker = new LYMarkerMessage(
                        AppConfig.myServentInfo,
                        AppConfig.getInfoById(neighbor),
                        snapshotInfo.getInitiatorId(),
                        snapshotInfoStatus);
                MessageUtil.sendMessage(clMarker);
                try {
                    /*
                     * This sleep is here to artificially produce some white node -> red node messages.
                     * Not actually recommended, as we are sleeping while we have colorLock.
                     */
                    Thread.sleep(100);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    public void sendResult() {
        synchronized (AppConfig.colorLock) {
            int recordedAmount = getCurrentBitcakeAmount();
            regionInfo.getRegionResult().addResult(AppConfig.myServentInfo.getId(), new LYSnapshotResult(AppConfig.myServentInfo.getId(), recordedAmount, giveHistory, getHistory));
            if (regionInfo.getMaster().getId() == AppConfig.myServentInfo.getId()) {
                // dobio sam sve rezoltove
                AppConfig.timestampedErrorPrint("ALL RESULTS: " + regionInfo.getRegionResult().getResults());
                AppConfig.timestampedErrorPrint("Already1: " + alreadyProcessed);
                for (SnapshotInfo snapshotInfo : regionInfo.getRegionResult().getSnapshotInfos()) {
                    if (snapshotInfo.getInitiatorId() != AppConfig.myServentInfo.getId() && !alreadyContains(snapshotInfo)) {
                        otherInitiators.put(snapshotInfo.getInitiatorId(), false);
                        alreadyProcessed.add(snapshotInfo);
                    }
                }
                AppConfig.timestampedErrorPrint("Other snapshots: " + otherInitiators);
                AppConfig.timestampedErrorPrint("Already2: " + alreadyProcessed);
                if (otherInitiators.size() == 0) {
                    snapshotCollector.setRegionResult(regionInfo.getRegionResult());
                    regionInfo = new RegionInfo();
                    otherInitiators = new ConcurrentHashMap<>();
                } else {
                    Thread shareResultsThread = new Thread(new Runnable() {
                        @Override
                        public void run() {
                            boolean working = true;
                            while (working) {
                                boolean shouldStop = true;
                                synchronized (otherLock) {
                                    for (Map.Entry<Integer, Boolean> entry : otherInitiators.entrySet()) {
                                        if (!entry.getValue()) {
                                            shouldStop = false;
                                            break;
                                        }
                                    }
                                }
                                if (shouldStop) {
                                    working = false;
                                    continue;
                                }
                                try {
                                    Thread.sleep(1000);
                                } catch (InterruptedException e) {
                                    e.printStackTrace();
                                }
                                AppConfig.timestampedErrorPrint("RADIM");
                                for (SnapshotInfo snapshotInfo : regionInfo.getRegionResult().getSnapshotInfos()) {
                                    Message message = new InitiatorResultMessage(
                                            AppConfig.myServentInfo,
                                            AppConfig.getInfoById(snapshotInfo.getInitiatorId()),
                                            regionInfo.getRegionResult(),
                                            true
                                    );
                                    MessageUtil.sendMessage(message);
                                }
                            }
                            AppConfig.timestampedErrorPrint("ZAVRSIO");
                            snapshotCollector.setRegionResult(regionInfo.getRegionResult());
                            regionInfo = new RegionInfo();
                            otherInitiators = new ConcurrentHashMap<>();
                        }
                    });
                    shareResultsThread.start();
                }
            } else {
                Message resultMessage = new ResultMessage(AppConfig.myServentInfo, regionInfo.getMaster(), regionInfo.getRegionResult());
                MessageUtil.sendMessage(resultMessage);
                Thread delay = new Thread(new Runnable() {
                    @Override
                    public void run() {
                        try {
                            Thread.sleep(4000);
                        } catch (InterruptedException e) {
                            e.printStackTrace();
                        }
                        regionInfo = new RegionInfo();
                    }
                });
                delay.start();
            }
        }
    }


    private class MapValueUpdater implements BiFunction<Integer, Integer, Integer> {

        private int valueToAdd;

        public MapValueUpdater(int valueToAdd) {
            this.valueToAdd = valueToAdd;
        }

        @Override
        public Integer apply(Integer key, Integer oldValue) {
            return oldValue + valueToAdd;
        }
    }

    public void recordGiveTransaction(int neighbor, int amount) {
        giveHistory.compute(neighbor, new MapValueUpdater(amount));
    }

    public void recordGetTransaction(int neighbor, int amount) {
        getHistory.compute(neighbor, new MapValueUpdater(amount));
    }

    public ServentInfo getMaster() {
        return this.regionInfo.getMaster();
    }

    public void removeChild(ServentInfo serventInfo) {
        this.regionInfo.removeChild(serventInfo);
    }

    public List<ServentInfo> getChildren() {
        return this.regionInfo.getChildren();
    }

    public void setMasterForSnapshot(ServentInfo master, SnapshotInfo snapshotInfo) {
        this.regionInfo.setMaster(master);
        this.regionInfo.setCurrentSnapshot(snapshotInfo);
        changeCurrentSnapshotInInfo(snapshotInfo);
        AppConfig.timestampedErrorPrint("new master " + master);
    }

    public SnapshotInfo getCurrentSnapshot() {
        return this.regionInfo.getCurrentSnapshot();
    }

    public SnapshotInfo getSnapshotIfDifferent(SnapshotInfoStatus other) {
        for (SnapshotInfo snapshotInfo : other.getSnapshotInfos()) {
            if (!snapshotInfoStatus.getSnapshotInfos().contains(snapshotInfo))
                return snapshotInfo;
        }
        return null;
    }

    public SnapshotInfo getCurrentSnapshotInfoById(int id) {
        for (SnapshotInfo snapshotInfo : snapshotInfoStatus.getSnapshotInfos()) {
            if (snapshotInfo.getInitiatorId() == id)
                return snapshotInfo;
        }
        return null;
    }

    public void changeCurrentSnapshotInInfo(SnapshotInfo newSnapshotInfo) {
        for (SnapshotInfo snapshotInfo : snapshotInfoStatus.getSnapshotInfos()) {
            if (snapshotInfo.getInitiatorId() == newSnapshotInfo.getInitiatorId()) {
                snapshotInfo.setSequenceNo(newSnapshotInfo.getSequenceNo());
            }
        }
    }

    public RegionInfo getRegionInfo() {
        return regionInfo;
    }

    public Map<Integer, Boolean> getOtherInitiators() {
        return otherInitiators;
    }

    public Object getOtherLock() {
        return otherLock;
    }

    public boolean alreadyContains(SnapshotInfo snapshotInfo) {
        for (SnapshotInfo snp : alreadyProcessed) {
            if (snp.getInitiatorId() == snapshotInfo.getInitiatorId() && snp.getSequenceNo() == snapshotInfo.getSequenceNo()) {
                return true;
            }
        }
        return false;
    }

    public Set<SnapshotInfo> getAlreadyProcessed() {
        return alreadyProcessed;
    }
}
