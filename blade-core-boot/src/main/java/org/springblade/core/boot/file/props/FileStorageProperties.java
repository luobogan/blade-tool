package org.springblade.core.boot.file.props;

import lombok.Data;
import org.springblade.core.boot.file.enums.FileStorageType;
import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.List;

/**
 * 通用上传组件配置（blade.file.storage.*）
 *
 * <p>与框架自带的 {@code blade.file.*}（BladeFileProperties）并存：
 * 前者控制「是否启用增强能力」，本类控制「通用上传组件」的默认行为与可选项。</p>
 *
 * @author Blade
 */
@Data
@ConfigurationProperties(prefix = "blade.file.storage")
public class FileStorageProperties {

	/**
	 * 是否启用通用上传组件（关闭后各模块回落到各自原有实现）
	 */
	private boolean enabled = true;

	/**
	 * 默认存储方式：LOCAL / OSS
	 */
	private FileStorageType defaultType = FileStorageType.LOCAL;

	/**
	 * 默认是否打包为 ZIP（前端可覆盖）
	 */
	private boolean defaultNeedZip = false;

	/**
	 * 默认是否 AES 加密（前端可覆盖）
	 */
	private boolean defaultNeedEncrypt = false;

	/**
	 * 默认是否对图片做尺寸压缩（仅图片生效）
	 */
	private boolean defaultImageCompress = true;

	/**
	 * 是否允许前端选择 ZIP 打包
	 */
	private boolean allowZip = true;

	/**
	 * 是否允许前端选择 AES 加密
	 */
	private boolean allowEncrypt = true;

	/**
	 * 是否允许前端选择存储方式（本地/OSS）
	 */
	private boolean allowStorageSwitch = true;

	/**
	 * 单文件大小上限（字节），默认 100MB
	 */
	private long maxSize = 100L * 1024 * 1024;

	/**
	 * 物理根目录（相对路径基于应用工作目录）
	 */
	private String rootPath = System.getProperty("user.dir");

	/**
	 * 上传子目录名（通用目录；未配置 imageDir / fileDir 时的兜底）
	 */
	private String uploadDir = "upload";

	/**
	 * 图片存放目录（本地存储生效；留空回退 uploadDir）
	 */
	private String imageDir = null;

	/**
	 * 文件存放目录（本地存储生效；留空回退 uploadDir）
	 */
	private String fileDir = null;

	/**
	 * 文件备份目录（本地存储生效；留空表示不备份）
	 */
	private String backupDir = null;

	/**
	 * 文件备份周期（分钟），默认 1440 = 24 小时
	 */
	private Integer backupCycle = 1440;

	/**
	 * 允许的扩展名白名单（空 = 不限制），不含点号，如 jpg、png、docx
	 */
	private List<String> allowedExtensions = null;

	/**
	 * OSS 存储桶名（留空 = 使用 oss.bucket-name 配置）
	 */
	private String ossBucketName = null;

	/**
	 * OSS 外网访问域名（留空 = 使用 OssTemplate 生成的外链）
	 */
	private String ossDomain = null;

}
