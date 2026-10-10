package gcs.app.vm.controller;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import gcs.app.ugrp.service.GcsShiroService;
import gcs.app.vm.domain.MetaView;
import gcs.app.vm.mapper.MetaViewMapper;
import gcs.modules.log.annotation.GLog;
import gcs.base.core.bean.GcsResult;
import gcs.base.core.condition.GcsCondition;
import gcs.base.core.validators.ValidatorUtils;
import gcs.base.shiro.spi.SpiUser;
import gcs.base.tools.GcsBindingUtils;
import org.apache.shiro.authz.annotation.RequiresPermissions;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

@RestController
@RequestMapping("/metaView")
public class MetaViewController extends ServiceImpl<MetaViewMapper, MetaView> {

	@Autowired
	private GcsShiroService gcsShiroService;

	@RequestMapping(value = "/index", method = {RequestMethod.GET, RequestMethod.POST})
	@RequiresPermissions("metaView:index")
	public GcsResult index(@RequestBody(required = false) Optional<GcsCondition> payload){
		GcsCondition gcsCondition = payload.orElse(new GcsCondition());
		SpiUser spiUser = gcsShiroService.currentUser();
		QueryWrapper<MetaView> conditionQueryWrapper = gcsCondition.buildQueryWrapper(MetaView.class);
		if(!spiUser.isSuperAdmin()){
			conditionQueryWrapper.eq("type", MetaView.TYPE_OPEN);
		}
		IPage<MetaView> page = this.page(
				gcsCondition.buildQueryPage(),
				conditionQueryWrapper
		);
		page.convert(MetaViewController::_entityMap);
		return GcsResult.successResult().wrapPage(page);
	}

	@GetMapping("/show/{id}")
	@RequiresPermissions("metaView:show")
	public GcsResult show(@PathVariable("id") Long id){
		MetaView entity = this.getById(id);
		if(entity == null){
			return GcsResult.notExistResult().put("id", id);
		}
		return GcsResult.successResult().putAll(_entityMap(entity));
	}

	@GLog("保存视图")
	@PostMapping("/save")
	@RequiresPermissions("metaView:save")
	public GcsResult save1(@RequestBody Map<String, Object> jsonMap){
		MetaView entity = GcsBindingUtils.bind(jsonMap, MetaView.class);
		ValidatorUtils.validateObj(entity);
		this.save(entity);
		return GcsResult.successResult().putAll(_entityMap(entity));
	}

	@GLog("更新视图")
	@PutMapping("/update/{id}")
	@RequiresPermissions("metaView:update")
	public GcsResult update(@PathVariable("id") Long id, @RequestBody Map<String, Object> jsonMap){
		MetaView entity = this.getById(id);
		if(entity == null){
			return GcsResult.notExistResult().put("id", id);
		}
		GcsBindingUtils.bind(jsonMap, entity);
		ValidatorUtils.validateObj(entity);
		this.updateById(entity);
		return GcsResult.successResult().putAll(_entityMap(entity));
	}

	@GLog("删除视图")
	@DeleteMapping("/delete/{id}")
	@RequiresPermissions("metaView:delete")
	public GcsResult delete(@PathVariable("id") Long id){
		Boolean isSuccess = this.removeById(id);
		if(!isSuccess){
			return GcsResult.notExistResult().put("id", id);
		}
		return GcsResult.successResult().put("id", id);
	}

	/** ----- 工具方法 ----- */
	public static Map<String, Object> _entityMap(MetaView entity){
		Map<String, Object> entityMap = new HashMap<>();
		entityMap.put("id", entity.getId());
		entityMap.put("name", entity.getName());
		entityMap.put("type", entity.getType());
		entityMap.put("icon", entity.getIcon());
		entityMap.put("seqOrder", entity.getSeqOrder());
		entityMap.put("active", entity.getActive());
		entityMap.put("remark", entity.getRemark());
		return entityMap;
	}
}
