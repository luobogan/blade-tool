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
package org.springblade.core.tenant.config;

import com.baomidou.mybatisplus.extension.plugins.handler.TenantLineHandler;
import com.baomidou.mybatisplus.extension.plugins.inner.TenantLineInnerInterceptor;
import org.apache.ibatis.session.SqlSessionFactory;
import org.springblade.core.tenant.*;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;

/**
 * 多租户配置类
 *
 * @author Chill
 */
@AutoConfiguration
@EnableConfigurationProperties(BladeTenantProperties.class)
public class TenantConfiguration {

	/**
	 * 自定义多租户处理器（无 SqlSessionFactory 依赖，避免循环依赖）
	 *
	 * @param tenantProperties 多租户配置类
	 * @return TenantHandler
	 */
	@Bean
	@Primary
	public TenantLineHandler bladeTenantHandler(BladeTenantProperties tenantProperties) {
		return new BladeTenantHandler(tenantProperties);
	}

	/**
	 * 自定义租户拦截器
	 *
	 * @param tenantHandler 多租户处理器
	 * @return BladeTenantInterceptor
	 */
	@Bean
	@Primary
	public TenantLineInnerInterceptor tenantLineInnerInterceptor(TenantLineHandler tenantHandler) {
		BladeTenantInterceptor tenantInterceptor = new BladeTenantInterceptor();
		tenantInterceptor.setTenantLineHandler(tenantHandler);
		return tenantInterceptor;
	}

	/**
	 * 租户表预扫描器：在 SqlSessionFactory 创建完毕后，预热所有 Mapper 的 TableInfo，
	 * 使 BladeTenantHandler 能在首次 SQL 前识别所有 TenantEntity 子类表。
	 */
	@Bean
	@ConditionalOnBean(SqlSessionFactory.class)
	public BladeTenantTableScanner bladeTenantTableScanner(SqlSessionFactory sqlSessionFactory) {
		return new BladeTenantTableScanner(sqlSessionFactory);
	}

	/**
	 * 自定义租户id生成器
	 *
	 * @return TenantId
	 */
	@Bean
	@ConditionalOnMissingBean(TenantId.class)
	public TenantId tenantId() {
		return new BladeTenantId();
	}

}
