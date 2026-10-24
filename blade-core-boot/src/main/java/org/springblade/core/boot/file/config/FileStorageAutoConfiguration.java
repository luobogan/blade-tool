package org.springblade.core.boot.file.config;

import org.springblade.core.boot.file.service.FileStorageService;
import org.springblade.core.boot.file.service.IFileStorageService;
import org.springblade.core.boot.file.controller.CommonFileController;
import org.springblade.core.boot.file.props.FileStorageProperties;
import org.springblade.core.boot.file.spi.DefaultStorageConfigProvider;
import org.springblade.core.boot.file.spi.StorageConfigProvider;
import org.springblade.core.boot.file.store.FileMetadataRepository;
import org.springblade.core.boot.file.store.LazyFileMetadataRepository;
import org.springblade.core.oss.OssTemplate;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;

import javax.sql.DataSource;

/**
 * 通用上传组件自动装配
 *
 * <p>各业务服务只要引入 blade-file 依赖即自动获得：</p>
 * <ul>
 *     <li>{@code IFileStorageService} —— 统一上传/下载服务</li>
 *     <li>{@code /file/**} 端点 —— 统一上传/下载 HTTP 接口</li>
 *     <li>元数据仓储 —— 有数据源时落 imagefile 表，无数据源时降级为内存实现</li>
 * </ul>
 *
 * @author Blade
 */
@AutoConfiguration
@EnableConfigurationProperties(FileStorageProperties.class)
public class FileStorageAutoConfiguration {

	/**
	 * 元数据仓储：首次使用时解析数据源，有则落 imagefile 表，无则降级内存实现
	 */
	@Bean
	@ConditionalOnMissingBean
	public FileMetadataRepository fileMetadataRepository(ObjectProvider<DataSource> dataSourceProvider) {
		return new LazyFileMetadataRepository(dataSourceProvider);
	}

	/**
	 * 存储配置来源：默认读配置中心 blade.file.storage.* + 容器中的 OssTemplate。
	 * 业务模块注册自己的实现（例如从 blade_oss 配置表读取启用配置）后，本默认实现自动让位。
	 */
	@Bean
	@ConditionalOnMissingBean
	public StorageConfigProvider storageConfigProvider(FileStorageProperties properties,
													   ObjectProvider<OssTemplate> ossTemplateProvider) {
		return new DefaultStorageConfigProvider(properties, ossTemplateProvider);
	}

	/**
	 * 统一文件服务
	 */
	@Bean
	@ConditionalOnMissingBean
	public IFileStorageService fileStorageService(FileStorageProperties properties,
												   ObjectProvider<FileMetadataRepository> repositoryProvider,
												   ObjectProvider<OssTemplate> ossTemplateProvider,
												   ObjectProvider<StorageConfigProvider> configProvider) {
		StorageConfigProvider provider = configProvider.getIfAvailable();
		if (provider == null) {
			provider = new DefaultStorageConfigProvider(properties, ossTemplateProvider);
		}
		return new FileStorageService(properties, repositoryProvider.getIfAvailable(), ossTemplateProvider, provider);
	}

	/**
	 * 统一上传/下载端点（仅 Servlet 应用注册，网关等响应式服务不注册）
	 */
	@Bean
	@ConditionalOnMissingBean
	@ConditionalOnWebApplication(type = ConditionalOnWebApplication.Type.SERVLET)
	public CommonFileController commonFileController(IFileStorageService fileStorageService) {
		return new CommonFileController(fileStorageService);
	}

}