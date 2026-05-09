package org.springblade.core.boot.file;

import lombok.extern.slf4j.Slf4j;

import java.io.*;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import java.util.zip.ZipOutputStream;

/**
 * ZIP压缩工具类
 * 参考ecology项目实现
 *
 * @author Blade
 */
@Slf4j
public class ZipCompressUtil {

	private static final int BUFFER_SIZE = 1024;

	/**
	 * 压缩文件为ZIP格式（使用文件名作为ZIP条目名）
	 *
	 * @param sourceFilePath 源文件路径
	 * @param zipFilePath    输出ZIP路径
	 */
	public static void compressToZip(String sourceFilePath, String zipFilePath) {
		File sourceFile = new File(sourceFilePath);
		if (!sourceFile.exists()) {
			log.error("源文件不存在: {}", sourceFilePath);
			return;
		}

		String entryName = sourceFile.getName();
		compressToZip(sourceFilePath, zipFilePath, entryName);
	}

	/**
	 * 压缩文件为ZIP格式（使用指定条目名）
	 * 参考 ecology 的 ImageFileManager.saveImageFile() 实现：
	 *
	 * ecology 关键代码：
	 *   ZipOutputStream filezipOut = new ZipOutputStream(new BufferedOutputStream(new FileOutputStream(file)));
	 *   filezipOut.setMethod(ZipOutputStream.DEFLATED);
	 *   filezipOut.putNextEntry(new ZipEntry(random));
	 *   fileOut = filezipOut;
	 *   fileOut.write(this.data, 0, this.data.length);
	 *   finally { fileOut.close(); }
	 *
	 * 注意：ecology 只关闭最外层流（fileOut），它会自动关闭所有底层流并写入ZIP结束标记
	 *
	 * @param sourceFilePath 源文件路径
	 * @param zipFilePath    输出ZIP路径
	 * @param entryName      ZIP内部条目名称（通常为UUID，不带扩展名）
	 */
	public static void compressToZip(String sourceFilePath, String zipFilePath, String entryName) {
		File sourceFile = new File(sourceFilePath);
		if (!sourceFile.exists()) {
			log.error("源文件不存在: {}", sourceFilePath);
			return;
		}

		FileInputStream fis = null;
		ZipOutputStream zos = null;

		try {
			fis = new FileInputStream(sourceFile);

			zos = new ZipOutputStream(new BufferedOutputStream(new FileOutputStream(zipFilePath)));
			zos.setMethod(ZipOutputStream.DEFLATED);

			ZipEntry zipEntry = new ZipEntry(entryName);
			zos.putNextEntry(zipEntry);

			byte[] buffer = new byte[BUFFER_SIZE];
			int len;
			while ((len = fis.read(buffer)) != -1) {
				zos.write(buffer, 0, len);
			}

			zos.closeEntry();
			log.info("文件压缩成功: {} -> {}, entry={}", sourceFilePath, zipFilePath, entryName);

		} catch (IOException e) {
			log.error("压缩文件失败", e);
			File zipFile = new File(zipFilePath);
			if (zipFile.exists()) {
				zipFile.delete();
			}
		} finally {
			try {
				if (fis != null) {
					fis.close();
				}
			} catch (IOException e) {
				log.error("关闭输入流失败", e);
			}
			try {
				if (zos != null) {
					zos.close();
				}
			} catch (IOException e) {
				log.error("关闭ZIP输出流失败", e);
			}
		}
	}

	public static void decompressFromZip(String zipFilePath, String outputDir) {
		File zipFile = new File(zipFilePath);
		if (!zipFile.exists()) {
			log.error("ZIP文件不存在: {}", zipFilePath);
			return;
		}

		File outputDirectory = new File(outputDir);
		if (!outputDirectory.exists()) {
			outputDirectory.mkdirs();
		}

		try (FileInputStream fis = new FileInputStream(zipFilePath);
			 ZipInputStream zis = new ZipInputStream(fis)) {

			ZipEntry entry;
			while ((entry = zis.getNextEntry()) != null) {
				File outputFile = new File(outputDir, entry.getName());
				try (FileOutputStream fos = new FileOutputStream(outputFile)) {
					byte[] buffer = new byte[BUFFER_SIZE];
					int len;
					while ((len = zis.read(buffer)) != -1) {
						fos.write(buffer, 0, len);
					}
				}
				zis.closeEntry();
			}

			log.info("ZIP解压成功: {} -> {}", zipFilePath, outputDir);

		} catch (IOException e) {
			log.error("解压ZIP失败", e);
		}
	}

	public static InputStream getZipInputStream(String zipFilePath) {
		try {
			FileInputStream fis = new FileInputStream(zipFilePath);
			ZipInputStream zis = new ZipInputStream(fis);
			if (zis.getNextEntry() != null) {
				return new BufferedInputStream(zis);
			}
		} catch (IOException e) {
			log.error("获取ZIP输入流失败", e);
		}
		return null;
	}

	public static boolean isZipFile(String filePath) {
		if (filePath == null || !filePath.toLowerCase().endsWith(".zip")) {
			return false;
		}
		try (RandomAccessFile raf = new RandomAccessFile(filePath, "r")) {
			long signature = raf.readInt();
			return signature == 0x504b0304;
		} catch (IOException e) {
			return false;
		}
	}

}
