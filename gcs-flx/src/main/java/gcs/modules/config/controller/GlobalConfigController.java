package gcs.modules.config.controller;

import cn.hutool.core.map.MapUtil;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.baomidou.mybatisplus.extension.toolkit.Db;
import gcs.base.core.bean.GcsMap;
import gcs.app.sipfw.model.SipAnalysisOptions;
import gcs.modules.config.domain.GlobalConfig;
import gcs.modules.config.domain.ScConfig;
import gcs.modules.config.mapper.GlobalConfigMapper;
import gcs.modules.log.annotation.GLog;
import gcs.base.core.bean.GcsResult;
import gcs.base.core.condition.GcsCondition;
import gcs.base.core.validators.ValidatorUtils;
import gcs.base.tools.GcsBindingUtils;
import gcs.modules.config.mapper.ScConfigMapper;
import org.apache.commons.lang3.StringUtils;
import org.apache.shiro.authz.annotation.RequiresPermissions;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.*;

@RestController
@RequestMapping("/globalConfig")
public class GlobalConfigController extends ServiceImpl<GlobalConfigMapper, GlobalConfig> {

	@Autowired
	private SipAnalysisOptions sipAnalysisOptions;



	@RequestMapping(value = "/index", method = {RequestMethod.GET, RequestMethod.POST})
	public GcsResult index(@RequestBody(required = false) Optional<GcsCondition> payload){
		GcsCondition gcsCondition = payload.orElse(new GcsCondition());
		IPage<GlobalConfig> page = this.page(
				gcsCondition.buildQueryPage(),
				gcsCondition.buildQueryWrapper(GlobalConfig.class)
		);
		page.convert(GlobalConfigController::_entityMap);
		return GcsResult.successResult().wrapPage(page)
				.put("sipAnalysisEnabled", sipAnalysisOptions.isEnabled());
	}

	@GetMapping("/show/{id}")
	public GcsResult show(@PathVariable("id") Long id){
		GlobalConfig entity = this.getById(id);
		if(entity == null){
			return GcsResult.notExistResult().put("id", id);
		}
		return GcsResult.successResult().putAll(_entityMap(entity));
	}
	
	@GLog("保存或更新配置")
	@PostMapping("/saveOrUpdate")
	@RequiresPermissions("globalConfig:saveOrUpdate")
	public GcsResult saveOrUpdate(@RequestBody GcsMap jsonMap){
		String configKey = jsonMap.stringValue("key");
		if(StringUtils.isBlank(configKey)){
			return GcsResult.notExistResult().put("configKey", configKey);
		}
		GlobalConfig entity = Db.getOne(Wrappers.lambdaQuery(GlobalConfig.class).eq(GlobalConfig::getKey, configKey));
		if(entity == null){ entity = new GlobalConfig(); entity.setKey(configKey); }
		GcsBindingUtils.bind(jsonMap, entity);
		ValidatorUtils.validateObj(entity);
		Db.saveOrUpdate(entity);
		return GcsResult.successResult().putAll(_entityMap(entity));
	}

	@GLog("删除全局配置")
	@DeleteMapping("/delete/{id}")
	@RequiresPermissions("globalConfig:delete")
	public GcsResult delete(@PathVariable("id") Long id){
		Boolean isSuccess = this.removeById(id);
		if(!isSuccess){
			return GcsResult.notExistResult().put("id", id);
		}
		return GcsResult.successResult().put("id", id);
	}

	/** ----- 工具方法 ----- */
	public static Map<String, Object> _entityMap(GlobalConfig entity){
		Map<String, Object> entityMap = new HashMap<>();
		entityMap.put("id", entity.getId());
		entityMap.put("key", entity.getKey());
		entityMap.put("value", entity.getValue());
		entityMap.put("remark", entity.getRemark());
		return entityMap;
	}
}
