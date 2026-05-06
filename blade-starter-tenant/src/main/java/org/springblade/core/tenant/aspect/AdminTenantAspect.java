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
package org.springblade.core.tenant.aspect;

import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.springblade.core.secure.utils.AuthUtil;
import org.springblade.core.tenant.TenantUtil;

/**
 * 管理员租户切面
 * 000000租户访问标注 @AdminTenant 的方法时，不进行租户隔离
 *
 * @author BladeX
 */
@Slf4j
@Aspect
public class AdminTenantAspect {

	/**
	 * 管理员租户ID
	 */
	private static final String ADMIN_TENANT_ID = "000000";

	@Around("@annotation(org.springblade.core.tenant.annotation.AdminTenant)")
	public Object around(ProceedingJoinPoint point) throws Throwable {
		String tenantId = AuthUtil.getTenantId();
		log.debug("AdminTenantAspect - 当前租户ID: {}", tenantId);

		if (ADMIN_TENANT_ID.equals(tenantId)) {
			// 000000租户忽略租户隔离
			log.debug("AdminTenantAspect - 管理员租户，忽略租户隔离");
			return TenantUtil.ignore(() -> {
				try {
					return point.proceed();
				} catch (Throwable e) {
					throw new RuntimeException(e);
				}
			});
		}
		// 普通租户正常执行
		return point.proceed();
	}

}
