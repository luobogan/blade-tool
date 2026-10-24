package org.springblade.core.boot.file.util;

import lombok.extern.slf4j.Slf4j;
import org.springblade.core.boot.file.AESCoder;
import org.springblade.core.boot.file.ZipCompressUtil;

import javax.imageio.ImageIO;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Random;
import java.util.UUID;

/**
 * 文件存储底层工具：路径生成 + 委托框架自带能力
 *
 * <p><b>ZIP 打包/解压、AES 加解密直接复用框架自带实现</b>：
 * {@link ZipCompressUtil}、{@link AESCoder}（均在 blade-core-boot），
 * 本类不再重复实现算法，保证与历史附件（由 BladeFileProxyFactory 产出）互相可解压/可解密。</p>
 *
 * <p>本类只保留框架未覆盖的部分：存储路径规则（各段字符清洗防目录穿越）、
 * 文件名生成、图片尺寸压缩、原地覆盖的安全封装。</p>
 *
 * @author Blade
 */
@Slf4j
public final class FileStorageSupport {

	/**
	 * 图片压缩后的最大宽度（超过则等比缩小）
	 */
	private static final int MAX_IMAGE_WIDTH = 1600;

	/**
	 * 目录分片用的随机数生成器
	 */
	private static final Random RANDOM = new Random();

	private FileStorageSupport() {
	}

	/**
	 * 生成附件存储的相对目录：{uploadDir}/{tenantId}/{dir}/{yyyyMM}/{A-Z}
	 *
	 * <p>各段均做字符清洗，避免租户ID/目录名中的分隔符造成目录穿越。</p>
	 * <p>末尾追加 A-Z 随机字母分片，与框架历史 {@code BladeFileProxyFactory} 行为一致，
	 * 将文件打散到 26 个字母目录，避免单目录文件过多。</p>
	 */
	public static String buildDateDir(String uploadDir, String tenantId, String dir) {
		StringBuilder sb = new StringBuilder();
		appendSegment(sb, uploadDir);
		appendSegment(sb, tenantId);
		appendSegment(sb, dir);
		appendSegment(sb, new SimpleDateFormat("yyyyMM").format(new Date()));
		appendSegment(sb, randomLetter());
		return sb.toString();
	}

	/**
	 * 随机 A-Z 单个大写字母（用于目录分片）
	 */
	private static String randomLetter() {
		return String.valueOf((char) ('A' + RANDOM.nextInt(26)));
	}

	private static void appendSegment(StringBuilder sb, String segment) {
		if (segment == null || segment.isBlank()) {
			return;
		}
		String safe = segment.trim().replaceAll("[^A-Za-z0-9_\\-]", "_");
		if (safe.isEmpty()) {
			return;
		}
		if (sb.length() > 0) {
			sb.append('/');
		}
		sb.append(safe);
	}

	/**
	 * 生成存储文件名（UUID + 扩展名）
	 */
	public static String buildFileName(String originalName) {
		return UUID.randomUUID().toString().replace("-", "") + extensionOf(originalName);
	}

	/**
	 * 取扩展名（含点），无扩展名返回空串
	 */
	public static String extensionOf(String fileName) {
		if (fileName == null) {
			return "";
		}
		int idx = fileName.lastIndexOf('.');
		return idx > 0 ? fileName.substring(idx).toLowerCase() : "";
	}

	/**
	 * 打包为 ZIP（单文件），返回 zip 文件路径
	 *
	 * <p>复用框架自带 {@link ZipCompressUtil#compressToZip(String, String, String)}，
	 * ZIP 内条目名沿用 uuid（不含扩展名），与框架 BladeFileProxyFactory 产出一致。</p>
	 */
	public static Path zipFile(Path source) {
		Path zipPath = source.resolveSibling(stripExtension(source.getFileName().toString()) + ".zip");
		String entryName = stripExtension(source.getFileName().toString());
		ZipCompressUtil.compressToZip(source.toString(), zipPath.toString(), entryName);
		if (!Files.exists(zipPath)) {
			throw new IllegalStateException("ZIP 打包失败: " + source);
		}
		return zipPath;
	}

	/**
	 * 读取 ZIP 内首个条目内容（框架自带工具提供）
	 */
	public static byte[] readZipEntry(Path zipPath) {
		try (InputStream in = ZipCompressUtil.getZipInputStream(zipPath.toString())) {
			if (in == null) {
				return new byte[0];
			}
			ByteArrayOutputStream baos = new ByteArrayOutputStream();
			byte[] buffer = new byte[8192];
			int len;
			while ((len = in.read(buffer)) != -1) {
				baos.write(buffer, 0, len);
			}
			return baos.toByteArray();
		} catch (IOException e) {
			throw new IllegalStateException("ZIP 读取失败: " + e.getMessage(), e);
		}
	}

	private static String stripExtension(String name) {
		int idx = name.lastIndexOf('.');
		return idx > 0 ? name.substring(0, idx) : name;
	}

	/**
	 * 生成 13 位随机 AES 密钥（复用框架自带 {@link AESCoder#generateRandomKey()}）
	 */
	public static String generateAesKey() {
		return AESCoder.generateRandomKey();
	}

	/**
	 * 加密文件（原地覆盖）
	 *
	 * <p>复用框架自带 {@link AESCoder#encryptFile(String, String, String)}；
	 * 因其入参/出参为两个路径，传入同路径会因 FileOutputStream 先截断文件而丢内容，
	 * 故先写临时文件再原子替换（框架 BladeFileProxyFactory 亦为原地调用，同一问题一并规避）。</p>
	 */
	public static void encryptFile(Path file, String code) {
		Path temp = file.resolveSibling(file.getFileName().toString() + ".enc");
		AESCoder.encryptFile(file.toString(), temp.toString(), code);
		try {
			Files.deleteIfExists(file);
			Files.move(temp, file);
		} catch (IOException e) {
			throw new IllegalStateException("加密文件替换失败: " + e.getMessage(), e);
		}
	}

	/**
	 * 解密字节流（复用框架自带 {@link AESCoder#decrypt(InputStream, String)}）
	 */
	public static byte[] decryptBytes(byte[] data, String code) {
		if (data == null || data.length == 0) {
			return data;
		}
		try (InputStream in = AESCoder.decrypt(new java.io.ByteArrayInputStream(data), code)) {
			ByteArrayOutputStream baos = new ByteArrayOutputStream(data.length);
			byte[] buffer = new byte[8192];
			int len;
			while ((len = in.read(buffer)) != -1) {
				baos.write(buffer, 0, len);
			}
			return baos.toByteArray();
		} catch (Exception e) {
			throw new IllegalStateException("文件解密失败: " + e.getMessage(), e);
		}
	}

	/**
	 * 图片压缩：宽度超过阈值时等比缩小并重写原文件（非图片或压缩失败时静默跳过）
	 */
	public static void compressImage(Path file) {
		Path temp = file.resolveSibling(file.getFileName().toString() + ".tmp");
		try {
			BufferedImage source = ImageIO.read(file.toFile());
			if (source == null) {
				return;
			}
			int width = source.getWidth();
			int height = source.getHeight();
			if (width <= MAX_IMAGE_WIDTH) {
				return;
			}
			int targetHeight = (int) Math.round(height * (MAX_IMAGE_WIDTH / (double) width));
			BufferedImage scaled = new BufferedImage(MAX_IMAGE_WIDTH, targetHeight, BufferedImage.TYPE_INT_RGB);
			Graphics2D g = scaled.createGraphics();
			g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
			g.drawImage(source, 0, 0, MAX_IMAGE_WIDTH, targetHeight, null);
			g.dispose();
			if (ImageIO.write(scaled, formatOf(file), temp.toFile())) {
				Files.deleteIfExists(file);
				Files.move(temp, file);
				log.debug("图片已压缩: {} -> {}x{}", file, MAX_IMAGE_WIDTH, targetHeight);
			} else {
				Files.deleteIfExists(temp);
			}
		} catch (Exception e) {
			log.warn("图片压缩失败（忽略）: {}", file, e);
			try {
				Files.deleteIfExists(temp);
			} catch (IOException ignore) {
				// ignore
			}
		}
	}

	private static String formatOf(Path file) {
		String ext = extensionOf(file.getFileName().toString());
		return "png".equals(ext) ? "png" : "jpg";
	}

	/**
	 * 删除文件（忽略异常）
	 */
	public static void deleteQuietly(File file) {
		if (file != null && file.exists() && !file.delete()) {
			log.warn("文件删除失败: {}", file.getAbsolutePath());
		}
	}

}