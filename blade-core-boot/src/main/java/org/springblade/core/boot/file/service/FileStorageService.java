package org.springblade.core.boot.file.service;

import lombok.extern.slf4j.Slf4j;
import org.springblade.core.boot.file.dto.FileUploadOptions;
import org.springblade.core.boot.file.enums.FileStorageType;
import org.springblade.core.boot.file.id.SnowflakeIdGenerator;
import org.springblade.core.boot.file.props.FileStorageProperties;
import org.springblade.core.boot.file.spi.StorageConfigProvider;
import org.springblade.core.boot.file.store.FileMetadata;
import org.springblade.core.boot.file.store.FileMetadataRepository;
import org.springblade.core.boot.file.util.FileStorageSupport;
import org.springblade.core.boot.file.vo.FileDownloadContent;
import org.springblade.core.boot.file.vo.FileStorageConfigVO;
import org.springblade.core.boot.file.vo.FileUploadResult;
import org.springblade.core.oss.OssTemplate;
import org.springblade.core.oss.model.BladeFile;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.web.multipart.MultipartFile;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Date;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

/**
 * 通用文件服务实现
 *
 * <p>处理链路：落盘 → 图片压缩（仅图片）→ ZIP 打包（任意类型）→ AES 加密 → OSS 上传（可选）→ 元数据落库。</p>
 *
 * <p>上传与下载采用一致的处理机制：二者都以 {@link FileStorageType} 为准绳分流，
 * 上传「压缩 → 打包 → 加密 → 落存储」，下载「取存储 → 解密 → 解包」，互为逆向且共用同一份元数据。</p>
 *
 * <p>配置按存储类型适配（经 {@link StorageConfigProvider} 可插拔提供）：</p>
 * <ul>
 *     <li>本地存储：使用 imageDir（图片）/ fileDir（其它）分别落盘，并按 zipEnabled / encryptEnabled 决定是否打包、加密；</li>
 *     <li>第三方存储：使用该类型对应的 {@link OssTemplate}（七牛 / 阿里云 / MinIO 等各自实现）上传与取流。</li>
 * </ul>
 *
 * @author Blade
 */
@Slf4j
public class FileStorageService implements IFileStorageService {

	private final FileStorageProperties properties;
	private final FileMetadataRepository repository;
	private final ObjectProvider<OssTemplate> ossTemplateProvider;
	private final StorageConfigProvider configProvider;

	public FileStorageService(FileStorageProperties properties,
							  FileMetadataRepository repository,
							  ObjectProvider<OssTemplate> ossTemplateProvider,
							  StorageConfigProvider configProvider) {
		this.properties = properties;
		this.repository = repository;
		this.ossTemplateProvider = ossTemplateProvider;
		this.configProvider = configProvider;
	}

	@Override
	public FileUploadResult upload(MultipartFile file, FileUploadOptions options) {
		if (file == null || file.isEmpty()) {
			throw new IllegalArgumentException("文件不能为空");
		}
		FileUploadOptions opts = options == null ? new FileUploadOptions() : options;
		String originalName = file.getOriginalFilename();
		validate(originalName, file.getSize(), opts);
		Path target = resolveTargetPath(opts, originalName, isImage(file.getContentType()));
		try {
			Files.createDirectories(target.getParent());
			file.transferTo(target.toFile());
		} catch (IOException e) {
			throw new IllegalStateException("文件写入失败: " + e.getMessage(), e);
		}
		return finalizeStore(target, originalName, file.getContentType(), file.getSize(), opts);
	}

	@Override
	public FileUploadResult store(byte[] content, String fileName, FileUploadOptions options) {
		if (content == null || content.length == 0) {
			throw new IllegalArgumentException("文件内容为空");
		}
		FileUploadOptions opts = options == null ? new FileUploadOptions() : options;
		validate(fileName, content.length, opts);
		Path target = resolveTargetPath(opts, fileName, isImageByExt(fileName));
		try {
			Files.createDirectories(target.getParent());
			Files.write(target, content);
		} catch (IOException e) {
			throw new IllegalStateException("文件写入失败: " + e.getMessage(), e);
		}
		return finalizeStore(target, fileName, null, content.length, opts);
	}

	private FileUploadResult finalizeStore(Path target, String originalName, String contentType,
										   long size, FileUploadOptions options) {
		boolean needZip = resolveNeedZip(options);
		boolean needEncrypt = resolveNeedEncrypt(options);
		boolean imageCompress = resolveImageCompress(options) && isImage(contentType);
		FileStorageType storage = resolveStorage(options);

		if (imageCompress) {
			FileStorageSupport.compressImage(target);
		}

		boolean zipped = false;
		if (needZip) {
			Path zipPath = FileStorageSupport.zipFile(target);
			deleteQuietly(target);
			target = zipPath;
			zipped = true;
		}

		String aesCode = null;
		boolean encrypted = false;
		if (needEncrypt) {
			aesCode = FileStorageSupport.generateAesKey();
			FileStorageSupport.encryptFile(target, aesCode);
			encrypted = true;
		}

		String realPath = target.toString();
		String directUrl = null;
		if (storage == FileStorageType.OSS) {
			OssTemplate template = requireOssTemplate();
			try (InputStream in = Files.newInputStream(target)) {
				BladeFile ossFile = template.putFile(target.getFileName().toString(), in);
				realPath = ossFile.getName();
				directUrl = ossFile.getLink();
			} catch (IOException e) {
				throw new IllegalStateException("对象存储上传失败: " + e.getMessage(), e);
			}
			deleteQuietly(target);
		}

		FileMetadata metadata = new FileMetadata();
		metadata.setId(SnowflakeIdGenerator.nextId());
		metadata.setFilename(originalName);
		metadata.setContentType(contentType);
		metadata.setFilesize(size);
		metadata.setPath(realPath);
		metadata.setStorage(storage.name());
		metadata.setZip(zipped);
		metadata.setEncrypt(encrypted);
		metadata.setAesCode(aesCode);
		metadata.setTenantId(options.getTenantId());
		metadata.setComefrom(options.getComefrom());
		metadata.setStorageStatus(storage == FileStorageType.OSS ? "oss" : "local");
		metadata.setCreatedAt(new Date());
		metadata.setUpdatedAt(new Date());
		Long id = repository.save(metadata);

		FileUploadResult result = new FileUploadResult();
		result.setId(id);
		result.setFilename(originalName);
		result.setContentType(contentType);
		result.setFilesize(size);
		result.setStorage(storage);
		result.setPath(realPath);
		result.setUrl(directUrl != null ? directUrl : ("/file/download/" + id));
		result.setDownloadUrl("/file/download/" + id);
		result.setZip(zipped);
		result.setEncrypt(encrypted);
		result.setAesCode(aesCode);
		result.setTenantId(options.getTenantId());
		log.info("文件上传完成: fileId={}, filename={}, storage={}, zip={}, encrypt={}",
			id, originalName, storage, zipped, encrypted);
		return result;
	}

	@Override
	public FileDownloadContent download(Long fileId) {
		FileMetadata metadata = requireMetadata(fileId);
		byte[] raw;
		if (isOss(metadata)) {
			raw = fetchOss(metadata.getPath());
		} else {
			Path path = Paths.get(metadata.getPath());
			if (!Files.isRegularFile(path)) {
				throw new IllegalArgumentException("文件不存在: " + fileId);
			}
			try {
				raw = Files.readAllBytes(path);
			} catch (IOException e) {
				throw new IllegalStateException("读取文件失败: " + e.getMessage(), e);
			}
		}
		if (metadata.isEncrypt() && metadata.getAesCode() != null) {
			raw = FileStorageSupport.decryptBytes(raw, metadata.getAesCode());
		}
		if (metadata.isZip()) {
			raw = unzipFirstEntry(raw);
		}
		FileDownloadContent content = new FileDownloadContent();
		content.setFilename(metadata.getFilename());
		content.setContentType(metadata.getContentType());
		content.setContent(raw);
		content.setZip(metadata.isZip());
		return content;
	}

	@Override
	public String link(Long fileId) {
		FileMetadata metadata = requireMetadata(fileId);
		if (isOss(metadata)) {
			return requireOssTemplate().fileLink(metadata.getPath());
		}
		return "/file/download/" + fileId;
	}

	@Override
	public java.util.Optional<FileMetadata> metadata(Long fileId) {
		if (fileId == null) {
			return java.util.Optional.empty();
		}
		return repository.findById(fileId);
	}

	@Override
	public boolean delete(Long fileId) {
		FileMetadata metadata = repository.findById(fileId).orElse(null);
		if (metadata == null) {
			return false;
		}
		if (isOss(metadata)) {
			try {
				requireOssTemplate().removeFile(metadata.getPath());
			} catch (Exception e) {
				log.warn("删除对象存储文件失败（忽略）: {}", metadata.getPath(), e);
			}
		} else if (metadata.getPath() != null) {
			FileStorageSupport.deleteQuietly(Paths.get(metadata.getPath()).toFile());
		}
		return repository.deleteById(fileId);
	}

	@Override
	public FileStorageConfigVO config() {
		FileStorageConfigVO config = new FileStorageConfigVO();
		config.setEnabled(properties.isEnabled());
		config.setDefaultStorage(resolveStorage(null).name());
		config.setOssAvailable(configProvider.ossTemplate() != null);
		config.setDefaultNeedZip(properties.isDefaultNeedZip());
		config.setDefaultNeedEncrypt(properties.isDefaultNeedEncrypt());
		config.setAllowZip(properties.isAllowZip());
		config.setAllowEncrypt(properties.isAllowEncrypt());
		config.setAllowStorageSwitch(properties.isAllowStorageSwitch());
		config.setMaxSize(properties.getMaxSize());
		config.setAllowedExtensions(properties.getAllowedExtensions());
		return config;
	}

	// ------------------------------------------------------------------ 内部方法

	private void deleteQuietly(Path path) {
		try {
			Files.deleteIfExists(path);
		} catch (IOException e) {
			log.warn("临时文件删除失败（忽略）: {}", path, e);
		}
	}

	private FileMetadata requireMetadata(Long fileId) {
		if (fileId == null) {
			throw new IllegalArgumentException("附件ID不能为空");
		}
		return repository.findById(fileId)
			.orElseThrow(() -> new IllegalArgumentException("文件不存在: " + fileId));
	}

	private void validate(String fileName, long size, FileUploadOptions options) {
		if (properties.getMaxSize() > 0 && size > properties.getMaxSize()) {
			throw new IllegalArgumentException("文件大小超过限制 " + (properties.getMaxSize() / 1024 / 1024) + "MB");
		}
		List<String> allowed = properties.getAllowedExtensions();
		if (allowed != null && !allowed.isEmpty()) {
			String ext = FileStorageSupport.extensionOf(fileName);
			String pure = ext.startsWith(".") ? ext.substring(1) : ext;
			boolean hit = allowed.stream().anyMatch(item -> {
				String target = item.startsWith(".") ? item.substring(1) : item;
				return target.equalsIgnoreCase(pure);
			});
			if (!hit) {
				throw new IllegalArgumentException("不支持的文件类型: " + pure);
			}
		}
		if (Boolean.TRUE.equals(options.getNeedZip()) && !properties.isAllowZip()) {
			throw new IllegalArgumentException("当前服务未开放 ZIP 打包选项");
		}
		if (Boolean.TRUE.equals(options.getNeedEncrypt()) && !properties.isAllowEncrypt()) {
			throw new IllegalArgumentException("当前服务未开放 AES 加密选项");
		}
		if (options.getStorage() == FileStorageType.OSS && !properties.isAllowStorageSwitch()) {
			throw new IllegalArgumentException("当前服务未开放存储方式切换");
		}
	}

	/**
	 * 解析落盘目标路径：按是否图片分流到 imageDir / fileDir（均留空时回退 uploadDir）
	 */
	private Path resolveTargetPath(FileUploadOptions options, String originalName, boolean image) {
		String tenantId = options.getTenantId() == null ? "" : options.getTenantId();
		String dir = image
			? firstNonBlank(configProvider.imageDir(), properties.getImageDir(), properties.getUploadDir())
			: firstNonBlank(configProvider.fileDir(), properties.getFileDir(), properties.getUploadDir());
		Path root;
		if (isAbsolutePath(dir)) {
			// 目录为绝对路径（如 D:/data/oss、/data/oss）：直接作为根目录，不再拼接 rootPath
			root = Paths.get(dir).resolve(FileStorageSupport.buildDateDir(null, tenantId, options.getDir()));
		} else {
			root = Paths.get(properties.getRootPath(), FileStorageSupport.buildDateDir(dir, tenantId, options.getDir()));
		}
		return root.resolve(FileStorageSupport.buildFileName(originalName));
	}

	/**
	 * 判断是否为绝对路径：Windows 盘符（D:/xxx、D:\xxx）或 Unix 根路径（/xxx）
	 */
	private static boolean isAbsolutePath(String path) {
		if (path == null || path.isBlank()) {
			return false;
		}
		String normalized = path.replace('\\', '/');
		if (normalized.startsWith("/")) {
			return true;
		}
		return normalized.length() >= 2 && Character.isLetter(normalized.charAt(0)) && normalized.charAt(1) == ':';
	}

	/**
	 * 解析存储方式：请求指定 → 配置来源（本地/第三方）→ 配置中心；第三方无可用模板时降级本地
	 */
	private FileStorageType resolveStorage(FileUploadOptions options) {
		if (options != null && options.getStorage() != null) {
			if (options.getStorage() == FileStorageType.OSS && configProvider.ossTemplate() == null) {
				throw new IllegalStateException("对象存储未配置，无法使用 OSS 存储");
			}
			return options.getStorage();
		}
		FileStorageType type = configProvider.defaultType();
		if (type == null) {
			type = properties.getDefaultType() == null ? FileStorageType.LOCAL : properties.getDefaultType();
		}
		if (type == FileStorageType.OSS && configProvider.ossTemplate() == null) {
			log.warn("默认存储方式为 OSS 但未配置对象存储，自动降级为本地存储");
			return FileStorageType.LOCAL;
		}
		return type;
	}

	private boolean resolveNeedZip(FileUploadOptions options) {
		if (options != null && options.getNeedZip() != null) {
			return options.getNeedZip() && properties.isAllowZip();
		}
		Boolean fromProvider = configProvider.zipEnabled();
		boolean def = fromProvider != null ? fromProvider : properties.isDefaultNeedZip();
		return def && properties.isAllowZip();
	}

	private boolean resolveNeedEncrypt(FileUploadOptions options) {
		if (options != null && options.getNeedEncrypt() != null) {
			return options.getNeedEncrypt() && properties.isAllowEncrypt();
		}
		Boolean fromProvider = configProvider.encryptEnabled();
		boolean def = fromProvider != null ? fromProvider : properties.isDefaultNeedEncrypt();
		return def && properties.isAllowEncrypt();
	}

	private boolean resolveImageCompress(FileUploadOptions options) {
		if (options != null && options.getImageCompress() != null) {
			return options.getImageCompress();
		}
		return properties.isDefaultImageCompress();
	}

	private boolean isImage(String contentType) {
		return contentType != null && contentType.startsWith("image/");
	}

	private boolean isImageByExt(String fileName) {
		if (fileName == null) {
			return false;
		}
		String lower = fileName.toLowerCase();
		return lower.endsWith(".jpg") || lower.endsWith(".jpeg") || lower.endsWith(".png")
			|| lower.endsWith(".gif") || lower.endsWith(".bmp") || lower.endsWith(".webp")
			|| lower.endsWith(".svg");
	}

	private boolean isOss(FileMetadata metadata) {
		return FileStorageType.OSS.name().equalsIgnoreCase(metadata.getStorage())
			|| "oss".equalsIgnoreCase(metadata.getStorageStatus());
	}

	private OssTemplate requireOssTemplate() {
		OssTemplate template = configProvider.ossTemplate();
		if (template == null) {
			throw new IllegalStateException("对象存储未配置，无法使用 OSS 存储");
		}
		return template;
	}

	private byte[] fetchOss(String objectKey) {
		OssTemplate template = requireOssTemplate();
		String link = template.fileLink(objectKey);
		try (HttpClient client = HttpClient.newHttpClient()) {
			HttpRequest request = HttpRequest.newBuilder(URI.create(link)).GET().build();
			HttpResponse<byte[]> response = client.send(request, HttpResponse.BodyHandlers.ofByteArray());
			if (response.statusCode() / 100 != 2) {
				throw new IllegalStateException("读取对象存储文件失败: HTTP " + response.statusCode());
			}
			return response.body();
		} catch (InterruptedException e) {
			Thread.currentThread().interrupt();
			throw new IllegalStateException("读取对象存储文件被中断", e);
		} catch (Exception e) {
			throw new IllegalStateException("读取对象存储文件失败: " + e.getMessage(), e);
		}
	}

	private byte[] unzipFirstEntry(byte[] zipBytes) {
		try (ZipInputStream zis = new ZipInputStream(new ByteArrayInputStream(zipBytes))) {
			ZipEntry entry = zis.getNextEntry();
			if (entry == null) {
				return zipBytes;
			}
			java.io.ByteArrayOutputStream baos = new java.io.ByteArrayOutputStream(zipBytes.length);
			byte[] buffer = new byte[8192];
			int len;
			while ((len = zis.read(buffer)) != -1) {
				baos.write(buffer, 0, len);
			}
			zis.closeEntry();
			return baos.toByteArray();
		} catch (IOException e) {
			throw new IllegalStateException("解压附件失败: " + e.getMessage(), e);
		}
	}

	private static String firstNonBlank(String... values) {
		if (values == null) {
			return null;
		}
		for (String value : values) {
			if (value != null && !value.trim().isEmpty()) {
				return value;
			}
		}
		return null;
	}

}
