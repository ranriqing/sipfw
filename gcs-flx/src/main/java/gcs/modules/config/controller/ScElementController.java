package gcs.modules.config.controller;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.baomidou.mybatisplus.extension.toolkit.Db;
import gcs.base.BaseController;
import gcs.modules.config.domain.ScConfig;
import gcs.modules.config.domain.ScElement;
import gcs.modules.config.mapper.ScElementMapper;
import gcs.modules.log.annotation.GLog;
import gcs.base.core.bean.GcsResult;
import gcs.base.core.condition.GcsCondition;
import gcs.base.core.validators.ValidatorUtils;
import gcs.base.tools.GcsBindingUtils;
import org.apache.shiro.authz.annotation.RequiresPermissions;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.*;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/scElement")
public class ScElementController extends ServiceImpl<ScElementMapper, ScElement> {

	@RequestMapping(value = "/index", method = {RequestMethod.GET, RequestMethod.POST})
	public GcsResult index(@RequestBody(required = false) Optional<GcsCondition> payload){
		GcsCondition gcsCondition = payload.orElse(new GcsCondition());
		IPage<ScElement> page = this.page(
				gcsCondition.buildQueryPage(),
				gcsCondition.buildQueryWrapper(ScElement.class)
		);
		page.convert(ScElementController::_entityMap);
		return GcsResult.successResult().wrapPage(page);
	}

	@GetMapping("/show/{id}")
	public GcsResult show(@PathVariable("id") Long id){
		ScElement entity = this.getById(id);
		if(entity == null){
			return GcsResult.notExistResult().put("id", id);
		}
		return GcsResult.successResult().putAll(_entityMap(entity));
	}
	
	@GLog("保存选项元素")
	@PostMapping("/save")
	@RequiresPermissions("scElement:save")
	public GcsResult save1(@RequestBody Map<String, Object> jsonMap){
		ScElement entity = GcsBindingUtils.bind(jsonMap, ScElement.class);
		// 尝试创建ScConfig
		String scConfigKey = (String)jsonMap.get("scConfigKey");
		if(entity.getScConfigId()==null && scConfigKey!=null){
			ScConfig scConfig = Db.getOne(Wrappers.lambdaQuery(ScConfig.class).eq(ScConfig::getKey, scConfigKey));
			if(scConfig == null){
				scConfig = new ScConfig();
				scConfig.setKey(scConfigKey);
				Db.save(scConfig);
			}
			entity.setScConfigId(scConfig.getId());
		}
		ValidatorUtils.validateObj(entity);
		this.save(entity);
		return GcsResult.successResult().putAll(_entityMap(entity));
	}

	@GLog("更新选项元素")
	@PutMapping("/update/{id}")
	@RequiresPermissions("scElement:update")
	public GcsResult update(@PathVariable("id") Long id, @RequestBody Map<String, Object> jsonMap){
		ScElement entity = this.getById(id);
		if(entity == null){
			return GcsResult.notExistResult().put("id", id);
		}
		GcsBindingUtils.bind(jsonMap, entity);
		ValidatorUtils.validateObj(entity);
		this.updateById(entity);
		return GcsResult.successResult().putAll(_entityMap(entity));
	}

	@GLog("删除选项元素")
	@DeleteMapping("/delete/{id}")
	@RequiresPermissions("scElement:delete")
	public GcsResult delete(@PathVariable("id") Long id){
		Boolean isSuccess = this.removeById(id);
		if(!isSuccess){
			return GcsResult.notExistResult().put("id", id);
		}
		return GcsResult.successResult().put("id", id);
	}

	/**
	 * 排序实体
	 * @return
	 */
	@PostMapping("/seqOrders")
	public GcsResult seqOrders(@RequestBody Map<String, Object> jsonMap){
		// 执行操作
		List<ScElement> entityList = new ArrayList<>();
		List<Map<String, Object>> itemList = (List<Map<String, Object>>)jsonMap.get("items");
		for (Map<String, Object> item : itemList) {
			String id = item.get("id").toString();
			if (id != null) {
				ScElement entity = this.getById(Long.parseLong(id));
				if (entity != null) {
					String parentIdStr = (String)item.get("parentId");
					if (parentIdStr != null) {
						entity.setParent(this.getById(Long.parseLong(parentIdStr)));
					}
					entity.setSeqOrder(item.get("seqOrder")!= null?Integer.parseInt(item.get("seqOrder").toString()) : 100);
					this.updateById(entity);
					entityList.add(entity);
				}
			}
		}
		return GcsResult.successResult().singleData(entityList.stream().map(ScElementController::_entityMap).collect(Collectors.toList()));
	}

	/** ----- 工具方法 ----- */
	public static Map<String, Object> _entityMap(ScElement entity){
		Map<String, Object> entityMap = BaseController._entityMap(entity);
		if(entity.getScConfigId()!=null){
			ScConfig scConfig = Db.getById(entity.getScConfigId(), ScConfig.class);
			if(scConfig!=null){
				entityMap.put("scConfigKey", scConfig.getKey());
			}
		}
		return entityMap;
	}
}
