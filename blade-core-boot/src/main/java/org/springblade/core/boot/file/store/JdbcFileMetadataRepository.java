package org.springblade.core.boot.file.store;

import lombok.extern.slf4j.Slf4j;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.Date;
import java.util.Optional;

import javax.sql.DataSource;

/**
 * 基于 ImageFile 表的元数据仓储（纯 JDBC，不依赖 MyBatis，避免给无状态服务引入 ORM 负担）
 *
 * <p>列名与 blade.sql 中 imagefile 表保持一致（注意 fileSize / secretLevel / TokenKey 大小写敏感）。</p>
 *
 * @author Blade
 */
@Slf4j
public class JdbcFileMetadataRepository implements FileMetadataRepository {

	private static final String INSERT_SQL = "INSERT INTO imagefile "
		+ "(imagefileid, imagefilename, imagefiletype, imagefileused, filerealpath, iszip, isencrypt, "
		+ "fileSize, isaesencrypt, aescode, TokenKey, secretLevel, storagestatus, comefrom, "
		+ "createdat, updatedat, tenantid) "
		+ "VALUES (?, ?, ?, 1, ?, ?, ?, ?, ?, ?, NULL, 4, ?, ?, ?, ?, ?)";

	private static final String SELECT_SQL = "SELECT imagefileid, imagefilename, imagefiletype, fileSize, "
		+ "filerealpath, iszip, isaesencrypt, aescode, storagestatus, comefrom, tenantid, createdat "
		+ "FROM imagefile WHERE imagefileid = ?";

	private static final String DELETE_SQL = "DELETE FROM imagefile WHERE imagefileid = ?";

	private final DataSource dataSource;

	public JdbcFileMetadataRepository(DataSource dataSource) {
		this.dataSource = dataSource;
	}

	@Override
	public Long save(FileMetadata metadata) {
		try (Connection conn = dataSource.getConnection();
			 PreparedStatement ps = conn.prepareStatement(INSERT_SQL)) {
			Date now = metadata.getCreatedAt() != null ? metadata.getCreatedAt() : new Date();
			ps.setLong(1, metadata.getId());
			ps.setString(2, truncate(metadata.getFilename(), 500));
			ps.setString(3, truncate(metadata.getContentType(), 100));
			ps.setString(4, truncate(metadata.getPath(), 1000));
			ps.setInt(5, metadata.isZip() ? 1 : 0);
			ps.setInt(6, metadata.isEncrypt() ? 1 : 0);
			if (metadata.getFilesize() == null) {
				ps.setNull(7, java.sql.Types.BIGINT);
			} else {
				ps.setLong(7, metadata.getFilesize());
			}
			ps.setInt(8, metadata.isEncrypt() ? 1 : 0);
			ps.setString(9, truncate(metadata.getAesCode(), 50));
			ps.setString(10, truncate(metadata.getStorageStatus(), 50));
			ps.setString(11, truncate(metadata.getComefrom(), 100));
			ps.setTimestamp(12, new Timestamp(now.getTime()));
			ps.setTimestamp(13, new Timestamp(now.getTime()));
			ps.setString(14, metadata.getTenantId() == null ? "" : metadata.getTenantId());
			ps.executeUpdate();
			log.info("附件元数据已保存: fileId={}, filename={}", metadata.getId(), metadata.getFilename());
			return metadata.getId();
		} catch (SQLException e) {
			log.error("保存附件元数据失败: fileId={}", metadata.getId(), e);
			throw new IllegalStateException("保存附件元数据失败: " + e.getMessage(), e);
		}
	}

	@Override
	public Optional<FileMetadata> findById(Long id) {
		if (id == null) {
			return Optional.empty();
		}
		try (Connection conn = dataSource.getConnection();
			 PreparedStatement ps = conn.prepareStatement(SELECT_SQL)) {
			ps.setLong(1, id);
			try (ResultSet rs = ps.executeQuery()) {
				if (!rs.next()) {
					return Optional.empty();
				}
				FileMetadata metadata = new FileMetadata();
				metadata.setId(rs.getLong("imagefileid"));
				metadata.setFilename(rs.getString("imagefilename"));
				metadata.setContentType(rs.getString("imagefiletype"));
				long size = rs.getLong("fileSize");
				metadata.setFilesize(rs.wasNull() ? null : size);
				metadata.setPath(rs.getString("filerealpath"));
				metadata.setZip(rs.getInt("iszip") == 1);
				metadata.setEncrypt(rs.getInt("isaesencrypt") == 1);
				metadata.setAesCode(rs.getString("aescode"));
				metadata.setStorageStatus(rs.getString("storagestatus"));
				metadata.setComefrom(rs.getString("comefrom"));
				metadata.setTenantId(rs.getString("tenantid"));
				metadata.setCreatedAt(rs.getTimestamp("createdat"));
				return Optional.of(metadata);
			}
		} catch (SQLException e) {
			log.error("查询附件元数据失败: fileId={}", id, e);
			return Optional.empty();
		}
	}

	@Override
	public boolean deleteById(Long id) {
		if (id == null) {
			return false;
		}
		try (Connection conn = dataSource.getConnection();
			 PreparedStatement ps = conn.prepareStatement(DELETE_SQL)) {
			ps.setLong(1, id);
			return ps.executeUpdate() > 0;
		} catch (SQLException e) {
			log.error("删除附件元数据失败: fileId={}", id, e);
			return false;
		}
	}

	private String truncate(String value, int max) {
		if (value == null) {
			return null;
		}
		return value.length() <= max ? value : value.substring(0, max);
	}

}