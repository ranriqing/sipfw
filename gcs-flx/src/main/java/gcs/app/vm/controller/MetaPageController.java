package gcs.app.vm.controller;

import cn.hutool.core.collection.ListUtil;
import cn.hutool.core.util.ObjectUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.toolkit.Db;
import com.baomidou.mybatisplus.extension.toolkit.SqlHelper;
import com.google.common.base.CaseFormat;
import gcs.base.BaseController;
import gcs.modules.config.controller.ScConfigController;
import gcs.modules.config.controller.ScElementController;
import gcs.modules.config.domain.ScConfig;
import gcs.modules.config.domain.ScElement;
import gcs.app.ugrp.controller.GcsGroupController;
import gcs.app.ugrp.controller.GcsUserController;
import gcs.app.ugrp.domain.GcsGroup;
import gcs.app.ugrp.domain.GcsRole;
import gcs.app.ugrp.domain.GcsUser;
import gcs.app.ugrp.mapper.GcsGroupMapper;
import gcs.app.ugrp.mapper.GcsUserMapper;
import gcs.app.ugrp.service.GcsRasaService;
import gcs.app.ugrp.service.GcsShiroService;
import gcs.app.vm.domain.*;
import gcs.app.vm.mapper.*;
import gcs.base.core.GcsHolder;
import gcs.base.core.bean.GcsMap;
import gcs.base.core.bean.GcsResult;
import gcs.base.core.bean.GcsState;
import gcs.base.core.condition.GcsCondition;
import gcs.base.core.validators.ValidatorUtils;
import gcs.base.shiro.spi.SpiUser;
import gcs.base.tools.GcsBindingUtils;
import org.apache.commons.lang.RandomStringUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.shiro.authz.annotation.RequiresPermissions;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.*;
import java.util.AbstractMap.SimpleEntry;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@RestController
@RequestMapping("/orm")
public class MetaPageController{
	private Logger logger = LoggerFactory.getLogger(getClass());

	@Autowired
	private GcsRasaService gcsRasaService;
	@Autowired
	private GcsShiroService gcsShiroService;
	@Autowired
	private GcsUserMapper gcsUserMapper;
	@Autowired
	private GcsGroupMapper gcsGroupMapper;
	@Autowired
	private MetaViewMapper metaViewMapper;
	@Autowired
	private MetaViewMenuMapper metaViewMenuMapper;
	@Autowired
	private MetaPageMapper metaPageMapper;
	@Autowired
	private MetaPageLinkMapper metaPageLinkMapper;
	@Autowired
	private MetaPageComponentMapper metaPageComponentMapper;
	@Autowired
	private MetaPageComponentFieldMapper metaPageComponentFieldMapper;

	private static Map<String, Class<?>> pageMetaMap = Stream.of(
		new SimpleEntry<>("metaView", MetaView.class),
		new SimpleEntry<>("metaViewMenu", MetaViewMenu.class),
		new SimpleEntry<>("metaPage", MetaPage.class),
		new SimpleEntry<>("metaPageLink", MetaPageLink.class),
		new SimpleEntry<>("metaPageComponent", MetaPageComponent.class),
		new SimpleEntry<>("metaPageComponentField", MetaPageComponentField.class),
		new SimpleEntry<>("metaPermission", MetaPermission.class)
	).collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue, (e1, e2) -> e1, LinkedHashMap::new));


	@RequestMapping(value = "/metas", method = {RequestMethod.GET, RequestMethod.POST})
	@RequiresPermissions("orm:metas")
	public GcsResult metas(){
		Map<String, Object> metaMap = new HashMap<>();

		// 用户组织
		QueryWrapper<GcsGroup> groupRasaQueryWrapper = gcsRasaService.query(Wrappers.query(), GcsGroup.class.getSimpleName(), new HashMap<>());
		List<GcsGroup> gcsGroupList = gcsGroupMapper.selectList(groupRasaQueryWrapper);
		metaMap.put("gcsGroup", gcsGroupList.stream().map(MetaPageController::_entityMap).collect(Collectors.toList()));
		QueryWrapper<GcsUser> userRasaQueryWrapper = gcsRasaService.query(Wrappers.query(), GcsUser.class.getSimpleName(), new HashMap<>());
		List<GcsUser> gcsUserList = gcsUserMapper.selectList(userRasaQueryWrapper);
		metaMap.put("gcsUser", gcsUserList.stream().map(MetaPageController::_entityMap).collect(Collectors.toList()));
		// 实体信息
		Stream.of(
			new SimpleEntry<>("metaDomain", MetaDomain.class),
			new SimpleEntry<>("metaDomainField", MetaDomainField.class),
			new SimpleEntry<>("metaPermission", MetaPermission.class),
			new SimpleEntry<>("scConfig", ScConfig.class),
			new SimpleEntry<>("scElement", ScElement.class)
		).collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue, (e1, e2) -> e1, LinkedHashMap::new))
			.forEach((String domainName, Class<?> domainClazz) -> {
			String domainMapperName = domainName + "Mapper";
			if(domainClazz!=null && GcsHolder.containsBean(domainMapperName)){
				BaseMapper dynamicMapper = (BaseMapper)GcsHolder.getBean(domainMapperName);
				List<Object> domainList = dynamicMapper.selectList(Wrappers.query());
				metaMap.put(domainName, domainList.stream().map(MetaPageController::_entityMap).collect(Collectors.toList()));
			}
		});
		// 可见数据
		List<Long> viewIdList = new ArrayList<>();
		SpiUser spiUser = gcsShiroService.currentUser();
		if(!spiUser.isSuperAdmin()){
			viewIdList = metaViewMapper.selectList(Wrappers.query()).stream().map(MetaView::getId).collect(Collectors.toList());
		}else if(spiUser.getRoleId()!=null){
			GcsRole userRole = Db.getById(spiUser.getRoleId(), GcsRole.class);
			viewIdList = Arrays.asList(userRole.getViewId());
		}
		List<MetaView> metaViewList = new ArrayList<>();
		List<MetaViewMenu> metaViewMenuList = new ArrayList<>();
		List<MetaPage> metaPageList = new ArrayList<>();
		List<MetaPageLink> metaPageLinkList = new ArrayList<>();
		List<MetaPageComponent> metaPageComponentList = new ArrayList<>();
		List<MetaPageComponentField> metaPageComponentFieldList = new ArrayList<>();
		for (Map.Entry<String, Class<?>> entry : pageMetaMap.entrySet()) {
			String domainName = entry.getKey();
			if("metaView".equals(domainName)){
				if(!viewIdList.isEmpty()){
					metaViewList = metaViewMapper.selectList(Wrappers.query());
				}
				metaMap.put(domainName, metaViewList.stream().map(MetaPageController::_entityMap).collect(Collectors.toList()));
			}else if("metaViewMenu".equals(domainName)){
				if(!metaViewList.isEmpty()){
					metaViewMenuList = metaViewMenuMapper.selectList(
						Wrappers.lambdaQuery(MetaViewMenu.class).in(
							MetaViewMenu::getMetaViewId,
							metaViewList.stream().map(MetaView::getId).collect(Collectors.toList()))
					);
				}
				metaMap.put(domainName, metaViewMenuList.stream().map(MetaPageController::_entityMap).collect(Collectors.toList()));
			}else if("metaPage".equals(domainName)){
				if(!metaViewList.isEmpty()){
					metaPageList = metaPageMapper.selectList(
						Wrappers.lambdaQuery(MetaPage.class).in(
							MetaPage::getMetaViewId,
							metaViewList.stream().map(MetaView::getId).collect(Collectors.toList()))
					);
				}
				metaMap.put(domainName, metaPageList.stream().map(MetaPageController::_entityMap).collect(Collectors.toList()));
			}else if("metaPageLink".equals(domainName)){
				if(!metaPageList.isEmpty()){
					metaPageLinkList = metaPageLinkMapper.selectList(
						Wrappers.lambdaQuery(MetaPageLink.class).in(
							MetaPageLink::getMetaPageId,
							metaPageList.stream().map(MetaPage::getId).collect(Collectors.toList()))
					);
				}
				metaMap.put(domainName, metaPageLinkList.stream().map(MetaPageController::_entityMap).collect(Collectors.toList()));
			}else if("metaPageComponent".equals(domainName)){
				final List<MetaPageLink> finalMetaPageLinkList = metaPageLinkList;
				if(!metaPageLinkList.isEmpty()){
					metaPageComponentList = metaPageComponentMapper.selectList(
						Wrappers.lambdaQuery(MetaPageComponent.class).in(
								MetaPageComponent::getId,
								finalMetaPageLinkList.stream().map(MetaPageLink::getMetaPageComponentId).collect(Collectors.toList())
						).or( wrapper -> {
							wrapper.in(MetaPageComponent::getType, Arrays.asList(
									"FIELD_COMPONENT",                       // 字段关联组件
									"FIELD_POP_ADD", "FIELD_POP_EDIT",       // 字段弹窗组件
									"PART_TAB"                               // Tab组件
							));
						})
					);
				}
				metaMap.put(domainName, metaPageComponentList.stream().map(MetaPageController::_entityMap).collect(Collectors.toList()));
			}else if("metaPageComponentField".equals(domainName)){
				if(!metaPageComponentList.isEmpty()){
					metaPageComponentFieldList = metaPageComponentFieldMapper.selectList(
						Wrappers.lambdaQuery(MetaPageComponentField.class).in(
							MetaPageComponentField::getMetaPageComponentId,
							metaPageComponentList.stream().map(MetaPageComponent::getId).collect(Collectors.toList()))
					);
				}
				metaMap.put(domainName, metaPageComponentFieldList.stream().map(MetaPageController::_entityMap).collect(Collectors.toList()));
			}
		}
		return GcsResult.successResult().putAll(metaMap);
	}

	/**
	 * 单个页面
	 */
	@GetMapping("/singlePage/{id}")
	@ResponseBody
	public GcsResult singlePage(@PathVariable Long id) {
		Map<String, Object> metaMap = new HashMap<>();

		// 页面组件
		List<MetaPage> metaPageList = metaPageMapper.selectList(
				Wrappers.lambdaQuery(MetaPage.class).eq(MetaPage::getId, id)
		);
		List<MetaPageLink> metaPageLinkList = metaPageList.isEmpty() ?
			ListUtil.list(false) :
			metaPageLinkMapper.selectList(
				Wrappers.lambdaQuery(MetaPageLink.class).in(
					MetaPageLink::getMetaPageId,
					metaPageList.stream().map(MetaPage::getId).collect(Collectors.toList()))
			);

		List<MetaPageComponent> metaPageComponentList = metaPageLinkList.isEmpty() ?
			ListUtil.list(false) :
			metaPageComponentMapper.selectList(
				Wrappers.lambdaQuery(MetaPageComponent.class).in(
					MetaPageComponent::getId,
					metaPageLinkList.stream().map(MetaPageLink::getMetaPageComponentId).collect(Collectors.toList()))
			);

		List<MetaPageComponentField> metaPageComponentFieldList = metaPageComponentList.isEmpty() ?
			ListUtil.list(false) :
			metaPageComponentFieldMapper.selectList(
				Wrappers.lambdaQuery(MetaPageComponentField.class).in(
					MetaPageComponentField::getMetaPageComponentId,
					metaPageComponentList.stream().map(MetaPageComponent::getId).collect(Collectors.toList()))
			);

		// 包装并返回结果
		metaMap.put("metaPage", metaPageList.stream().map(MetaPageController::_entityMap).collect(Collectors.toList()));
		metaMap.put("metaPageLink", metaPageLinkList.stream().map(MetaPageController::_entityMap).collect(Collectors.toList()));
		metaMap.put("metaPageComponent", metaPageComponentList.stream().map(MetaPageController::_entityMap).collect(Collectors.toList()));
		metaMap.put("metaPageComponentField", metaPageComponentFieldList.stream().map(MetaPageController::_entityMap).collect(Collectors.toList()));
		return GcsResult.successResult().putAll(metaMap);
	}

	/**
	 * 拷贝视图
	 */
	@PostMapping("/copyView")
	@ResponseBody
	public GcsResult copyView(@RequestBody GcsMap gcsMap) {
		// 参数校验
		Long viewId = gcsMap.longValue("viewId");
		String copyViewName = gcsMap.stringValue("copyViewName");

		if (viewId == null || copyViewName == null) {
			return GcsResult.stateResult(GcsState.PARAM_INVALID, "缺少必要参数");
		}

		MetaView srcMetaView = metaViewMapper.selectById(viewId);
		if (srcMetaView == null) {
			return GcsResult.stateResult(GcsState.NOT_EXIST, "源视图不存在");
		}

		// 开始拷贝
		Map<String, Object> metaMap = new HashMap<>();

		// 拷贝视图
		MetaView dstMetaView = new MetaView();
		dstMetaView.setName(copyViewName);
		dstMetaView.setType(srcMetaView.getType());
		dstMetaView.setIcon(srcMetaView.getIcon());
		dstMetaView.setSeqOrder(srcMetaView.getSeqOrder());
		dstMetaView.setActive(false);
		SqlHelper.retBool(metaViewMapper.insert(dstMetaView));
		metaMap.put("metaView", Collections.singletonList(dstMetaView));

		// 拷贝菜单
		Map<Long, MetaViewMenu> oldIdentToDstMenuMap = new HashMap<>();
		_copyMenuRecurse(srcMetaView, dstMetaView, null, null, oldIdentToDstMenuMap);
		metaMap.put("metaViewMenu", new ArrayList<>(oldIdentToDstMenuMap.values()));

		// 拷贝页面
		List<MetaPage> dstMetaPageList = new ArrayList<>();
		List<MetaPageLink> dstMetaPageLinkList = new ArrayList<>();

		List<MetaPage> srcMetaPageList = metaPageMapper.selectList(Wrappers.lambdaQuery(MetaPage.class).eq(MetaPage::getMetaViewId, srcMetaView.getId()));
		for (MetaPage srcMetaPage : srcMetaPageList) {
			MetaPage dstMetaPage = new MetaPage();
			dstMetaPage.setIdent(RandomStringUtils.randomAlphabetic(srcMetaPage.getIdent().length()).toLowerCase());
			dstMetaPage.setMetaViewId(dstMetaView.getId());
			if(srcMetaPage.getMetaViewMenuId()!=null) {
				MetaViewMenu dstMenu = oldIdentToDstMenuMap.get(srcMetaPage.getMetaViewMenuId());
				dstMetaPage.setMetaViewMenuId(dstMenu!=null?dstMenu.getId():null);
			}
			dstMetaPage.setPdent(srcMetaPage.getPdent());
			dstMetaPage.setTitle(srcMetaPage.getTitle());
			dstMetaPage.setComponent(srcMetaPage.getComponent());
			dstMetaPage.setSeqOrder(srcMetaPage.getSeqOrder());
			dstMetaPage.setInitialized(srcMetaPage.getInitialized());
			dstMetaPage.setExtend(srcMetaPage.getExtend());
			dstMetaPage.setRemark(srcMetaPage.getRemark());
			SqlHelper.retBool(metaPageMapper.insert(dstMetaPage));
			dstMetaPageList.add(dstMetaPage);

			// 页面组件
			List<MetaPageLink> srcMetaPageLinkList = metaPageLinkMapper.selectList(Wrappers.lambdaQuery(MetaPageLink.class).eq(MetaPageLink::getMetaPageId, srcMetaPage.getId()));
			for (MetaPageLink srcMetaPageLink : srcMetaPageLinkList) {
				MetaPageLink dstMetaPageLink = new MetaPageLink();
				Long srcMetaPageComponentId = srcMetaPageLink.getMetaPageComponentId();
				dstMetaPageLink.setSeqOrder(srcMetaPageLink.getSeqOrder());
				dstMetaPageLink.setMetaPageId(dstMetaPage.getId());
				dstMetaPageLink.setMetaPageComponentId(srcMetaPageComponentId); // 仅创建关联
				SqlHelper.retBool(metaPageLinkMapper.insert(dstMetaPageLink));
				dstMetaPageLinkList.add(dstMetaPageLink);
			}
		}

		// 包装页面并返回结果
		metaMap.put("metaPage", dstMetaPageList.stream().map(MetaPageController::_entityMap).collect(Collectors.toList()));
		metaMap.put("metaPageLink", dstMetaPageLinkList.stream().map(MetaPageController::_entityMap).collect(Collectors.toList()));
		metaMap.put("metaPageComponent", ListUtil.list(false));       // 未实际拷贝组件
		metaMap.put("metaPageComponentField", ListUtil.list(false));  // 未实际拷贝字段
		return GcsResult.successResult().putAll(metaMap);
	}

	/**
	 * 拷贝页面
	 */
	@PostMapping("/copyPage")
	@ResponseBody
	public GcsResult copyPage(@RequestBody GcsMap gcsMap) {
		// 参数校验
		Long pageId = gcsMap.longValue("pageId");
		MetaPage srcMetaPage = metaPageMapper.selectById(pageId);
		if (srcMetaPage == null) {
			return GcsResult.stateResult(GcsState.NOT_EXIST, "源页面不存在");
		}

		// 拷贝页面
		Map<String, Object> metaMap = new HashMap<>();
		List<MetaPage> dstMetaPageList = new ArrayList<>();
		List<MetaPageLink> dstMetaPageLinkList = new ArrayList<>();
		List<Long> viewIds = gcsMap.getList("checkViewIds", Long.class);
		for (Long viewId : viewIds) {
			MetaView dstMetaView = metaViewMapper.selectById(viewId);
			if (dstMetaView != null) {
				// 同pdent目标页面(优先绑定菜单的页面)
				List<MetaPage> relMetaPageList = metaPageMapper.selectList(
					Wrappers.lambdaQuery(MetaPage.class)
						.eq(MetaPage::getMetaViewId, dstMetaView.getId())
						.eq(MetaPage::getPdent, srcMetaPage.getPdent())
						.orderByAsc(MetaPage::getSeqOrder)
						.orderByDesc(MetaPage::getId)
				);
				MetaPage overwriteMetaPage = relMetaPageList.stream()
						.filter(p -> p.getMetaViewMenuId() != null)
						.findFirst()
						.orElse(relMetaPageList.isEmpty() ? null : relMetaPageList.get(0));
				// 拷贝到新页面
				MetaPage dstMetaPage = new MetaPage();
				dstMetaPage.setIdent(RandomStringUtils.randomAlphabetic(srcMetaPage.getIdent().length()).toLowerCase());
				dstMetaPage.setMetaViewId(dstMetaView.getId());
				if(overwriteMetaPage != null){
					dstMetaPage.setMetaViewMenuId(overwriteMetaPage.getMetaViewMenuId());
				}
				dstMetaPage.setPdent(srcMetaPage.getPdent());
				dstMetaPage.setTitle(srcMetaPage.getTitle());
				dstMetaPage.setComponent(srcMetaPage.getComponent());
				dstMetaPage.setSeqOrder(0); // 最新页面
				dstMetaPage.setInitialized(srcMetaPage.getInitialized());
				dstMetaPage.setExtend(srcMetaPage.getExtend());
				dstMetaPage.setRemark(srcMetaPage.getRemark());
				SqlHelper.retBool(metaPageMapper.insert(dstMetaPage));
				dstMetaPageList.add(dstMetaPage);

				// 拷贝页面组件
				List<MetaPageLink> srcMetaPageLinkList = metaPageLinkMapper.selectList(Wrappers.lambdaQuery(MetaPageLink.class).eq(MetaPageLink::getMetaPageId, srcMetaPage.getId()));
				for (MetaPageLink srcMetaPageLink : srcMetaPageLinkList) {
					MetaPageLink dstMetaPageLink = new MetaPageLink();
					dstMetaPageLink.setSeqOrder(srcMetaPageLink.getSeqOrder());
					dstMetaPageLink.setMetaPageId(dstMetaPage.getId());
					dstMetaPageLink.setMetaPageComponentId(srcMetaPageLink.getMetaPageComponentId()); // 仅创建关联
					SqlHelper.retBool(metaPageLinkMapper.insert(dstMetaPageLink));
					dstMetaPageLinkList.add(dstMetaPageLink);
				}
			}
		}

		// 包装页面并返回结果
		metaMap.put("metaPage", dstMetaPageList.stream().map(MetaPageController::_entityMap).collect(Collectors.toList()));
		metaMap.put("metaPageLink", dstMetaPageLinkList.stream().map(MetaPageController::_entityMap).collect(Collectors.toList()));
		metaMap.put("metaPageComponent", ListUtil.list(false));       // 未实际拷贝组件
		metaMap.put("metaPageComponentField", ListUtil.list(false));  // 未实际拷贝字段
		return GcsResult.successResult().putAll(metaMap);
	}

	/**
	 * 拷贝页面
	 */
	@PostMapping("/copyComponent")
	@ResponseBody
	public GcsResult copyComponent(@RequestBody GcsMap gcsMap) {
		// 参数校验
		Long srcComponentId = gcsMap.longValue("srcComponentId", 0L);
		MetaPageComponent srcMetaPageComponent = metaPageComponentMapper.selectById(srcComponentId);
		if (srcMetaPageComponent == null) {
			return GcsResult.stateResult(GcsState.NOT_EXIST, "源组件不存在");
		}

		// 目标参数(可选)
		String dstType = gcsMap.stringValue("dstType", srcMetaPageComponent.getType());
		String dstComponent = gcsMap.stringValue("dstComponent", srcMetaPageComponent.getComponent());
		Long dstMetaDomainId = gcsMap.longValue("dstMetaDomainId", srcMetaPageComponent.getMetaDomainId());
		String dstTitle = gcsMap.stringValue("dstTitle", null);
		if (dstTitle == null) {
			String srcTitle = srcMetaPageComponent.getTitle() != null ? srcMetaPageComponent.getTitle() : "组件";
			dstTitle = srcTitle + " - 拷贝";
		}

		// seqOrder: 放到当前Domain下的末尾
		List<MetaPageComponent> relComponents = metaPageComponentMapper.selectList(
				Wrappers.lambdaQuery(MetaPageComponent.class)
						.eq(MetaPageComponent::getMetaDomainId, dstMetaDomainId)
		);
		int maxSeqOrder = relComponents.stream()
				.map(MetaPageComponent::getSeqOrder)
				.filter(Objects::nonNull)
				.max(Integer::compareTo)
				.orElse(0);

		// 拷贝组件
		MetaPageComponent dstMetaPageComponent = new MetaPageComponent();
		dstMetaPageComponent.setTitle(dstTitle);
		dstMetaPageComponent.setPIdent(srcMetaPageComponent.getPIdent());
		dstMetaPageComponent.setIdent(_copyComponentIdent(srcMetaPageComponent.getIdent()));
		dstMetaPageComponent.setType(dstType);
		dstMetaPageComponent.setComponent(dstComponent);
		dstMetaPageComponent.setSeqOrder(maxSeqOrder + 1);
		dstMetaPageComponent.setInitialized(false);
		dstMetaPageComponent.setExtend(srcMetaPageComponent.getExtend());
		dstMetaPageComponent.setMetaDomainId(dstMetaDomainId);
		SqlHelper.retBool(metaPageComponentMapper.insert(dstMetaPageComponent));

		// 拷贝字段
		List<MetaPageComponentField> srcFields = metaPageComponentFieldMapper.selectList(
				Wrappers.lambdaQuery(MetaPageComponentField.class)
						.eq(MetaPageComponentField::getMetaPageComponentId, srcMetaPageComponent.getId())
						.orderByAsc(MetaPageComponentField::getSeqOrder)
						.orderByAsc(MetaPageComponentField::getId)
		);
		List<MetaPageComponentField> dstFields = new ArrayList<>();
		for (MetaPageComponentField srcField : srcFields) {
			MetaPageComponentField dstField = new MetaPageComponentField();
			dstField.setTitle(srcField.getTitle());
			dstField.setField(srcField.getField());
			dstField.setType(srcField.getType());
			dstField.setComponent(srcField.getComponent());
			dstField.setSeqOrder(srcField.getSeqOrder());
			dstField.setPlaceholder(srcField.getPlaceholder());
			dstField.setPIdent(srcField.getPIdent());
			dstField.setExtend(srcField.getExtend());
			dstField.setMetaPageComponentId(dstMetaPageComponent.getId());
			dstField.setMetaDomainFieldId(srcField.getMetaDomainFieldId());
			SqlHelper.retBool(metaPageComponentFieldMapper.insert(dstField));
			dstFields.add(dstField);
		}

		// 包装并返回结果
		Map<String, Object> metaMap = new HashMap<>();
		metaMap.put("metaPageComponent", Collections.singletonList(dstMetaPageComponent));
		metaMap.put("metaPageComponentField", dstFields);
		return GcsResult.successResult().putAll(metaMap);
	}

	// 拷贝组件Ident
	private String _copyComponentIdent(String srcIdent) {
		// NOTE: 前端常用格式 comp_xxxxxxxx，这里尽量沿用前缀
		String rand = RandomStringUtils.randomAlphanumeric(8).toLowerCase();
		if (StringUtils.isBlank(srcIdent)) {
			return "comp_" + rand;
		}
		if (srcIdent.startsWith("comp_")) {
			return "comp_" + rand;
		}
		// 保持原长度(如果较短则直接使用随机)
		if (srcIdent.length() <= 4) {
			return rand;
		}
		return RandomStringUtils.randomAlphanumeric(srcIdent.length()).toLowerCase();
	}

	// 递归拷贝菜单
	private void _copyMenuRecurse(MetaView srcMetaView, MetaView dstMetaView,
								 MetaViewMenu oldParent, MetaViewMenu newParent,
								 Map<Long, MetaViewMenu> oldIdentToDstMenuMap) {
		LambdaQueryWrapper<MetaViewMenu> queryWrapper = Wrappers.lambdaQuery(MetaViewMenu.class)
				.eq(MetaViewMenu::getMetaViewId, srcMetaView.getId());
		if(oldParent==null){
			queryWrapper.isNull(MetaViewMenu::getParentId);
		}else{
			queryWrapper.eq(MetaViewMenu::getParentId, oldParent.getId());
		}
		List<MetaViewMenu> srcMetaViewMenuList = metaViewMenuMapper.selectList(queryWrapper);
		for (MetaViewMenu srcEntity : srcMetaViewMenuList) {
			MetaViewMenu dstEntity = new MetaViewMenu();
			if(newParent!=null){
				dstEntity.setParentId(newParent.getId());
			}
			dstEntity.setMetaViewId(dstMetaView.getId());
			dstEntity.setTitle(srcEntity.getTitle());
			dstEntity.setIcon(srcEntity.getIcon());
			dstEntity.setSeqOrder(srcEntity.getSeqOrder());
			dstEntity.setBindHref(srcEntity.getBindHref());
			dstEntity.setRemark(srcEntity.getRemark());
			SqlHelper.retBool(metaViewMenuMapper.insert(dstEntity));
			oldIdentToDstMenuMap.put(srcEntity.getId(), dstEntity);
			_copyMenuRecurse(srcMetaView, dstMetaView, srcEntity, dstEntity, oldIdentToDstMenuMap);
		}
	}

	@RequestMapping(value = "/{domainName}/index", method = {RequestMethod.GET, RequestMethod.POST})
	// @RequiresPermissions("gcsRole:index")
	public GcsResult index(@PathVariable("domainName") String domainName, @RequestBody(required = false) Optional<GcsCondition> payload){
		GcsCondition gcsCondition = payload.orElse(new GcsCondition());
		Class<?> domainClazz = pageMetaMap.get(domainName);
		String domainMapperName = domainName + "Mapper";
		if(domainClazz==null || !GcsHolder.containsBean(domainMapperName)){
			return GcsResult.notExistResult().put("domainName", domainName);
		}
		BaseMapper dynamicMapper = (BaseMapper)GcsHolder.getBean(domainMapperName);
		IPage<?> page = dynamicMapper.selectPage(
			gcsCondition.buildQueryPage(),
			gcsRasaService.query(
				gcsCondition.buildQueryWrapper(domainClazz),
				domainClazz.getSimpleName(), new HashMap<>()
			)
		);
		page.convert(MetaPageController::_entityMap);
		return GcsResult.successResult().wrapPage(page);
	}

	@GetMapping("/{domainName}/show/{id}")
	// @RequiresPermissions("gcsRole:show")
	public GcsResult show(@PathVariable("domainName") String domainName, @PathVariable("id") Long id){
		Class<?> domainClazz = pageMetaMap.get(domainName);
		String domainMapperName = domainName + "Mapper";
		if(domainClazz==null || !GcsHolder.containsBean(domainMapperName)){
			return GcsResult.notExistResult().put("domainName", domainName);
		}
		BaseMapper dynamicMapper = (BaseMapper)GcsHolder.getBean(domainMapperName);
		Object entity = dynamicMapper.selectById(id);
		if(entity == null){
			return GcsResult.notExistResult().put("id", id);
		}
		return GcsResult.successResult().putAll(_entityMap(entity));
	}

	@PostMapping("/{domainName}/save")
	// @RequiresPermissions("gcsRole:save")
	public GcsResult save1(@PathVariable("domainName") String domainName, @RequestBody Map<String, Object> jsonMap){
		Class<?> domainClazz = pageMetaMap.get(domainName);
		String domainMapperName = domainName + "Mapper";
		if(domainClazz==null || !GcsHolder.containsBean(domainMapperName)){
			return GcsResult.notExistResult().put("domainName", domainName);
		}
		BaseMapper dynamicMapper = (BaseMapper)GcsHolder.getBean(domainMapperName);
		Object entity = GcsBindingUtils.bind(jsonMap, domainClazz);
		ValidatorUtils.validateObj(entity);
		SqlHelper.retBool(dynamicMapper.insert(entity));
		return GcsResult.successResult().putAll(_entityMap(entity));
	}

	@PutMapping("/{domainName}/update/{id}")
	// @RequiresPermissions("gcsRole:update")
	public GcsResult update(@PathVariable("domainName") String domainName, @PathVariable("id") Long id, @RequestBody Map<String, Object> jsonMap){
		Class<?> domainClazz = pageMetaMap.get(domainName);
		String domainMapperName = domainName + "Mapper";
		if(domainClazz==null || !GcsHolder.containsBean(domainMapperName)){
			return GcsResult.notExistResult().put("domainName", domainName);
		}
		BaseMapper dynamicMapper = (BaseMapper)GcsHolder.getBean(domainMapperName);
		Object entity = dynamicMapper.selectById(id);
		if(entity == null){
			return GcsResult.notExistResult().put("id", id);
		}
		GcsBindingUtils.bind(jsonMap, entity);
		ValidatorUtils.validateObj(entity);
		SqlHelper.retBool(dynamicMapper.updateById(entity));
		return GcsResult.successResult().putAll(_entityMap(entity));
	}

	@DeleteMapping("/{domainName}/delete/{id}")
	// @RequiresPermissions("gcsRole:delete")
	public GcsResult delete(@PathVariable("domainName") String domainName, @PathVariable("id") Long id){
		Class<?> domainClazz = pageMetaMap.get(domainName);
		String domainMapperName = domainName + "Mapper";
		if(domainClazz==null || !GcsHolder.containsBean(domainMapperName)){
			return GcsResult.notExistResult().put("domainName", domainName);
		}
		// 获取Mapper
		BaseMapper dynamicMapper = (BaseMapper)GcsHolder.getBean(domainMapperName);
		List<Map<String, Object>> entityList = dynamicMapper.selectMaps(Wrappers.query().eq("id", id));
		if(entityList.isEmpty()){
			return GcsResult.notExistResult().put("id", id);
		}
		// 递归删除关联
		this._delRelations(domainName, entityList.get(0), ListUtil.list(false));
		// 特殊处理组件
		if(domainClazz.equals(MetaViewMenu.class)){
			metaPageMapper.update(
					Wrappers.lambdaUpdate(MetaPage.class)
							.eq(MetaPage::getMetaViewMenuId, entityList.get(0).get("id"))
							.set(MetaPage::getMetaViewMenuId, null)
			);
		}
		if(domainClazz.equals(MetaPageComponentField.class)){
			if("RefInverse".equals(entityList.get(0).get("type"))){
				List<Map<String, Object>> relComponentMapList = metaPageComponentMapper.selectMaps(
						Wrappers.lambdaQuery(MetaPageComponent.class).in(MetaPageComponent::getType, Arrays.asList("FIELD_POP_ADD", "FIELD_POP_EDIT"))
				);
				relComponentMapList.forEach(relComponentMap -> {
					this._delRelations("metaPageComponent", relComponentMap, ListUtil.list(false));
				});
			}
		}
		Boolean isSuccess = SqlHelper.retBool(dynamicMapper.deleteById(id));
		if(!isSuccess){
			return GcsResult.notExistResult().put("id", id);
		}
		return GcsResult.successResult().put("id", id);
	}

	public void _delRelations(String domainName, Map<String, Object> entityMap, List<String> deletedList){
		Map<String, String> relMap = _relationConfig(domainName);
		if(ObjectUtil.isNull(relMap)){ return; }
		relMap.forEach((relDomainName, relFieldName) -> {
			Class<?> relDomainClazz = pageMetaMap.get(relDomainName);
			String relDomainMapperName = relDomainName + "Mapper";
			BaseMapper dynamicMapper = (BaseMapper)GcsHolder.getBean(relDomainMapperName);
			Long entityId = (Long)entityMap.get("id");
			String relDomainField = CaseFormat.UPPER_CAMEL.to(CaseFormat.LOWER_UNDERSCORE, relFieldName);
			List<Map<String, Object>> relEntityList = dynamicMapper.selectMaps(Wrappers.query().eq(relDomainField, entityId));
			relEntityList.forEach(relEntityMap -> {
				_delRelations(relDomainName, relEntityMap, deletedList);
				String entityIdent = relEntityMap.get("id") + "@" + relDomainName;
				if(!deletedList.contains(entityIdent)){
					// 特殊处理组件
					if(relDomainClazz.equals(MetaViewMenu.class)){
						metaPageMapper.update(
								Wrappers.lambdaUpdate(MetaPage.class)
										.eq(MetaPage::getMetaViewMenuId, relEntityMap.get("id"))
										.set(MetaPage::getMetaViewMenuId, null)
						);
					}
					if(relDomainClazz.equals(MetaPageComponentField.class)){
						if("RefInverse".equals(relEntityMap.get("type"))){
							List<Map<String, Object>> relComponentMapList = metaPageComponentMapper.selectMaps(
									Wrappers.lambdaQuery(MetaPageComponent.class).in(MetaPageComponent::getType, Arrays.asList("FIELD_POP_ADD", "FIELD_POP_EDIT"))
							);
							relComponentMapList.forEach(relComponentMap -> {
								this._delRelations("metaPageComponent", relComponentMap, deletedList);
							});
						}
					}
					dynamicMapper.deleteById(relEntityMap.get("id"));
					deletedList.add(entityIdent);
				}
			});
		});
	}

	@PostMapping(value = "/{domainName}/seqOrders")
	public GcsResult seqOrders(@PathVariable("domainName") String domainName, @RequestBody Map<String, Object> jsonMap){
		Class<?> domainClazz = pageMetaMap.get(domainName);
		String domainMapperName = domainName + "Mapper";
		if(domainClazz==null || !GcsHolder.containsBean(domainMapperName)){
			return GcsResult.notExistResult().put("domainName", domainName);
		}
		BaseMapper dynamicMapper = (BaseMapper)GcsHolder.getBean(domainMapperName);
		// 执行操作
		List<Object> entityList = new ArrayList<>();
		List<Map<String, Object>> itemList = (List<Map<String, Object>>)jsonMap.get("items");
		for (Map<String, Object> item : itemList) {
			String id = item.get("id").toString();
			if (id != null) {
				Integer seqOrder = item.get("seqOrder")!= null?Integer.parseInt(item.get("seqOrder").toString()):100;
				Long parentId = item.get("parentId")!= null?Long.parseLong(item.get("parentId").toString()):null;
				Object entity = dynamicMapper.selectById(Long.parseLong(id));
				if (entity != null) {
					if(entity instanceof MetaView){
						((MetaView)entity).setSeqOrder(seqOrder);
					}else if(entity instanceof MetaViewMenu){
						((MetaViewMenu)entity).setSeqOrder(seqOrder);
						if(parentId!=null){
							((MetaViewMenu)entity).setParentId(parentId);
						}
					}else if(entity instanceof MetaPage){
						((MetaPage)entity).setSeqOrder(seqOrder);
					}else if(entity instanceof MetaPageLink){
						((MetaPageLink)entity).setSeqOrder(seqOrder);
					}else if(entity instanceof MetaPageComponent){
						((MetaPageComponent)entity).setSeqOrder(seqOrder);
					}else if(entity instanceof MetaPageComponentField){
						((MetaPageComponentField)entity).setSeqOrder(seqOrder);
					}else if(entity instanceof MetaPermission){
						((MetaPermission)entity).setSeqOrder(seqOrder);
					}
					dynamicMapper.updateById(entity);
					entityList.add(entity);
				}
			}
		}
		return GcsResult.successResult().singleData(entityList.stream().map(MetaPageController::_entityMap).collect(Collectors.toList()));
	}

	private static Map<String, String> _relationConfig(String domainName) {
		// 关联实体->关联字段映射(relDomainName->relFieldName)
		Map<String, Map<String, String>> relationMap = new TreeMap<>();

		// 元视图关系
		Map<String, String> metaViewRelations = new TreeMap<>();
		metaViewRelations.put("metaViewMenu", "metaViewId");
		metaViewRelations.put("metaPage", "metaViewId");
		relationMap.put("metaView", metaViewRelations);

		// 元视图菜单关系
		Map<String, String> metaViewMenuRelations = new TreeMap<>();
		metaViewMenuRelations.put("metaViewMenu", "parentId");
		relationMap.put("metaViewMenu", metaViewMenuRelations);

		// 元页面关系
		Map<String, String> metaPageRelations = new TreeMap<>();
		metaPageRelations.put("metaPageLink", "metaPageId");
		relationMap.put("metaPage", metaPageRelations);

		// 元页面组件关系
		Map<String, String> metaPageComponentRelations = new TreeMap<>();
		metaPageComponentRelations.put("metaPageLink", "metaPageComponentId");
		metaPageComponentRelations.put("metaPageComponentField", "metaPageComponentId");
		relationMap.put("metaPageComponent", metaPageComponentRelations);

		// 返回对应实体的关系配置
		return relationMap.get(domainName);
	}

	/** ----- 工具方法 ----- */
	public static Map<String, Object> _entityMap(Object entity){
		Map<String, Object> entityMap = new HashMap<>();
		if(entity instanceof GcsGroup) {
			GcsGroup gcsGroup = (GcsGroup)entity;
			entityMap = GcsGroupController._entityMap(gcsGroup);
		}else if(entity instanceof GcsUser) {
			GcsUser gcsUser = (GcsUser)entity;
			entityMap = GcsUserController._entityMap(gcsUser);
		}else if(entity instanceof ScConfig){
			ScConfig scConfig = (ScConfig)entity;
			entityMap = ScConfigController._entityMap(scConfig);
		}else if(entity instanceof ScElement){
			ScElement scElement = (ScElement) entity;
			entityMap = ScElementController._entityMap(scElement);
		}else if(entity instanceof MetaDomain){
			MetaDomain metaDomain = (MetaDomain)entity;
			entityMap = MetaDomainController._entityMap(metaDomain);
		}else if(entity instanceof MetaDomainField){
			MetaDomainField metaDomainField = (MetaDomainField)entity;
			entityMap = MetaDomainFieldController._entityMap(metaDomainField);
		}else if(entity instanceof MetaPermission){
			MetaPermission metaPermission = (MetaPermission)entity;
			entityMap = BaseController._entityMap(metaPermission);
		}else if(entity instanceof MetaView){
			MetaView metaView = (MetaView)entity;
			entityMap = MetaViewController._entityMap(metaView);
		}else if(entity instanceof MetaViewMenu){
			MetaViewMenu metaViewMenu = (MetaViewMenu)entity;
			entityMap = BaseController._entityMap(metaViewMenu);
		}else if(entity instanceof MetaPage){
			MetaPage metaPage = (MetaPage)entity;
			entityMap = BaseController._entityMap(metaPage);
		}else if(entity instanceof MetaPageLink){
			MetaPageLink metaPageLink = (MetaPageLink)entity;
			entityMap = BaseController._entityMap(metaPageLink);
		}else if(entity instanceof MetaPageComponent){
			MetaPageComponent metaPageComponent = (MetaPageComponent)entity;
			entityMap = BaseController._entityMap(metaPageComponent);
		}else if(entity instanceof MetaPageComponentField){
			MetaPageComponentField metaPageComponentField = (MetaPageComponentField)entity;
			entityMap = BaseController._entityMap(metaPageComponentField);
		}
		return entityMap;
	}


}
