package gcs.app.vm.controller;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import gcs.app.vm.domain.MetaDomain;
import gcs.app.vm.domain.MetaDomainField;
import gcs.app.vm.domain.MetaPageComponentField;
import gcs.app.vm.bean.VmCodePoet;
import gcs.app.vm.component.VmCodePoetRegistry;
import gcs.app.vm.mapper.MetaDomainFieldMapper;
import gcs.app.vm.mapper.MetaDomainMapper;
import gcs.app.vm.mapper.MetaPageComponentFieldMapper;
import gcs.app.vm.utils.VmCacheHolder;
import gcs.app.vm.utils.VmUtils;
import gcs.base.BaseController;
import gcs.base.core.GcsHolder;
import gcs.base.core.GcsConstants;
import gcs.modules.log.annotation.GLog;
import gcs.base.core.bean.GcsResult;
import gcs.base.core.condition.GcsCondition;
import gcs.base.core.validators.ValidatorUtils;
import gcs.base.tools.GcsBindingUtils;
import org.apache.shiro.authz.annotation.RequiresPermissions;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.lang.reflect.Field;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/metaDomainField")
public class MetaDomainFieldController extends ServiceImpl<MetaDomainFieldMapper, MetaDomainField> {

	@Autowired
	private MetaDomainMapper metaDomainMapper;
	@Autowired
	private MetaPageComponentFieldMapper metaPageComponentFieldMapper;

	@RequestMapping(value = "/index", method = {RequestMethod.GET, RequestMethod.POST})
	@RequiresPermissions("metaDomainField:index")
	public GcsResult index(@RequestBody(required = false) Optional<GcsCondition> payload){
		GcsCondition gcsCondition = payload.orElse(new GcsCondition());
		IPage<MetaDomainField> page = this.page(
				gcsCondition.buildQueryPage(),
				gcsCondition.buildQueryWrapper(MetaDomainField.class)
		);
		page.convert(MetaDomainFieldController::_entityMap);
		return GcsResult.successResult().wrapPage(page);
	}

	@GetMapping("/show/{id}")
	@RequiresPermissions("metaDomainField:show")
	public GcsResult show(@PathVariable("id") Long id){
		MetaDomainField entity = this.getById(id);
		if(entity == null){
			return GcsResult.notExistResult().put("id", id);
		}
		return GcsResult.successResult().putAll(_entityMap(entity));
	}

	@GLog("保存字段")
	@PostMapping("/save")
	@RequiresPermissions("metaDomainField:save")
	public GcsResult save1(@RequestBody Map<String, Object> jsonMap){
		MetaDomainField entity = GcsBindingUtils.bind(jsonMap, MetaDomainField.class);
		if(GcsConstants.KEYWORDS.contains(entity.getFieldName())){
			return GcsResult.failedResult("字段名[" + entity.getFieldName() + "]为关键字，请更改字段名");
		}
		ValidatorUtils.validateObj(entity);
		this.save(entity);
		MetaDomain metaDomain = metaDomainMapper.selectById(entity.getMetaDomainId());
		String upperDomainName = VmUtils.toUpperCamel(metaDomain.getDomainName());
		VmCacheHolder.cleanVmClassCache(upperDomainName);
		return GcsResult.successResult().putAll(_entityMap(entity));
	}

	@GLog("更新字段")
	@PutMapping("/update/{id}")
	@RequiresPermissions("metaDomainField:update")
	public GcsResult update(@PathVariable("id") Long id, @RequestBody Map<String, Object> jsonMap){
		MetaDomainField entity = this.getById(id);
		if(entity == null){
			return GcsResult.notExistResult().put("id", id);
		}
		GcsBindingUtils.bind(jsonMap, entity);
		ValidatorUtils.validateObj(entity);
		this.updateById(entity);
		MetaDomain metaDomain = metaDomainMapper.selectById(entity.getMetaDomainId());
		String upperDomainName = VmUtils.toUpperCamel(metaDomain.getDomainName());
		VmCacheHolder.cleanVmClassCache(upperDomainName);
		return GcsResult.successResult().putAll(_entityMap(entity));
	}

	@GLog("删除字段")
	@DeleteMapping("/delete/{id}")
	@RequiresPermissions("metaDomainField:delete")
	public GcsResult delete(@PathVariable("id") Long id){
		MetaDomainField entity = this.getById(id);
		if(entity == null){
			return GcsResult.notExistResult().put("id", id);
		}
		metaPageComponentFieldMapper.delete(Wrappers.lambdaQuery(MetaPageComponentField.class)
			.eq(MetaPageComponentField::getMetaDomainFieldId, id));
		Boolean isSuccess = this.removeById(id);
		if(!isSuccess){
			return GcsResult.notExistResult().put("id", id);
		}
		MetaDomain metaDomain = metaDomainMapper.selectById(entity.getMetaDomainId());
		String upperDomainName = VmUtils.toUpperCamel(metaDomain.getDomainName());
		VmCacheHolder.cleanVmClassCache(upperDomainName);
		return GcsResult.successResult().put("id", id);
	}

	/**
	 * 排序实体
	 * @return
	 */
	@PostMapping("/seqOrders")
	public GcsResult seqOrders(@RequestBody Map<String, Object> jsonMap){
		// 执行操作
		List<MetaDomainField> entityList = new ArrayList<>();
		List<Map<String, Object>> itemList = (List<Map<String, Object>>)jsonMap.get("items");
		for (Map<String, Object> item : itemList) {
			String id = item.get("id").toString();
			if (id != null) {
				MetaDomainField entity = this.getById(Long.parseLong(id));
				if (entity != null) {
					entity.setSeqOrder(item.get("seqOrder")!= null?Integer.parseInt(item.get("seqOrder").toString()) : 100);
					this.updateById(entity);
					entityList.add(entity);
				}
			}
		}
		return GcsResult.successResult().singleData(entityList.stream()
				.map(MetaDomainFieldController::_entityMap)
				.collect(Collectors.toList()));
	}

	/** ----- 工具方法 ----- */
	private static final Map<String, Set<String>> CODE_FIELDS_CACHE = new ConcurrentHashMap<>();

	private static Set<String> _codeFieldNames(String domainName){
		if(domainName == null){
			return Collections.emptySet();
		}
		String camelDomainName = VmUtils.toUpperCamel(domainName);
		/* if(!VmCodePoetRegistry.isSupport(camelDomainName)){
			return Collections.emptySet();
		} */
		return CODE_FIELDS_CACHE.computeIfAbsent(camelDomainName, dn -> {
			VmCodePoet vmCodePoet = VmCodePoetRegistry.getVmCodePoet(dn);
			Class<?> superClass = vmCodePoet!=null?vmCodePoet.getSuperClass():null;
			if(superClass == null){
				return Collections.emptySet();
			}
			Set<String> fieldNames = new HashSet<>();
			for (Field f : superClass.getDeclaredFields()) {
				fieldNames.add(f.getName());
			}
			return fieldNames;
		});
	}

	public static Map<String, Object> _entityMap(MetaDomainField entity){
		Map<String, Object> entityMap = BaseController._entityMap(entity);
		entityMap.put("metaDomain", entity.getMetaDomainId());
		// 标识：该字段是否在 code 的 superClass 中已定义（避免与动态字段重复）
		boolean codeField = false;
		MetaDomain metaDomain = GcsHolder.getBean(MetaDomainMapper.class).selectById(entity.getMetaDomainId());
		String domainName = metaDomain!=null?metaDomain.getDomainName():null;
		if(domainName != null){
			codeField = _codeFieldNames(domainName).contains(entity.getFieldName());
		}
		entityMap.put("codeField", codeField);
		return entityMap;
	}
}
