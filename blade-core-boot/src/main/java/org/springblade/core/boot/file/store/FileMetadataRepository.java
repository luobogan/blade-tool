package org.springblade.core.boot.file.store;

import java.util.Optional;

/**
 * 附件元数据仓储（默认落 ImageFile 表；无数据源时降级为内存实现）
 *
 * @author Blade
 */
public interface FileMetadataRepository {

	/**
	 * 保存元数据
	 *
	 * @param metadata 元数据
	 * @return 附件ID
	 */
	Long save(FileMetadata metadata);

	/**
	 * 按ID查询元数据
	 *
	 * @param id 附件ID
	 * @return 元数据
	 */
	Optional<FileMetadata> findById(Long id);

	/**
	 * 按ID删除元数据
	 *
	 * @param id 附件ID
	 * @return 是否删除成功
	 */
	boolean deleteById(Long id);

}