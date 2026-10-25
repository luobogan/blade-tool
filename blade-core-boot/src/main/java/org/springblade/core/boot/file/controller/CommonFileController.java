package org.springblade.core.boot.file.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springblade.core.boot.file.service.IFileStorageService;
import org.springblade.core.boot.file.dto.FileUploadOptions;
import org.springblade.core.boot.file.enums.FileStorageType;
import org.springblade.core.boot.file.vo.FileDownloadContent;
import org.springblade.core.boot.file.vo.FileStorageConfigVO;
import org.springblade.core.boot.file.vo.FileUploadResult;
import org.springblade.core.tool.api.R;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import jakarta.servlet.http.HttpServletRequest;

/**
 * 通用上传/下载端点（所有业务服务同构暴露）
 *
 * <ul>
 *     <li>POST /file/upload —— 上传，支持 needZip / needEncrypt / storage 三个可配置项</li>
 *     <li>GET  /file/download/{fileId} —— 统一下载（自动解密、自动解 ZIP）</li>
 *     <li>GET  /file/link/{fileId} —— 获取访问地址</li>
 *     <li>GET  /file/config —— 获取上传能力配置（前端渲染上传选项面板）</li>
 *     <li>POST /file/delete/{fileId} —— 删除附件</li>
 * </ul>
 *
 * @author Blade
 */
@RestController
@RequestMapping("/file")
@Tag(name = "通用文件", description = "统一上传/下载（本地/OSS、ZIP 压缩、AES 加密）")
public class CommonFileController {

	private final IFileStorageService fileStorageService;

	public CommonFileController(IFileStorageService fileStorageService) {
		this.fileStorageService = fileStorageService;
	}

	@PostMapping("/upload")
	@Operation(summary = "上传文件", description = "needZip=打包ZIP，needEncrypt=AES加密，storage=LOCAL/OSS；为空则用服务端默认")
	public R<FileUploadResult> upload(@RequestParam("file") MultipartFile file,
									  @RequestParam(required = false) String dir,
									  @RequestParam(required = false) FileStorageType storage,
									  @RequestParam(required = false) Boolean needZip,
									  @RequestParam(required = false) Boolean needEncrypt,
									  @RequestParam(required = false) String comefrom,
									  HttpServletRequest request) {
		FileUploadOptions options = new FileUploadOptions()
			.dir(dir == null || dir.isBlank() ? "common" : dir)
			.storage(storage)
			.needZip(needZip)
			.needEncrypt(needEncrypt)
			.comefrom(comefrom)
			.tenantId(tenantId(request));
		return R.data(fileStorageService.upload(file, options), "上传成功");
	}

	@GetMapping("/download/{fileId}")
	@Operation(summary = "下载文件", description = "自动解密与解 ZIP，返回原始文件内容")
	public ResponseEntity<byte[]> download(@PathVariable Long fileId) {
		// 文件已被删除（元数据不存在）应返回 404，而不是抛异常变成 500
		if (fileStorageService.metadata(fileId).isEmpty()) {
			return ResponseEntity.notFound().build();
		}
		FileDownloadContent content;
		try {
			content = fileStorageService.download(fileId);
		} catch (IllegalArgumentException e) {
			// 元数据在但物理文件已不在（存储层报「文件不存在」）→ 404
			return ResponseEntity.notFound().build();
		}
		String filename = content.getFilename() == null ? ("file-" + fileId) : content.getFilename();
		MediaType mediaType;
		try {
			mediaType = content.getContentType() == null
				? MediaType.APPLICATION_OCTET_STREAM
				: MediaType.parseMediaType(content.getContentType());
		} catch (Exception e) {
			mediaType = MediaType.APPLICATION_OCTET_STREAM;
		}
		return ResponseEntity.ok()
			.contentType(mediaType)
			.header(HttpHeaders.CONTENT_DISPOSITION, "inline; filename=\"" + filename + "\"")
			.contentLength(content.getContent() == null ? 0 : content.getContent().length)
			.body(content.getContent());
	}

	@GetMapping("/link/{fileId}")
	@Operation(summary = "获取文件访问地址")
	public R<String> link(@PathVariable Long fileId) {
		return R.data(fileStorageService.link(fileId));
	}

	@GetMapping("/config")
	@Operation(summary = "获取上传能力配置", description = "前端据此渲染「压缩/加密/存储方式」选项")
	public R<FileStorageConfigVO> config() {
		return R.data(fileStorageService.config());
	}

	@PostMapping("/delete/{fileId}")
	@Operation(summary = "删除附件")
	public R<Boolean> delete(@PathVariable Long fileId) {
		return R.data(fileStorageService.delete(fileId));
	}

	private String tenantId(HttpServletRequest request) {
		String tenantId = request.getHeader("Tenant-Id");
		if (tenantId == null || tenantId.isBlank()) {
			tenantId = request.getHeader("tenant-id");
		}
		return (tenantId == null || tenantId.isBlank()) ? "000000" : tenantId;
	}

}