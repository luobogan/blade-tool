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

import lombok.experimental.UtilityClass;
import org.springblade.core.secure.BladeUser;
import org.springblade.core.secure.utils.AuthUtil;
import org.springblade.core.tool.utils.StringUtil;

/**
 * 超级管理员租户工具类
 * 判断当前用户是否为超级管理员（租户ID=000000 且 角色包含admin）
 *
 * @author BladeX
 */
@UtilityClass
public class AdminTenantUtil {

	/**
	 * 管理员租户ID
	 */
	private static final String ADMIN_TENANT_ID = "000000";

	/**
	 * 管理员角色标识
	 */
	private static final String ADMIN_ROLE = "admin";

	/**
	 * 判断当前用户是否为超级管理员
	 * 条件：租户ID为000000 且 角色包含admin
	 *
	 * @return true=超级管理员, false=普通用户
	 */
	public boolean isSuperAdmin() {
		BladeUser user = AuthUtil.getUser();
		if (user == null) {
			return false;
		}

		if (!ADMIN_TENANT_ID.equals(user.getTenantId())) {
			return false;
		}

		String roleName = user.getRoleName();
		if (StringUtil.isBlank(roleName)) {
			return false;
		}

		return roleName.contains(ADMIN_ROLE);
	}

	/**
	 * 判断当前租户ID是否为管理员租户（仅判断租户ID，不判断角色）
	 *
	 * @return true=管理员租户, false=普通租户
	 */
	public boolean isAdminTenant() {
		String tenantId = AuthUtil.getTenantId();
		return ADMIN_TENANT_ID.equals(tenantId);
	}

	/**
	 * 判断当前用户角色是否包含admin
	 *
	 * @return true=包含admin角色, false=不包含
	 */
	public boolean isAdminRole() {
		BladeUser user = AuthUtil.getUser();
		if (user == null) {
			return false;
		}

		String roleName = user.getRoleName();
		if (StringUtil.isBlank(roleName)) {
			return false;
		}

		return roleName.contains(ADMIN_ROLE);
	}

}
