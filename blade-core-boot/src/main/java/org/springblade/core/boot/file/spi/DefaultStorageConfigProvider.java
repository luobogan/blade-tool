package org.springblade.core.boot.file.spi;

import org.springblade.core.boot.file.enums.FileStorageType;
import org.springblade.core.boot.file.props.FileStorageProperties;
import org.springblade.core.oss.OssTemplate;
import org.springframework.beans.factory.ObjectProvider;

/**
 * 默认存储配置来源：读配置中心 {@code blade.file.storage.*} + 容器中的 {@link OssTemplate}
 *
 * <p>与改造前行为完全一致，保证向后兼容：本地目录用 imageDir / fileDir（留空回退 uploadDir），
 * 第三方用 Spring 容器中由 {@code oss.name} 装配好的唯一 {@link OssTemplate}。</p>
 *
 * @author Blade
 */
public class DefaultStorageConfigProvider implements StorageConfigProvider {

	private final FileStorageProperties properties;
	private final ObjectProvider<OssTemplate> ossTemplateProvider;

	public DefaultStorageConfigProvider(FileStorageProperties properties,
										ObjectProvider<OssTemplate> ossTemplateProvider) {
		this.properties = properties;
		this.ossTemplateProvider = ossTemplateProvider;
	}

	@Override
	public FileStorageType defaultType() {
		return properties.getDefaultType() == null ? FileStorageType.LOCAL : properties.getDefaultType();
	}

	@Override
	public String imageDir() {
		return isBlank(properties.getImageDir()) ? properties.getUploadDir() : properties.getImageDir();
	}

	@Override
	public String fileDir() {
		return isBlank(properties.getFileDir()) ? properties.getUploadDir() : properties.getFileDir();
	}

	@Override
	public String backupDir() {
		return properties.getBackupDir();
	}

	@Override
	public Integer backupCycle() {
		return properties.getBackupCycle();
	}

	@Override
	public Boolean zipEnabled() {
		return properties.isDefaultNeedZip();
	}

	@Override
	public Boolean encryptEnabled() {
		return properties.isDefaultNeedEncrypt();
	}

	@Override
	public OssTemplate ossTemplate() {
		return ossTemplateProvider == null ? null : ossTemplateProvider.getIfAvailable();
	}

	private static boolean isBlank(String value) {
		return value == null || value.trim().isEmpty();
	}

}
