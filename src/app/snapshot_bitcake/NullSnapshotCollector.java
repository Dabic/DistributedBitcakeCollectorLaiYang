package app.snapshot_bitcake;

import app.snapshot_bitcake.customInfo.RegionResult;

/**
 * This class is used if the user hasn't specified a snapshot type in config.
 * 
 * @author bmilojkovic
 *
 */
public class NullSnapshotCollector implements SnapshotCollector {

	@Override
	public void run() {}

	@Override
	public void stop() {}

	@Override
	public BitcakeManager getBitcakeManager() {
		return null;
	}

	@Override
	public void addLYSnapshotInfo(int id, LYSnapshotResult lySnapshotResult) {}

	@Override
	public void startCollecting() {}

	@Override
	public void setRegionResult(RegionResult regionResult) {

	}

	@Override
	public void endCollecting() {

	}
}
