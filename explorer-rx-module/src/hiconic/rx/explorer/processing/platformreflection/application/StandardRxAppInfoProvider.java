package hiconic.rx.explorer.processing.platformreflection.application;

import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.stream.Collectors;

import com.braintribe.cfg.Configurable;
import com.braintribe.cfg.Required;
import com.braintribe.common.lcd.Numbers;
import com.braintribe.utils.FileTools;
import com.braintribe.utils.FolderSize;
import com.braintribe.utils.stream.pools.CompoundBlockPool;
import com.braintribe.utils.stream.stats.BlockKind;
import com.braintribe.utils.stream.stats.StreamPipeBlockStats;

import hiconic.rx.module.api.wire.RxPlatformContract;
import hiconic.rx.module.api.wire.RxPlatformResourcesContract;
import hiconic.rx.reflection.model.application.RxAppInfo;
import hiconic.rx.reflection.model.streampipes.PoolKind;
import hiconic.rx.reflection.model.streampipes.StreamPipeBlocksInfo;
import hiconic.rx.reflection.model.streampipes.StreamPipesInfo;
import hiconic.rx.reflection.model.system.disk.FolderInfo;

/**
 * @author peter.gazdik
 */
public class StandardRxAppInfoProvider implements Supplier<RxAppInfo> {

	private RxPlatformContract platform;
	private RxPlatformResourcesContract platformResources;

	private CompoundBlockPool compoundBlockPool;

	@Required
	public void setPlatformContract(RxPlatformContract platform) {
		this.platform = platform;
	}

	@Required
	public void setPlatformResourcesContract(RxPlatformResourcesContract platformResources) {
		this.platformResources = platformResources;
	}

	@Configurable
	public void setCompoundBlockPool(CompoundBlockPool compoundBlockPool) {
		this.compoundBlockPool = compoundBlockPool;
	}

	@Override
	public RxAppInfo get() {
		RxAppInfo result = RxAppInfo.T.create();
		result.setApplicationName(platform.applicationName());
		result.setApplicationId(platform.applicationId());
		result.setStreamPipeInfo(prepareStreamPipeBlocksInfo());
		result.setTempDirInfo(createFolderInfo(platformResources.tmpPath()));

		return result;
	}

	private StreamPipesInfo prepareStreamPipeBlocksInfo() {
		if (compoundBlockPool == null)
			return null;

		StreamPipesInfo streamPipesInfo = StreamPipesInfo.T.create();
		List<StreamPipeBlockStats> stats = compoundBlockPool.calculateStats();
		Map<BlockKind, StreamPipeBlockStats> groupedBlockStats = stats.stream() //
				.collect(Collectors.toMap( //
						StreamPipeBlockStats::getBlockKind, //
						Function.identity(), //
						StreamPipeBlockStats::merge //
				));

		StreamPipeBlockStats total = groupedBlockStats.values().stream() //
				.reduce(StreamPipeBlockStats::merge) //
				.orElse(null);

		List<StreamPipeBlocksInfo> poolList = stats.stream() //
				.map(this::toBlocksInfo) //
				.collect(Collectors.toList());

		streamPipesInfo.setFileBlocks(toBlocksInfo(groupedBlockStats.get(BlockKind.file)));
		streamPipesInfo.setInMemoryBlocks(toBlocksInfo(groupedBlockStats.get(BlockKind.inMemory)));
		streamPipesInfo.setTotal(toBlocksInfo(total));
		streamPipesInfo.setPoolList(poolList);

		return streamPipesInfo;
	}

	private StreamPipeBlocksInfo toBlocksInfo(StreamPipeBlockStats stats) {
		if (stats == null)
			return null;

		StreamPipeBlocksInfo blocksInfo = StreamPipeBlocksInfo.T.create();
		int mbTotal = toMb(stats.getBytesTotal());
		int unused = toMb(stats.getBytesUnused());
		int mbAllocatable = toMb(stats.getMaxBytesAllocatable());

		PoolKind poolKind = stats.getPoolKind() == null ? null : PoolKind.valueOf(stats.getPoolKind().name());

		blocksInfo.setMbTotal(mbTotal);
		blocksInfo.setMbUnused(unused);
		blocksInfo.setMbAllocatable(mbAllocatable);
		blocksInfo.setNumTotal(stats.getNumTotal());
		blocksInfo.setNumUnused(stats.getNumUnused());
		blocksInfo.setNumMax(stats.getMaxBlocksAllocatable());
		blocksInfo.setBlockSize(stats.getBlockSize());
		blocksInfo.setLocation(stats.getLocation());
		blocksInfo.setPoolKind(poolKind);
		blocksInfo.setInMemory(BlockKind.inMemory == stats.getBlockKind());
		return blocksInfo;
	}

	private static int toMb(long value) {
		return value < 0 //
				? -1 //
				: (int) Math.min(Integer.MAX_VALUE, value / Numbers.MEBIBYTE);
	}

	private static FolderInfo createFolderInfo(Path path) {
		FolderInfo folderInfo = FolderInfo.T.create();
		folderInfo.setPath(path.toAbsolutePath().toString());

		FolderSize folderSize = FileTools.getFolderSize(path);
		folderInfo.setSize(folderSize.getSize());
		folderInfo.setNumFiles(folderSize.getNumFiles());
		folderInfo.setNumFolders(folderSize.getNumFolders());
		folderInfo.setNumSymlinks(folderSize.getNumSymlinks());
		folderInfo.setNumOthers(folderSize.getNumOthers());
		return folderInfo;
	}

}
