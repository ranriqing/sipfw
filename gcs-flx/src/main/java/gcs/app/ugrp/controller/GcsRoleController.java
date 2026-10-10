package gcs.app.ugrp.controller;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.baomidou.mybatisplus.extension.toolkit.Db;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import gcs.app.ugrp.domain.GcsRole;
import gcs.app.ugrp.domain.GcsPermission;
import gcs.app.ugrp.mapper.GcsRoleMapper;
import gcs.app.vm.domain.MetaView;
import gcs.base.BaseController;
import gcs.modules.log.annotation.GLog;
import gcs.base.core.bean.GcsResult;
import gcs.base.core.condition.GcsCondition;
import gcs.base.core.validators.ValidatorUtils;
import gcs.base.tools.GcsBindingUtils;
import org.apache.shiro.authz.annotation.RequiresPermissions;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.Optional;

@RestController
@RequestMapping("/gcsRole")
public class GcsRoleController extends ServiceImpl<GcsRoleMapper, GcsRole> {

	@RequestMapping(value = "/index", method = {RequestMethod.GET, RequestMethod.POST})
	@RequiresPermissions("gcsRole:index")
	public GcsResult index(@RequestBody(required = false) Optional<GcsCondition> payload){
		GcsCondition gcsCondition = payload.orElse(new GcsCondition());
		IPage<GcsRole> page = this.page(
				gcsCondition.buildQueryPage(),
				gcsCondition.buildQueryWrapper(GcsRole.class)
		);
		page.convert(GcsRoleController::_entityMap);
		return GcsResult.successResult().wrapPage(page);
	}

	@GetMapping("/show/{id}")
	@RequiresPermissions("gcsRole:show")
	public GcsResult show(@PathVariable("id") Long id){
		GcsRole entity = this.getById(id);
		if(entity == null){
			return GcsResult.notExistResult().put("id", id);
		}
		return GcsResult.successResult().putAll(_entityMap(entity));
	}

	@GLog("保存角色")
	@PostMapping("/save")
	@RequiresPermissions("gcsRole:save")
	public GcsResult save1(@RequestBody Map<String, Object> jsonMap){
		GcsRole entity = GcsBindingUtils.bind(jsonMap, GcsRole.class);
		ValidatorUtils.validateObj(entity);
		this.save(entity);
		_syncRolePermissions(entity.getId(), jsonMap);
		return GcsResult.successResult().putAll(_entityMap(entity));
	}

	@GLog("更新角色")
	@PutMapping("/update/{id}")
	@RequiresPermissions("gcsRole:update")
	public GcsResult update(@PathVariable("id") Long id, @RequestBody Map<String, Object> jsonMap){
		GcsRole entity = this.getById(id);
		if(entity == null){
			return GcsResult.notExistResult().put("id", id);
		}
		GcsBindingUtils.bind(jsonMap, entity);
		ValidatorUtils.validateObj(entity);
		this.updateById(entity);
		_syncRolePermissions(entity.getId(), jsonMap);
		return GcsResult.successResult().putAll(_entityMap(entity));
	}

	@GLog("删除角色")
	@DeleteMapping("/delete/{id}")
	@RequiresPermissions("gcsRole:delete")
	public GcsResult delete(@PathVariable("id") Long id){
		Boolean isSuccess = this.removeById(id);
		if(!isSuccess){
			return GcsResult.notExistResult().put("id", id);
		}
		return GcsResult.successResult().put("id", id);
	}

	/** ----- 工具方法 ----- */
	public static Map<String, Object> _entityMap(GcsRole entity){
		Map<String, Object> entityMap = BaseController._entityMap(entity);
		MetaView metaView = Db.getById(entity.getViewId(), MetaView.class);
		entityMap.put("view", entity.getViewId());
		entityMap.put("viewCaption", metaView!=null?metaView.getName():null);
		// 角色权限（用于前端角色编辑页配置）
		List<GcsPermission> permissions = Db.list(
			Wrappers.lambdaQuery(GcsPermission.class).eq(GcsPermission::getRoleId, entity.getId())
		);
		entityMap.put("permissions", permissions);
		return entityMap;
	}

	/**
	 * 同步角色权限（jsonMap.permissions -> gcs_permission 表）
	 *
	 * 约定：
	 * - permissions 为数组，每项包含 {name, permission}
	 * - permission 支持逗号分隔（用于 checkbox 多选）
	 */
	private static void _syncRolePermissions(Long roleId, Map<String, Object> jsonMap) {
		if (roleId == null || jsonMap == null || !jsonMap.containsKey("permissions")) {
			return;
		}
		Object raw = jsonMap.get("permissions");
		// 允许传 null 表示清空
		Db.remove(Wrappers.lambdaQuery(GcsPermission.class).eq(GcsPermission::getRoleId, roleId));
		if (raw == null) {
			return;
		}
		if (!(raw instanceof List)) {
			return;
		}
		List<?> permList = (List<?>) raw;
		for (Object obj : permList) {
			if (!(obj instanceof Map)) continue;
			Map<?, ?> item = (Map<?, ?>) obj;
			String permission = cn.hutool.core.convert.Convert.toStr(item.get("permission"), null);
			String value = cn.hutool.core.convert.Convert.toStr(item.get("value"), null);
			if (permission == null || value == null) continue;
			GcsPermission p = new GcsPermission();
			p.setRoleId(roleId);
			p.setPermission(permission);
			p.setValue(value);
			Db.save(p);
		}
	}
}
