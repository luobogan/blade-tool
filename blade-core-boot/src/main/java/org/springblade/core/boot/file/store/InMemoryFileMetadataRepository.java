package org.springblade.core.boot.file.store;

import lombok.extern.slf4j.Slf4j;

import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 内存元数据仓储（服务未配置数据源时的降级实现）
 *
 * @author Blade
 */
@Slf4j
public class InMemoryFileMetadataRepository implements FileMetadataRepository {

	private final Map<Long, FileMetadata> store = new ConcurrentHashMap<>();

	@Override
	public Long save(FileMetadata metadata) {
		store.put(metadata.getId(), metadata);
		log.debug("元数据已写入内存仓储: fileId={}", metadata.getId());
		return metadata.getId();
	}

	@Override
	public Optional<FileMetadata> findById(Long id) {
		if (id == null) {
			return Optional.empty();
		}
		return Optional.ofNullable(store.get(id));
	}

	@Override
	public boolean deleteById(Long id) {
		return id != null && store.remove(id) != null;
	}

}