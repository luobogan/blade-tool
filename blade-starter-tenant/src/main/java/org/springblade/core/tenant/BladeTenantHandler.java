/**
 * Copyright (c) 2018-2099, Chill Zhuang 庄骞 (bladejava@qq.com).
 * <p>
 * Licensed under the GNU LESSER GENERAL PUBLIC LICENSE 3.0;
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * <p>
 * http://www.gnu.org/licenses/lgpl.html
 * <p>
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.springblade.core.tenant;

import com.baomidou.mybatisplus.core.metadata.TableInfoHelper;
import com.baomidou.mybatisplus.extension.plugins.handler.TenantLineHandler;
import lombok.extern.slf4j.Slf4j;
import net.sf.jsqlparser.expression.Expression;
import net.sf.jsqlparser.expression.StringValue;
import org.springblade.core.mp.base.TenantEntity;
import org.springblade.core.tool.utils.Func;
import org.springblade.core.tool.utils.StringUtil;

import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 租户信息处理器（增强版：支持自动匹配租户表）
 */
@Slf4j
public class BladeTenantHandler implements TenantLineHandler {

	private final BladeTenantProperties properties;

	public BladeTenantHandler(BladeTenantProperties properties) {
		this.properties = properties;
	}

	/**
	 * 自动匹配的租户表名集合（小写），由 BladeTenantTableScanner 在启动后填充
	 */
	static final Set<String> AUTO_TABLES = ConcurrentHashMap.newKeySet();
	private static volatile boolean scanned = false;
	/** 控制日志打印频率，避免刷屏 */
	private int logCounter = 0;
	private static final int LOG_LIMIT = 20;

	@Override
	public Expression getTenantId() {
		return new StringValue(Func.toStr(TenantUtil.getTenantId(), properties.getDefaultTenantId()));
	}

	@Override
	public String getTenantIdColumn() {
		return properties.getColumn();
	}

	@Override
	public boolean ignoreTable(String tableName) {
		logCounter++;
		boolean verbose = logCounter <= LOG_LIMIT;

		if (verbose)
			log.info("[BladeTenant#{}] ignoreTable({}) called", logCounter, tableName);

		if (BladeTenantHolder.isIgnore()) {
			if (verbose) log.info("[BladeTenant#{}] → IGNORED: BladeTenantHolder.isIgnore()=true", logCounter);
			return true;
		}
		if (TenantUtil.isIgnore()) {
			if (verbose) log.info("[BladeTenant#{}] → IGNORED: TenantUtil.isIgnore()=true", logCounter);
			return true;
		}

		String tenantId = TenantUtil.getTenantId();
		if (StringUtil.isBlank(tenantId)) {
			if (verbose) log.info("[BladeTenant#{}] → IGNORED: tenantId is blank/null! (value='{}')", logCounter, tenantId);
			return true;
		}

		String lowerName = tableName.toLowerCase();

		// 1. 手动配置检查
		boolean manualMatch = (!properties.getTables().isEmpty() && properties.getTables().contains(lowerName))
			|| properties.getBladeTables().contains(lowerName);
		if (manualMatch) {
			log.info("[BladeTenant#{}] → TENANT TABLE (manual config): {}", logCounter, tableName);
			return false;
		}

		// 2. 自动扫描（懒加载 + 缓存）
		boolean autoMatch = isAutoTenantTable(lowerName);
		if (verbose) {
			log.info("[BladeTenant#{}] → autoMatch={} for table '{}', scanned={}, cacheSize={}",
					logCounter, autoMatch, tableName, scanned, AUTO_TABLES.size());
		}
		return !autoMatch;
	}

	private boolean isAutoTenantTable(String tableName) {
		if (AUTO_TABLES.contains(tableName)) {
			return true;
		}
		if (!scanned) {
			synchronized (BladeTenantHandler.class) {
				if (!scanned) {
					scanFromTableInfoHelper();
					scanned = true;
				}
			}
		}
		return findSingleTable(tableName);
	}

	/**
	 * 从 TableInfoHelper 已缓存的 TableInfo 中收集所有 TenantEntity 子类表名
	 * （由 BladeTenantTableScanner.preloadAll() 先调用 getTableInfo 预热）
	 */
	static void scanFromTableInfoHelper() {
		try {
			var tableInfos = TableInfoHelper.getTableInfos();
			int totalCount = tableInfos.size();
			int count = 0;
			for (var info : tableInfos) {
				Class<?> entityClass = info.getEntityType();
				if (entityClass != null && TenantEntity.class.isAssignableFrom(entityClass)) {
					String name = info.getTableName();
					if (name != null && !name.isEmpty()) {
						AUTO_TABLES.add(name.toLowerCase());
						count++;
						log.info("[BladeTenant] SCAN-HIT: {} -> extends TenantEntity ({})", name, entityClass.getName());
					}
				} else if (entityClass != null && count + 1 <= LOG_LIMIT) {
					log.info("[BladeTenant] SCAN-SKIP: {} -> class={} (NOT TenantEntity)",
							info.getTableName(), entityClass.getName());
				}
			}
			log.info("[BladeTenant] === SCAN COMPLETE: total entities={}, tenant tables registered={} ===",
					totalCount, count);
		} catch (Exception e) {
			log.warn("[BladeTenant] Failed to scan: {}", e.getMessage(), e);
		}
	}

	private boolean findSingleTable(String tableName) {
		try {
			for (var info : TableInfoHelper.getTableInfos()) {
				if (tableName.equalsIgnoreCase(info.getTableName())) {
					Class<?> entityClass = info.getEntityType();
					log.info("[BladeTenant] SINGLE-LOOKUP: table={}, entity={}, isTenantEntity={}",
							tableName,
							entityClass != null ? entityClass.getName() : "null",
							entityClass != null ? TenantEntity.class.isAssignableFrom(entityClass) : "N/A");
					if (entityClass != null && TenantEntity.class.isAssignableFrom(entityClass)) {
						AUTO_TABLES.add(tableName.toLowerCase());
						return true;
					}
				}
			}
		} catch (Exception ignored) {
		}
		return false;
	}
}
