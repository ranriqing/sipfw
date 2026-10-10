package gcs.modules.config.controller;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import gcs.modules.config.domain.ScConfig;
import gcs.modules.config.mapper.ScConfigMapper;
import gcs.modules.log.annotation.GLog;
import gcs.base.core.bean.GcsResult;
import gcs.base.core.condition.GcsCondition;
import gcs.base.core.validators.ValidatorUtils;
import gcs.base.tools.GcsBindingUtils;
import org.apache.shiro.authz.annotation.RequiresPermissions;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

@RestController
@RequestMapping("/scConfig")
public class ScConfigController extends ServiceImpl<ScConfigMapper, ScConfig> {

	@RequestMapping(value = "/index", method = {RequestMethod.GET, RequestMethod.POST})
	public GcsResult index(@RequestBody(required = false) Optional<GcsCondition> payload){
		GcsCondition gcsCondition = payload.orElse(new GcsCondition());
		IPage<ScConfig> page = this.page(
				gcsCondition.buildQueryPage(),
				gcsCondition.buildQueryWrapper(ScConfig.class)
		);
		page.convert(ScConfigController::_entityMap);
		return GcsResult.successResult().wrapPage(page);
	}

	@GetMapping("/show/{id}")
	public GcsResult show(@PathVariable("id") Long id){
		ScConfig entity = this.getById(id);
		if(entity == null){
			return GcsResult.notExistResult().put("id", id);
		}
		return GcsResult.successResult().putAll(_entityMap(entity));
	}
	
	@GLog("保存选项配置")
	@PostMapping("/save")
	@RequiresPermissions("scConfig:save")
	public GcsResult save1(@RequestBody Map<String, Object> jsonMap){
		ScConfig entity = GcsBindingUtils.bind(jsonMap, ScConfig.class);
		ValidatorUtils.validateObj(entity);
		this.save(entity);
		return GcsResult.successResult().putAll(_entityMap(entity));
	}

	@GLog("更新选项配置")
	@PutMapping("/update/{id}")
	@RequiresPermissions("scConfig:update")
	public GcsResult update(@PathVariable("id") Long id, @RequestBody Map<String, Object> jsonMap){
		ScConfig entity = this.getById(id);
		if(entity == null){
			return GcsResult.notExistResult().put("id", id);
		}
		GcsBindingUtils.bind(jsonMap, entity);
		ValidatorUtils.validateObj(entity);
		this.updateById(entity);
		return GcsResult.successResult().putAll(_entityMap(entity));
	}

	@GLog("删除选项配置")
	@DeleteMapping("/delete/{id}")
	@RequiresPermissions("scConfig:delete")
	public GcsResult delete(@PathVariable("id") Long id){
		Boolean isSuccess = this.removeById(id);
		if(!isSuccess){
			return GcsResult.notExistResult().put("id", id);
		}
		return GcsResult.successResult().put("id", id);
	}

	/** ----- 工具方法 ----- */
	public static Map<String, Object> _entityMap(ScConfig entity){
		Map<String, Object> entityMap = new HashMap<>();
		entityMap.put("id", entity.getId());
		entityMap.put("key", entity.getKey());
		entityMap.put("title", entity.getTitle());
		entityMap.put("sciScCategory", entity.getSciScCategory());
		entityMap.put("seqOrder", entity.getSeqOrder());
		entityMap.put("freeCreation", entity.getFreeCreation());
		entityMap.put("multiLevel", entity.getMultiLevel());
		entityMap.put("allowModify", entity.getAllowModify());
		entityMap.put("remark", entity.getRemark());
		return entityMap;
	}
}
