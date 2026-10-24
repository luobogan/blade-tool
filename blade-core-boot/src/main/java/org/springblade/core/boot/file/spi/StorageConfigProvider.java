package org.springblade.core.boot.file.spi;

import org.springblade.core.boot.file.enums.FileStorageType;
import org.springblade.core.oss.OssTemplate;

/**
 * 存储配置来源（可插拔）
 *
 * <p>用于让「统一上传/下载组件」按存储类型适配配置来源：</p>
 * <ul>
 *     <li>本地存储：由 {@link #imageDir()} / {@link #fileDir()} / {@link #backupDir()} /
 *         {@link #backupCycle()} / {@link #zipEnabled()} / {@link #encryptEnabled()} 提供目录与开关；</li>
 *     <li>第三方存储：由 {@link #ossTemplate()} 提供该类型对应的 {@link OssTemplate}
 *         （七牛 / 阿里云 / MinIO 等各自的实现）。</li>
 * </ul>
 *
 * <p>各方法返回 {@code null} 表示该实现不关心此项，由组件回退到
 * {@code blade.file.storage.*} 配置（{@link org.springblade.core.boot.file.props.FileStorageProperties}）。</p>
 *
 * <p>默认实现见 {@link DefaultStorageConfigProvider}（读配置中心）；
 * 业务模块可注册自己的实现（如从数据库配置表读取），通过
 * {@code @ConditionalOnMissingBean} 自动替代默认实现。</p>
 *
 * @author Blade
 */
public interface StorageConfigProvider {

	/**
	 * 默认存储方式：LOCAL / OSS；null 表示回退配置
	 */
	FileStorageType defaultType();

	/**
	 * 图片存放目录（本地存储生效）；null 表示回退配置
	 */
	String imageDir();

	/**
	 * 文件存放目录（本地存储生效）；null 表示回退配置
	 */
	String fileDir();

	/**
	 * 文件备份目录（本地存储生效）；null 表示回退配置
	 */
	String backupDir();

	/**
	 * 文件备份周期（分钟）；null 表示回退配置
	 */
	Integer backupCycle();

	/**
	 * 是否默认 ZIP 压缩；null 表示回退配置
	 */
	Boolean zipEnabled();

	/**
	 * 是否默认 AES 加密；null 表示回退配置
	 */
	Boolean encryptEnabled();

	/**
	 * 第三方存储对应的模板（七牛/阿里/MinIO 等）；null 表示不可用，自动降级本地
	 */
	OssTemplate ossTemplate();

}
