package gcs.app.vm.controller;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import gcs.app.vm.domain.*;
import gcs.app.vm.mapper.*;
import gcs.app.vm.bean.VmCodePoet;
import gcs.app.vm.component.VmCodePoetRegistry;
import gcs.app.vm.utils.VmCacheHolder;
import gcs.app.vm.utils.VmUtils;
import gcs.base.core.bean.GcsResult;
import gcs.base.core.condition.GcsCondition;
import gcs.base.core.validators.ValidatorUtils;
import gcs.base.tools.GcsBindingUtils;
import gcs.modules.log.annotation.GLog;
import org.apache.shiro.authz.annotation.RequiresPermissions;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import cn.hutool.core.convert.Convert;

import java.util.*;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/metaDomain")
public class MetaDomainController extends ServiceImpl<MetaDomainMapper, MetaDomain> {


	@Autowired
	private MetaPageComponentMapper metaPageComponentMapper;
	@Autowired
	private MetaPageComponentFieldMapper metaPageComponentFieldMapper;
	@Autowired
	private MetaPageLinkMapper metaPageLinkMapper;
	@Autowired
	private MetaDomainFieldMapper metaDomainFieldMapper;


	@RequestMapping(value = "/index", method = {RequestMethod.GET, RequestMethod.POST})
	@RequiresPermissions("metaDomain:index")
	public GcsResult index(@RequestBody(required = false) Optional<GcsCondition> payload){
		GcsCondition gcsCondition = payload.orElse(new GcsCondition());
		IPage<MetaDomain> page = this.page(
				gcsCondition.buildQueryPage(),
				gcsCondition.buildQueryWrapper(MetaDomain.class)
		);
		page.convert(MetaDomainController::_entityMap);
		return GcsResult.successResult().wrapPage(page);
	}

	@GetMapping("/show/{id}")
	@RequiresPermissions("metaDomain:show")
	public GcsResult show(@PathVariable("id") Long id){
		MetaDomain entity = this.getById(id);
		if(entity == null){
			return GcsResult.notExistResult().put("id", id);
		}
		return GcsResult.successResult().putAll(_entityMap(entity));
	}

	@GLog("保存数据表")
	@PostMapping("/save")
	@RequiresPermissions("metaDomain:save")
	public GcsResult save1(@RequestBody Map<String, Object> jsonMap){
		MetaDomain entity = GcsBindingUtils.bind(jsonMap, MetaDomain.class);
		entity.setDomainName(VmUtils.toUpperCamel(entity.getDomainName()));
		ValidatorUtils.validateObj(entity);
		this.save(entity);
		VmCacheHolder.cleanVmClassCache(entity.getDomainName());
		return GcsResult.successResult().putAll(_entityMap(entity));
	}

	@GLog("更新数据表")
	@PutMapping("/update/{id}")
	@RequiresPermissions("metaDomain:update")
	public GcsResult update(@PathVariable("id") Long id, @RequestBody Map<String, Object> jsonMap){
		MetaDomain entity = this.getById(id);
		if(entity == null){
			return GcsResult.notExistResult().put("id", id);
		}
		GcsBindingUtils.bind(jsonMap, entity);
		entity.setDomainName(VmUtils.toUpperCamel(entity.getDomainName()));
		ValidatorUtils.validateObj(entity);
		this.updateById(entity);
		VmCacheHolder.cleanVmClassCache(entity.getDomainName());
		return GcsResult.successResult().putAll(_entityMap(entity));
	}

	@GLog("删除数据表")
	@DeleteMapping("/delete/{id}")
	@RequiresPermissions("metaDomain:delete")
	public GcsResult delete(@PathVariable("id") Long id){
		MetaDomain entity = this.getById(id);
		if(entity == null){
			return GcsResult.notExistResult().put("id", id);
		}
		// 删除页面组件
		List<MetaPageComponent> pageComponentList = metaPageComponentMapper.selectList(Wrappers.lambdaQuery(MetaPageComponent.class)
				.eq(MetaPageComponent::getMetaDomainId, entity.getId()));
		List<Long> pageComponentIds = pageComponentList.stream().map(MetaPageComponent::getId).collect(Collectors.toList());
		if(!pageComponentIds.isEmpty()){
			metaPageLinkMapper.delete(Wrappers.lambdaQuery(MetaPageLink.class)
					.in(MetaPageLink::getMetaPageComponentId, pageComponentIds));
			metaPageComponentFieldMapper.delete(Wrappers.lambdaQuery(MetaPageComponentField.class)
					.in(MetaPageComponentField::getMetaPageComponentId, pageComponentIds));
		}
		metaPageComponentMapper.delete(Wrappers.lambdaQuery(MetaPageComponent.class)
				.eq(MetaPageComponent::getMetaDomainId, entity.getId()));
		// 删除Domain字段
		metaDomainFieldMapper.delete(Wrappers.lambdaQuery(MetaDomainField.class)
				.eq(MetaDomainField::getMetaDomainId, entity.getId()));
		Boolean isSuccess = this.removeById(id);
		if(!isSuccess){
			return GcsResult.notExistResult().put("id", id);
		}
		VmCacheHolder.cleanVmClassCache(entity.getDomainName());
		return GcsResult.successResult().put("id", id);
	}

	/**
	 * 排序实体
	 * @return
	 */
	@PostMapping("/seqOrders")
	public GcsResult seqOrders(@RequestBody Map<String, Object> jsonMap){
		// 执行操作
		List<MetaDomain> entityList = new ArrayList<>();
		List<Map<String, Object>> itemList = (List<Map<String, Object>>)jsonMap.get("items");
		for (Map<String, Object> item : itemList) {
			String id = item.get("id").toString();
			if (id != null) {
				MetaDomain entity = this.getById(Long.parseLong(id));
				if (entity != null) {
					entity.setSeqOrder(item.get("seqOrder")!= null?Integer.parseInt(item.get("seqOrder").toString()) : 100);
						if (item.get("sciDomainCategory") != null) {
							entity.setSciDomainCategory(Convert.toLong(item.get("sciDomainCategory")));
						}
					this.updateById(entity);
					entityList.add(entity);
				}
			}
		}
		return GcsResult.successResult().singleData(entityList.stream().map(MetaDomainController::_entityMap).collect(Collectors.toList()));
	}

	/** ----- 工具方法 ----- */
	public static Map<String, Object> _entityMap(MetaDomain entity){
		Map<String, Object> entityMap = new HashMap<>();
		entityMap.put("id", entity.getId());
		entityMap.put("title", entity.getTitle());
		entityMap.put("domainName", entity.getDomainName());
		entityMap.put("seqOrder", entity.getSeqOrder());
			entityMap.put("sciDomainCategory", entity.getSciDomainCategory());
		entityMap.put("visible", entity.getVisible());
		entityMap.put("extendJson", entity.getExtendJson());
		entityMap.put("remark", entity.getRemark());
		// 标识：是否存在对应的 code 实体扩展（VmCodePoet）
		boolean codeEntity = VmCodePoetRegistry.isSupport(entity.getDomainName());
		entityMap.put("codeEntity", codeEntity);
		return entityMap;
	}
}
