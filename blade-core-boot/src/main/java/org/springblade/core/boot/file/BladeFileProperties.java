package org.springblade.core.boot.file;

import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import jakarta.annotation.PostConstruct;

/**
 * 文件上传配置属性
 * 统一配置：所有文件类型使用相同的规则
 *
 * @author Blade
 */
@Slf4j
@Data
@Component
@ConfigurationProperties(prefix = "blade.file")
public class BladeFileProperties {

	/**
	 * 是否启用增强功能（加密、压缩、数据库存储）
	 */
	private boolean enabled = true;

	/**
	 * 统一上传目录
	 */
	private String uploadDir = "upload";

	/**
	 * 是否ZIP压缩（所有文件统一）
	 */
	private boolean zipEnabled = false;

	/**
	 * 是否AES加密（所有文件统一）
	 */
	private boolean encryptEnabled = false;

	/**
	 * 是否保存到ImageFile数据库表（所有文件统一）
	 */
	private boolean saveToDb = false;

	/**
	 * 图片尺寸压缩（仅对图片类型生效）
	 */
	private boolean imageCompress = true;

	@PostConstruct
	public void init() {
		BladeFile.setProperties(this);
		log.info("BladeFile配置已加载: enabled={}, zipEnabled={}, encryptEnabled={}, saveToDb={}, imageCompress={}",
			enabled, zipEnabled, encryptEnabled, saveToDb, imageCompress);
	}

}
