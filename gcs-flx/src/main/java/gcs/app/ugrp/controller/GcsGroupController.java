package gcs.app.ugrp.controller;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import gcs.app.ugrp.domain.GcsGroup;
import gcs.app.ugrp.mapper.GcsGroupMapper;
import gcs.base.BaseController;
import gcs.modules.log.annotation.GLog;
import gcs.base.core.bean.GcsResult;
import gcs.base.core.condition.GcsCondition;
import gcs.base.core.validators.ValidatorUtils;
import gcs.base.tools.GcsBindingUtils;
import org.apache.shiro.authz.annotation.RequiresPermissions;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

@RestController
@RequestMapping("/gcsGroup")
public class GcsGroupController extends ServiceImpl<GcsGroupMapper, GcsGroup> {

	@RequestMapping(value = "/index", method = {RequestMethod.GET, RequestMethod.POST})
	@RequiresPermissions("gcsGroup:index")
	public GcsResult index(@RequestBody(required = false) Optional<GcsCondition> payload){
		GcsCondition gcsCondition = payload.orElse(new GcsCondition());
		IPage<GcsGroup> page = this.page(
				gcsCondition.buildQueryPage(),
				gcsCondition.buildQueryWrapper(GcsGroup.class)
		);
		page.convert(GcsGroupController::_entityMap);
		return GcsResult.successResult().wrapPage(page);
	}

	@GetMapping("/show/{id}")
	@RequiresPermissions("gcsGroup:show")
	public GcsResult show(@PathVariable("id") Long id){
		GcsGroup entity = this.getById(id);
		if(entity == null){
			return GcsResult.notExistResult().put("id", id);
		}
		return GcsResult.successResult().putAll(_entityMap(entity));
	}

	@GLog("保存角色")
	@PostMapping("/save")
	@RequiresPermissions("gcsGroup:save")
	public GcsResult save1(@RequestBody Map<String, Object> jsonMap){
		GcsGroup entity = GcsBindingUtils.bind(jsonMap, GcsGroup.class);
		ValidatorUtils.validateObj(entity);
		this.save(entity);
		return GcsResult.successResult().putAll(_entityMap(entity));
	}

	@GLog("更新角色")
	@PutMapping("/update/{id}")
	@RequiresPermissions("gcsGroup:update")
	public GcsResult update(@PathVariable("id") Long id, @RequestBody Map<String, Object> jsonMap){
		GcsGroup entity = this.getById(id);
		if(entity == null){
			return GcsResult.notExistResult().put("id", id);
		}
		GcsBindingUtils.bind(jsonMap, entity);
		ValidatorUtils.validateObj(entity);
		this.updateById(entity);
		return GcsResult.successResult().putAll(_entityMap(entity));
	}

	@GLog("删除角色")
	@DeleteMapping("/delete/{id}")
	@RequiresPermissions("gcsGroup:delete")
	public GcsResult delete(@PathVariable("id") Long id){
		Boolean isSuccess = this.removeById(id);
		if(!isSuccess){
			return GcsResult.notExistResult().put("id", id);
		}
		return GcsResult.successResult().put("id", id);
	}

	/** ----- 工具方法 ----- */
	public static Map<String, Object> _entityMap(GcsGroup entity){
		return BaseController._entityMap(entity);
	}
}
