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
import lombok.extern.slf4j.Slf4j;
import org.apache.ibatis.session.Configuration;
import org.apache.ibatis.session.SqlSessionFactory;
import org.springframework.beans.factory.SmartInitializingSingleton;
import org.springframework.context.annotation.DependsOn;

import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.Collection;

/**
 * 租户表预扫描器：在所有 Singleton Bean 初始化完成后，主动预热所有 Mapper 的 TableInfo，
 * 使 BladeTenantHandler 能在首次 SQL 执行前就识别出所有 TenantEntity 子类对应的表。
 *
 * 使用 SmartInitializingSingleton + @DependsOn("sqlSessionFactory") 确保在 SqlSessionFactory 创建完毕后执行。
 */
@Slf4j
@DependsOn("sqlSessionFactory")
public class BladeTenantTableScanner implements SmartInitializingSingleton {

	private final SqlSessionFactory sqlSessionFactory;

	public BladeTenantTableScanner(SqlSessionFactory sqlSessionFactory) {
		this.sqlSessionFactory = sqlSessionFactory;
	}

	@Override
	public void afterSingletonsInstantiated() {
		log.info("[BladeTenantTableScanner] Starting pre-scan of all Mapper TableInfos...");
		try {
			preloadAll();
			// 触发 BladeTenantHandler 收集已预热的 TableInfo
			BladeTenantHandler.scanFromTableInfoHelper();
			log.info("[BladeTenantTableScanner] Pre-scan complete. AUTO_TABLES.size()={}", BladeTenantHandler.AUTO_TABLES.size());
		} catch (Exception e) {
			log.warn("[BladeTenantTableScanner] Pre-scan failed (non-fatal): {}", e.getMessage(), e);
		}
	}

	/**
	 * 遍历所有注册的 Mapper 接口，通过反射提取泛型参数（如 BaseMapper<User> → User），
	 * 调用 TableInfoHelper.getTableInfo() 主动预热 TableInfo 缓存。
	 */
	void preloadAll() {
		try {
			Configuration configuration = sqlSessionFactory.getConfiguration();
			Collection<Class<?>> mappers = configuration.getMapperRegistry().getMappers();
			log.info("[BladeTenantTableScanner] Found {} registered mappers", mappers.size());
			int preloaded = 0;
			for (Class<?> mapperClass : mappers) {
				try {
					Class<?> entityType = extractEntityType(mapperClass);
					if (entityType != null && !entityType.isInterface() && !entityType.isEnum()) {
						var info = TableInfoHelper.getTableInfo(entityType);
						if (info != null) {
							preloaded++;
						}
					}
				} catch (Exception ignored) {
				}
			}
			log.info("[BladeTenantTableScanner] Preloaded {} entity TableInfos", preloaded);
		} catch (Exception e) {
			log.warn("[BladeTenantTableScanner] preloadAll failed: {}", e.getMessage());
		}
	}

	static Class<?> extractEntityType(Class<?> mapperClass) {
		for (var iface : mapperClass.getGenericInterfaces()) {
			if (iface instanceof ParameterizedType pt) {
				Type rawType = pt.getRawType();
				String rawName = rawType.getTypeName();
				if ("com.baomidou.mybatisplus.core.mapper.BaseMapper".equals(rawName)
					|| "com.baomidou.mybatisplus.core.mapper.Mapper".equals(rawName)) {
					Type[] args = pt.getActualTypeArguments();
					if (args.length > 0 && args[0] instanceof Class<?>) {
						return (Class<?>) args[0];
					}
				}
			}
		}
		return null;
	}
}
