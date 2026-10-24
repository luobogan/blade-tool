package org.springblade.core.boot.file.store;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.ObjectProvider;

import javax.sql.DataSource;
import java.util.Optional;

/**
 * 延迟解析的元数据仓储
 *
 * <p>首次使用时才向容器索取 DataSource：有数据源则落 imagefile 表，
 * 无数据源（如纯资源服务）则降级为内存实现。避免自动装配阶段的 Bean 注册顺序问题。</p>
 *
 * @author Blade
 */
@Slf4j
public class LazyFileMetadataRepository implements FileMetadataRepository {

	private final ObjectProvider<DataSource> dataSourceProvider;

	private volatile FileMetadataRepository delegate;

	public LazyFileMetadataRepository(ObjectProvider<DataSource> dataSourceProvider) {
		this.dataSourceProvider = dataSourceProvider;
	}

	private FileMetadataRepository delegate() {
		FileMetadataRepository current = delegate;
		if (current != null) {
			return current;
		}
		synchronized (this) {
			if (delegate == null) {
				DataSource dataSource = dataSourceProvider.getIfAvailable();
				if (dataSource != null) {
					delegate = new JdbcFileMetadataRepository(dataSource);
					log.info("附件元数据仓储：使用 imagefile 表");
				} else {
					delegate = new InMemoryFileMetadataRepository();
					log.warn("附件元数据仓储：未找到数据源，降级为内存实现（重启后元数据丢失）");
				}
			}
			return delegate;
		}
	}

	@Override
	public Long save(FileMetadata metadata) {
		return delegate().save(metadata);
	}

	@Override
	public Optional<FileMetadata> findById(Long id) {
		return delegate().findById(id);
	}

	@Override
	public boolean deleteById(Long id) {
		return delegate().deleteById(id);
	}

}