package gcs.app.vm.controller;

import cn.hutool.core.convert.Convert;
import cn.hutool.core.date.DatePattern;
import cn.hutool.core.date.DateUtil;
import cn.hutool.core.io.IoUtil;
import cn.hutool.core.map.MapUtil;
import cn.hutool.core.util.ObjectUtil;
import cn.hutool.core.util.RandomUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.json.JSONUtil;
import com.alibaba.excel.EasyExcel;
import com.alibaba.excel.ExcelWriter;
import com.alibaba.excel.metadata.data.WriteCellData;
import com.alibaba.excel.write.metadata.WriteSheet;
import com.alibaba.excel.write.metadata.style.WriteCellStyle;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.UpdateWrapper;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.toolkit.Db;
import com.baomidou.mybatisplus.extension.toolkit.SqlHelper;
import gcs.app.ugrp.beans.UgrpGroup;
import gcs.app.ugrp.beans.UgrpUser;
import gcs.app.ugrp.domain.GcsUserLink;
import gcs.app.ugrp.service.GcsRasaService;
import gcs.app.ugrp.service.GcsShiroService;
import gcs.app.ugrp.service.GcsUgrpService;
import gcs.app.ugrp.service.GcsUserLinkService;
import gcs.app.vm.bean.VmClass;
import gcs.app.vm.bean.VmClassField;
import gcs.app.vm.component.fields.*;
import gcs.app.vm.constants.VmConstants;
import gcs.app.vm.service.VmClassService;
import gcs.app.vm.service.VmDomainService;
import gcs.app.vm.service.VmFieldChangeService;
import gcs.app.vm.utils.VmUtils;
import gcs.base.BaseDomain;
import gcs.base.YmlConfig;

import gcs.base.core.GcsCoreService;
import gcs.base.core.bean.GcsMap;
import gcs.base.core.bean.GcsResult;
import gcs.base.core.condition.GcsCondition;
import gcs.base.core.condition.GcsRule;
import gcs.base.core.exception.GcsException;
import gcs.base.shiro.spi.SpiUser;
import gcs.base.tools.EasyExcelService;
import gcs.base.tools.GcsBindingUtils;
import gcs.base.tools.easyexcel.GcsProgressInfo;
import gcs.modules.activity.service.GcsActivityService;
import gcs.modules.config.domain.ScElement;
import gcs.modules.config.service.ScElementService;
import gcs.modules.others.domain.AddrArea;
import gcs.modules.others.domain.AddrCity;
import gcs.modules.others.domain.AddrProvince;
import org.apache.commons.codec.digest.DigestUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.BeanWrapper;
import org.springframework.beans.BeanWrapperImpl;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.messaging.simp.SimpMessageSendingOperations;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.multipart.MultipartHttpServletRequest;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.*;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.concurrent.Executor;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/{domainName}")
public class VmDomainController{
	private Logger logger = LoggerFactory.getLogger(this.getClass());

	@Autowired
    @Qualifier("gcsExecutor")
	private Executor gcsExecutor;

	@Autowired
	private YmlConfig ymlConfig;

	@Autowired
	private ScElementService scElementService;

	@Autowired
	private VmClassService vmClassService;

	@Autowired
	private VmDomainService vmDomainService;

	@Autowired
	private GcsRasaService gcsRasaService;

	@Autowired
    private GcsShiroService gcsShiroService;

	@Autowired
	private GcsUgrpService gcsUgrpService;

	@Autowired
	private GcsUserLinkService gcsUserLinkService;

	@Autowired
	private GcsCoreService gcsCoreService;

	@Autowired
	private GcsActivityService gcsActivityService;

	@Autowired
	private VmFieldChangeService vmFieldChangeService;

	@Autowired
	private SimpMessageSendingOperations brokerMessagingTemplate;

	@RequestMapping(value = "/index", method = {RequestMethod.GET, RequestMethod.POST})
	public GcsResult index(@PathVariable("domainName") String domainName, @RequestBody(required = false) Optional<GcsCondition> payload, HttpServletRequest request) throws Exception{
		// SecurityUtils.getSubject().checkPermissions("domainName:index");
		GcsCondition gcsCondition = payload.orElse(new GcsCondition());
		VmClass vmClass = vmClassService.resolveVmClass(domainName);
		gcsCondition.setVmClass(vmClass);
		BaseMapper vmMapper = vmClass.getDomainMapper();
		IPage<? extends BaseDomain> page = vmMapper.selectPage(
			gcsCondition.buildQueryPage(),
			gcsRasaService.query(
				gcsCondition.buildQueryWrapper(vmClass.getDomainClass()),
				vmClass.getDomainName(), MapUtil.of("vmClass", vmClass)
			)
		);
		page.convert(vmClass::vmDomainEntityMap);
		if (gcsCondition.getEnrichFields() != null && !gcsCondition.getEnrichFields().isEmpty()) {
			vmDomainService.enrichRelations(
				(List<Map<String, Object>>) (List<?>) page.getRecords(),
				vmClass,
				gcsCondition.getEnrichFields()
			);
		}
		return GcsResult.successResult().wrapPage(page);
	}

	@GetMapping("/show/{id}")
	public GcsResult show(@PathVariable("domainName") String domainName, @PathVariable("id") Long id){
		// SecurityUtils.getSubject().checkPermissions("domainName:show");
		VmClass vmClass = vmClassService.resolveVmClass(domainName);
		BaseDomain entity = (BaseDomain)vmClass.getDomainMapper().selectById(id);
		if(entity == null){
			return GcsResult.notExistResult().put("id", id);
		}
		return GcsResult.successResult().putAll(vmClass.vmDomainEntityMap(entity));
	}

	@PostMapping("/save")
	public GcsResult save1(@PathVariable("domainName") String domainName, @RequestBody Map<String, Object> jsonMap){
		// SecurityUtils.getSubject().checkPermissions("domainName:save");
		VmClass vmClass = vmClassService.resolveVmClass(domainName);
		BaseDomain entity = vmClass.bindVmDomain(vmClass.newVmDomain(), jsonMap);
		vmClass.validateVmDomain(entity);
		vmClass.saveVmDomain(entity);
		vmFieldChangeService.recordCreate(vmClass, entity);
		return GcsResult.successResult().putAll(vmClass.vmDomainEntityMap(entity));
	}

	@PutMapping("/update/{id}")
	public GcsResult update(@PathVariable("domainName") String domainName, @PathVariable("id") Long id, @RequestBody Map<String, Object> jsonMap){
		// SecurityUtils.getSubject().checkPermissions("domainName:update");
		VmClass vmClass = vmClassService.resolveVmClass(domainName);
		BaseDomain entity = (BaseDomain)vmClass.getDomainMapper().selectById(id);
		if(entity == null){
			return GcsResult.notExistResult().put("id", id);
		}
		Map<String, String> oldFieldMap = vmFieldChangeService.formatFieldMap(vmClass, entity);
		entity = vmClass.bindVmDomain(entity, jsonMap);
		vmClass.validateVmDomain(entity);
		vmClass.updateVmDomain(entity);
		vmFieldChangeService.recordUpdate(vmClass, oldFieldMap, entity);
		return GcsResult.successResult().putAll(vmClass.vmDomainEntityMap(entity));
	}

	@PostMapping("/batch")
	public GcsResult batch(@PathVariable("domainName") String domainName, @RequestBody GcsMap jsonMap) {
		// SecurityUtils.getSubject().checkPermissions("domainName:batch");
		VmClass vmClass = vmClassService.resolveVmClass(domainName);
		BeanWrapper beanWrapper = new BeanWrapperImpl(vmClass.newVmDomain());
		BaseMapper baseMapper = vmClass.getDomainMapper();

		// 准备相关条件
		Boolean batchDelete = jsonMap.booleanValue("batchDelete", false);
		Map<String, VmClassField> vmClassFieldMap = vmClass.getClassFieldMap();
		UpdateWrapper updateWrapper = new UpdateWrapper<>();
		List<String> fieldNames = jsonMap.list("fieldNames")
				.stream().map(Convert::toStr).collect(Collectors.toList());
		for(String fieldName: fieldNames){
			VmClassField vmClassField = vmClassFieldMap.get(fieldName);
			if(fieldName.endsWith(VmClassField.ID_SUFFIX)){
				updateWrapper.set(VmUtils.toLowerUnderscore(fieldName), jsonMap.getLong(fieldName));
			}else if(vmClassField!=null){
				if(!vmClassField.supportsBatchEdit()){
					throw new GcsException("字段[" + vmClassField.getFieldTitle() + "]不支持批量编辑");
				}
				if(VmConstants.FIELD_TYPE_STRING.equals(vmClassField.getFieldType())) {
					updateWrapper.set(VmUtils.toLowerUnderscore(vmClassField.getFieldName()), jsonMap.getString(vmClassField.getFieldName()));
				}else if(VmConstants.FIELD_TYPE_SELECT.equals(vmClassField.getFieldType())){
					updateWrapper.set(VmUtils.toLowerUnderscore(vmClassField.getFieldName()), jsonMap.getInt(vmClassField.getFieldName()));
				}else if(VmConstants.FIELD_TYPE_SC_EL.equals(vmClassField.getFieldType())){
					ScElClassField.ScElFieldExtend fieldExtend = (ScElClassField.ScElFieldExtend)vmClassField.getFieldExtend();
					Integer depth = fieldExtend.getRealDepth();
					List identList = Convert.toList(jsonMap.get(vmClassField.getFieldName()));
					if(ObjectUtil.isNotEmpty(identList)){
						for (int i = 0; i < depth; i++) {
							String subFieldName = vmClassField.getFieldName() + ScElClassField.DEPTH_SUFFIX[i];
							if (beanWrapper.isWritableProperty(subFieldName) && identList.size()>i) {
								updateWrapper.set(VmUtils.toLowerUnderscore(subFieldName), identList.get(i));
							}
						}
					}
				}else if(VmConstants.FIELD_TYPE_INTEGER.equals(vmClassField.getFieldType())){
					updateWrapper.set(VmUtils.toLowerUnderscore(vmClassField.getFieldName()), jsonMap.getInt(vmClassField.getFieldName()));
				}else if(VmConstants.FIELD_TYPE_LONG.equals(vmClassField.getFieldType())){
					updateWrapper.set(VmUtils.toLowerUnderscore(vmClassField.getFieldName()), jsonMap.getLong(vmClassField.getFieldName()));
				}else if(VmConstants.FIELD_TYPE_BIG_DECIMAL.equals(vmClassField.getFieldType())){
					updateWrapper.set(VmUtils.toLowerUnderscore(vmClassField.getFieldName()), jsonMap.getDouble(vmClassField.getFieldName()));
				}else if(VmConstants.FIELD_TYPE_DATE.equals(vmClassField.getFieldType())){
					updateWrapper.set(VmUtils.toLowerUnderscore(vmClassField.getFieldName()), jsonMap.date(vmClassField.getFieldName()));
				}else if(VmConstants.FIELD_TYPE_USER.equals(vmClassField.getFieldType())){
					updateWrapper.set(VmUtils.toLowerUnderscore(vmClassField.getRealFieldName()), jsonMap.longValue(vmClassField.getFieldName()));
				}else if(VmConstants.FIELD_TYPE_GROUP.equals(vmClassField.getFieldType())){
					updateWrapper.set(VmUtils.toLowerUnderscore(vmClassField.getRealFieldName()), jsonMap.longValue(vmClassField.getFieldName()));
				}else if(VmConstants.FIELD_TYPE_ADDR.equals(vmClassField.getFieldType())){
					List<Long> addrList = jsonMap.list(vmClassField.getFieldName()).stream().map(Convert::toLong)
							.filter(Objects::nonNull).collect(Collectors.toList());
					String provinceColumn = VmUtils.toLowerUnderscore(vmClassField.getFieldName() + AddrClassField.PROVINCE_ID_SUFFIX);
					String cityColumn = VmUtils.toLowerUnderscore(vmClassField.getFieldName() + AddrClassField.CITY_ID_SUFFIX);
					String areaColumn = VmUtils.toLowerUnderscore(vmClassField.getFieldName() + AddrClassField.AREA_ID_SUFFIX);
					if(!addrList.isEmpty()){
						updateWrapper.set(provinceColumn, addrList.get(0));
						if(addrList.size()>1){
							updateWrapper.set(cityColumn, addrList.get(1));
							if(addrList.size()>2){
								updateWrapper.set(areaColumn, addrList.get(2));
							}else{
								updateWrapper.set(areaColumn, null);
							}
						}
					}else{
						updateWrapper.set(provinceColumn, null);
						updateWrapper.set(cityColumn, null);
						updateWrapper.set(areaColumn, null);
					}
				}
			}
		}
		// 执行更新或删除
		int affectAmount = 0;
		GcsMap jsonCondition = jsonMap.getMap("queryCondition");
		if(jsonCondition!=null){
			GcsCondition gcsCondition = GcsBindingUtils.deepBind(jsonCondition, GcsCondition.class);
			gcsCondition.setVmClass(vmClass);
			QueryWrapper<?> queryWrapper = gcsCondition.buildQueryWrapper(vmClass.getDomainClass(), false);
			// 读整条旧实体用于变更记录
			List<BaseDomain> oldEntities = baseMapper.selectList(queryWrapper);
			List<Long> idList = oldEntities.stream().map(BaseDomain::getId).collect(Collectors.toList());
			// 按查询条件更新或删除
			if(batchDelete){
				if (!idList.isEmpty()) {
					String referenceClass = gcsCoreService.toReferenceClass(domainName);
					gcsActivityService.deleteByReference(referenceClass, idList);
				}
				affectAmount = baseMapper.delete(queryWrapper);
				dispatchBatchDelete(vmClass, oldEntities);
			}else{
				idList.add(0L);
				updateWrapper.in("id", idList);
				affectAmount = baseMapper.update(updateWrapper);
				dispatchBatchUpdate(vmClass, oldEntities, fieldNames, jsonMap);
			}
		}else{
			// 按记录更新或删除
			List<Long> opKeys = jsonMap.list("opKeys").stream().map(Convert::toLong).collect(Collectors.toList());
			List<BaseDomain> oldEntities = opKeys.isEmpty()
					? Collections.emptyList()
					: baseMapper.selectBatchIds(opKeys);
			List<Long> opKeysList = new ArrayList<>(opKeys);
			opKeysList.add(0L);
			if(batchDelete){
				String referenceClass = gcsCoreService.toReferenceClass(domainName);
				gcsActivityService.deleteByReference(referenceClass, opKeysList);
				affectAmount = baseMapper.deleteByIds(opKeysList);
				dispatchBatchDelete(vmClass, oldEntities);
			}else{
				updateWrapper.in(BaseDomain.ID, opKeysList);
				affectAmount = baseMapper.update(updateWrapper);
				dispatchBatchUpdate(vmClass, oldEntities, fieldNames, jsonMap);
			}
		}
		return GcsResult.successResult().put("modifiedAmount", affectAmount);
	}

	@DeleteMapping("/delete/{id}")
	public GcsResult delete(@PathVariable("domainName") String domainName, @PathVariable("id") Long id, HttpServletRequest request) throws Exception{
		// SecurityUtils.getSubject().checkPermissions("domainName:delete");
		VmClass vmClass = vmClassService.resolveVmClass(domainName);
		String referenceClass = gcsCoreService.toReferenceClass(domainName);
		gcsActivityService.deleteByReference(referenceClass, id);
		BaseDomain entity = (BaseDomain) vmClass.getDomainMapper().selectById(id);
		Boolean isSuccess = SqlHelper.retBool(vmClass.getDomainMapper().deleteById(id));
		if(!isSuccess){
			return GcsResult.notExistResult().put("id", id);
		}
		if (entity != null) {
			vmFieldChangeService.recordDelete(vmClass, entity);
		}
		return GcsResult.successResult().put("id", id);
	}

	private void dispatchBatchUpdate(VmClass vmClass, List<BaseDomain> oldEntities, List<String> fieldNames, GcsMap jsonMap){
		if (oldEntities == null || oldEntities.isEmpty()) {
			return;
		}
		SpiUser spiUser = currentSpiUser();
		Long operatorId = spiUser != null ? spiUser.getId() : null;
		String operatorName = spiUser != null ? spiUser.getDisplayName() : null;
		gcsExecutor.execute(() -> vmFieldChangeService.recordBatchUpdate(vmClass, oldEntities, fieldNames, jsonMap, operatorId, operatorName));
	}

	private void dispatchBatchDelete(VmClass vmClass, List<BaseDomain> oldEntities){
		if (oldEntities == null || oldEntities.isEmpty()) {
			return;
		}
		SpiUser spiUser = currentSpiUser();
		Long operatorId = spiUser != null ? spiUser.getId() : null;
		String operatorName = spiUser != null ? spiUser.getDisplayName() : null;
		gcsExecutor.execute(() -> vmFieldChangeService.recordBatchDelete(vmClass, oldEntities, operatorId, operatorName));
	}

	private SpiUser currentSpiUser(){
		try {
			return gcsShiroService.currentUser();
		} catch (Exception e) {
			return null;
		}
	}

	@PostMapping("/downloadExcel")
	public void downloadExcel(@RequestBody GcsMap jsonMap, HttpServletResponse response ) throws IOException {
		// 检查文件是否存在
		String fileName = jsonMap.getString("fileName");
		File excelFile = new File(ymlConfig.getTempStorageRootDir(), fileName);
		if (!excelFile.exists()) {
			response.setStatus(HttpServletResponse.SC_NOT_FOUND);
			try {
				GcsResult result = GcsResult.failedResult("文件不存在");
				response.getWriter().write(result.toString());
			} catch (IOException ex) {
				// 处理写入错误
			}
		}

		// 读取文件并写入响应流
		response.setContentType("application/octet-stream");
		String encodedFileName = URLEncoder.encode(fileName, StandardCharsets.UTF_8.name());
		response.setHeader("Content-disposition", "inline; filename=\"" + encodedFileName + "\";");
		try (InputStream inputStream = new FileInputStream(excelFile)) {
			IoUtil.copy(inputStream, response.getOutputStream());
			response.flushBuffer();
		} catch (IOException e) {
			logger.error("读取文件失败: {}", fileName, e);
		}
	}

	@PostMapping("/downloadTemplate")
	public void downloadTemplate(@PathVariable("domainName") String domainName, HttpServletResponse response){
		ByteArrayOutputStream excelStream = new ByteArrayOutputStream();
		SpiUser spiUser = gcsShiroService.currentUser();
		// 可导出字段排序
		VmClass vmClass = vmClassService.resolveVmClass(domainName);
		List<VmClassField> domainFields = new ArrayList<>(vmClass.getClassFieldMap().values());
		domainFields = domainFields.stream()
				.filter(VmClassField::supportsImport)
				.sorted(Comparator.comparing(VmClassField::getSeqOrder).thenComparing(VmClassField::getId))
				.collect(Collectors.toList());
		try {
			// 创建Excel写入器
			ExcelWriter excelWriter = EasyExcel.write(excelStream).build();
			WriteSheet writeSheet = EasyExcel.writerSheet("sheet1").build();
			excelWriter.write(Collections.singletonList(Arrays.asList("说明: [粗体]为必填项，[普通]为非必填项；结果中[灰色]为无效数据，[橙色]为重复数据，[红色]为失败数据")), writeSheet);
			// 写出Excel头部
			List<WriteCellData<String>> headerDataList = new ArrayList<>();
			for (VmClassField vmClassField : domainFields) {
				WriteCellStyle headerStyle = vmClassField.getFieldConstraint().getNullable()
						?EasyExcelService.NORMAL_CELL_STYLE:EasyExcelService.HEAD_CELL_STYLE;
				if(VmConstants.FIELD_TYPE_SC_EL.equals(vmClassField.getFieldType())) {
					ScElClassField.ScElFieldExtend fieldExtend = (ScElClassField.ScElFieldExtend) vmClassField.getFieldExtend();
					Integer depth = fieldExtend.getRealDepth();
					for (int i = 0; i < depth; i++) {
						String subFieldTitle = vmClassField.getFieldTitle() + ScElClassField.DEPTH_SUFFIX[i];
						WriteCellData<String> headerCellData = new WriteCellData<>(subFieldTitle);
						headerCellData.setWriteCellStyle(headerStyle);
						headerDataList.add(headerCellData);
					}
				}else if(VmConstants.FIELD_TYPE_ADDR.equals(vmClassField.getFieldType())){
					for (String suffix : Arrays.asList("省份", "城市", "区域")) {
						WriteCellData<String> headerCellData = new WriteCellData<>(vmClassField.getFieldTitle() + suffix);
						headerCellData.setWriteCellStyle(headerStyle);
						headerDataList.add(headerCellData);
					}
				}else{
					WriteCellData<String> headerCellData = new WriteCellData<>(vmClassField.getFieldTitle());
					headerCellData.setWriteCellStyle(headerStyle);
					headerDataList.add(headerCellData);
				}
			}
			excelWriter.write(Collections.singletonList(headerDataList), writeSheet);
			// 写出Excel内容
			int cursor = 0;
			while (cursor < 3) {
				cursor++;
				List<WriteCellData<String>> contentDataList = new ArrayList<>();
				for (VmClassField vmClassField : domainFields) {
					if(VmConstants.FIELD_TYPE_SC_EL.equals(vmClassField.getFieldType())){
						ScElClassField.ScElFieldExtend fieldExtend = (ScElClassField.ScElFieldExtend)vmClassField.getFieldExtend();
						Integer depth = fieldExtend.getRealDepth();
						String scKey = fieldExtend.getScKey();
						List<ScElement> scElements = scElementService.getScElementTree(scKey);
						ScElement currentNode = null;
						for (int i = 0; i < depth; i++) {
							WriteCellData<String> contentCellData = new WriteCellData<>("");
							if (i == 0 && !scElements.isEmpty()) {
								// 第一层：随机选根节点
								currentNode = scElements.get(RandomUtil.randomInt(0, scElements.size()));
								contentCellData.setStringValue(currentNode.getTitle());
							} else if (currentNode != null && currentNode.getChildren() != null && !currentNode.getChildren().isEmpty()) {
								// 后续层级：随机选子节点
								List<ScElement> children = currentNode.getChildren();
								currentNode = children.get(RandomUtil.randomInt(0, children.size()));
								contentCellData.setStringValue(currentNode.getTitle());
							} else {
								// 无节点时填充空值
								contentCellData.setStringValue("");
								currentNode = null; // 终止后续选择
							}
							contentDataList.add(contentCellData);
						}
					}else if(VmConstants.FIELD_TYPE_ADDR.equals(vmClassField.getFieldType())){
						for (Integer num : Arrays.asList(1, 2, 3)) {
							WriteCellData<String> contentCellData = new WriteCellData<>("");
							contentCellData.setStringValue(num==1?"上海市":num==2?"浦东新区":"");
							contentDataList.add(contentCellData);
						}
					}else{
						WriteCellData<String> contentCellData = new WriteCellData<>("");
						String demoValue = "";
						if(VmConstants.FIELD_TYPE_STRING.equals(vmClassField.getFieldType())) {
							demoValue = RandomUtil.randomString(5);
						}else if(VmConstants.FIELD_TYPE_SELECT.equals(vmClassField.getFieldType())){
							SelectClassField.SelectFieldExtend fieldExtend = (SelectClassField.SelectFieldExtend)vmClassField.getFieldExtend();
							List<String> keyList = new ArrayList<>(fieldExtend.getOptionsMap().keySet());
							demoValue = keyList.get(RandomUtil.randomInt(0, keyList.size()));
						}else if(VmConstants.FIELD_TYPE_SC_EL.equals(vmClassField.getFieldType())){

						}else if(VmConstants.FIELD_TYPE_INTEGER.equals(vmClassField.getFieldType())){
							IntegerClassField.IntegerFieldConstraint fieldConstraint = (IntegerClassField.IntegerFieldConstraint)vmClassField.getFieldConstraint();
							int min = fieldConstraint.getMin()!=null?fieldConstraint.getMin():0;
							int max = fieldConstraint.getMax()!=null?fieldConstraint.getMax():100;
							demoValue = StrUtil.toString(RandomUtil.randomInt(min, max));
						}else if(VmConstants.FIELD_TYPE_LONG.equals(vmClassField.getFieldType())){
							LongClassField.LongFieldConstraint fieldConstraint = (LongClassField.LongFieldConstraint)vmClassField.getFieldConstraint();
							long min = fieldConstraint.getMin()!=null?fieldConstraint.getMin():0;
							long max = fieldConstraint.getMax()!=null?fieldConstraint.getMax():100;
							demoValue = StrUtil.toString(RandomUtil.randomLong(min, max));
						}else if(VmConstants.FIELD_TYPE_BIG_DECIMAL.equals(vmClassField.getFieldType())){
							BigDecimalClassField.BigDecimalFieldConstraint fieldConstraint = (BigDecimalClassField.BigDecimalFieldConstraint)vmClassField.getFieldConstraint();
							BigDecimal min = fieldConstraint.getMin()!=null?fieldConstraint.getMin():BigDecimal.valueOf(0);
							BigDecimal max = fieldConstraint.getMax()!=null?fieldConstraint.getMax():BigDecimal.valueOf(100);
							demoValue = StrUtil.toString(RandomUtil.randomBigDecimal(min, max).setScale(2, RoundingMode.HALF_UP));
						}else if(VmConstants.FIELD_TYPE_DATE.equals(vmClassField.getFieldType())){
							DateClassField dateClassField = (DateClassField)vmClassField;
							demoValue = DateUtil.format(new Date(), dateClassField.getJavaDateFormat());
						}else if(VmConstants.FIELD_TYPE_USER.equals(vmClassField.getFieldType())){
							demoValue = spiUser.getDisplayName();
						}else if(VmConstants.FIELD_TYPE_GROUP.equals(vmClassField.getFieldType())){
							UgrpGroup ugrpGroup = gcsUgrpService.getUgrpGroup(spiUser.getGroupId());
							demoValue = ugrpGroup.getId()!=0L?ugrpGroup.getName():"";
						}else if(VmConstants.FIELD_TYPE_LINK_USER.equals(vmClassField.getFieldType())){
							demoValue = String.join("|", Arrays.asList(spiUser.getDisplayName()));
						}
						contentCellData.setStringValue(demoValue);
						contentDataList.add(contentCellData);
					}
				}
				excelWriter.write(Collections.singletonList(contentDataList), writeSheet);
			}
			// 响应Excel到流
			excelWriter.finish();
			EasyExcelService.writeToResponse(response, excelStream);
		} catch (Exception e) {
			e.printStackTrace();
			throw new RuntimeException("下载模板失败", e);
		}
	}

	@PostMapping("/importExcel")
	public GcsResult importExcel(@PathVariable("domainName") String domainName, HttpServletRequest request){
		VmClass vmClass = vmClassService.resolveVmClass(domainName);
		try {
			// 获取上传文件
			MultipartFile multipartFile;
			if (request instanceof MultipartHttpServletRequest) {
				multipartFile = ((MultipartHttpServletRequest) request).getFile("uploadFile");
			} else {
                multipartFile = null;
            }
            String identifier = request.getParameter("importIdentifier");
			identifier=identifier==null?UUID.randomUUID().toString():identifier;

			// 校验文件表头
			GcsResult validateResult = vmDomainService.validateFile(vmClass, multipartFile);
			if (!validateResult.isSuccess()) { return validateResult; }

			// 异步导入数据
			File excelFile = (File) validateResult.getGmodel();
			String finalIdentifier = identifier;
			gcsExecutor.execute(() ->
					vmDomainService.asyncImport(vmClass, multipartFile, excelFile, finalIdentifier)
			);
		} catch (Exception e) {
			e.printStackTrace();
		}
		return GcsResult.successResult();
	}

	@PostMapping("/exportExcel")
	public void exportExcel(@PathVariable("domainName") String domainName, @RequestBody GcsCondition gcsCondition, HttpServletResponse response){
		SpiUser spiUser = gcsShiroService.currentUser();
		String conditionMd5Str = DigestUtils.md5Hex(JSONUtil.toJsonStr(gcsCondition));
		String conditionIdentifier = DateUtil.format(DateUtil.date(), DatePattern.PURE_DATE_FORMAT) + "_" + conditionMd5Str.substring(0, 15);
		String exportIdentifier = gcsCondition.getRules().stream()
				.filter(rule -> "_extend_identifier".equals(rule.getField()))
				.findFirst().map(rule -> Convert.toStr(rule.getValue())).orElse(null);;
		ByteArrayOutputStream excelStream = new ByteArrayOutputStream();

		// 可导出字段排序
		VmClass vmClass = vmClassService.resolveVmClass(domainName);
		List<VmClassField> domainFields = new ArrayList<>(vmClass.getClassFieldMap().values());
		Set<String> excludedTypes = new HashSet<>();
		excludedTypes.add(VmConstants.FIELD_TYPE_ONE2MANY);
		excludedTypes.add(VmConstants.FIELD_TYPE_MANY2ONE);
		domainFields = domainFields.stream()
				.filter(field -> !excludedTypes.contains(field.getFieldType()))
				.sorted(Comparator.comparing(VmClassField::getSeqOrder).thenComparing(VmClassField::getId))
				.collect(Collectors.toList());

		QueryWrapper<?> queryWrapper = gcsCondition.buildQueryWrapper(vmClass.getDomainClass()).orderByDesc(BaseDomain.ID);
		try {
			// 创建Excel写入器
			ExcelWriter excelWriter = EasyExcel.write(excelStream).build();
			WriteSheet writeSheet = EasyExcel.writerSheet("sheet1").build();

			// 写出Excel头部
			List<WriteCellData<String>> headerDataList = new ArrayList<>();
			WriteCellData<String> headerIdCellData = new WriteCellData<>(BaseDomain.ID.toUpperCase());
			headerIdCellData.setWriteCellStyle(EasyExcelService.HEAD_CELL_STYLE);
			headerDataList.add(headerIdCellData);
			for (VmClassField vmClassField : domainFields) {
				WriteCellStyle headerStyle = vmClassField.getFieldConstraint().getNullable()
						?EasyExcelService.NORMAL_CELL_STYLE:EasyExcelService.HEAD_CELL_STYLE;
				if(VmConstants.FIELD_TYPE_SC_EL.equals(vmClassField.getFieldType())){
					ScElClassField.ScElFieldExtend fieldExtend = (ScElClassField.ScElFieldExtend)vmClassField.getFieldExtend();
					Integer depth = fieldExtend.getRealDepth();
					for (int i = 0; i < depth; i++) {
						String subFieldTitle = vmClassField.getFieldTitle() + ScElClassField.DEPTH_SUFFIX[i];
						WriteCellData<String> headerCellData = new WriteCellData<>(subFieldTitle);
						headerCellData.setWriteCellStyle(headerStyle);
						headerDataList.add(headerCellData);
					}
				}else if(VmConstants.FIELD_TYPE_ADDR.equals(vmClassField.getFieldType())){
					for (String suffix : Arrays.asList("省份", "城市", "区域")) {
						WriteCellData<String> headerCellData = new WriteCellData<>(vmClassField.getFieldTitle() + suffix);
						headerCellData.setWriteCellStyle(headerStyle);
						headerDataList.add(headerCellData);
					}
				}else{
					WriteCellData<String> headerCellData = new WriteCellData<>(vmClassField.getFieldTitle());
					headerCellData.setWriteCellStyle(headerStyle);
					headerDataList.add(headerCellData);
				}
			}
			excelWriter.write(Collections.singletonList(headerDataList), writeSheet);

			// 准备首页数据
			int STEP_SIZE = 100;
			Page page = new Page<>(0, STEP_SIZE);
			IPage<? extends BaseDomain> pageResult = vmClass.getDomainMapper().selectPage(page, queryWrapper);

			// 进度初始化
			GcsProgressInfo progressInfo = new GcsProgressInfo();
			progressInfo.setUsername(spiUser.getUsername().toLowerCase());
			progressInfo.setIdentifier(exportIdentifier!=null?exportIdentifier.toString():conditionIdentifier);
			progressInfo.setType(GcsProgressInfo.TYPE_EXPORT);
			progressInfo.setStatus(GcsProgressInfo.STATUS_NORMAL);
			progressInfo.setCursor(0L);
			progressInfo.setTotal(pageResult.getTotal());
			progressInfo.setPercent(new BigDecimal(0));

			// 分页处理所有数据
			while (!pageResult.getRecords().isEmpty()) {
				for (BaseDomain instance : pageResult.getRecords()) {
					// 处理单条记录
					BeanWrapper beanWrapper = new BeanWrapperImpl(instance);
					List<WriteCellData<String>> contentDataList = new ArrayList<>();
					// 处理ID字段
					WriteCellData<String> idCellData = new WriteCellData<>("");
					idCellData.setStringValue(Convert.toStr(beanWrapper.getPropertyValue(BaseDomain.ID), ""));
					contentDataList.add(idCellData);
					// 处理其它字段
					for (VmClassField vmClassField : domainFields) {
						if(VmConstants.FIELD_TYPE_SC_EL.equals(vmClassField.getFieldType())){
							ScElClassField.ScElFieldExtend fieldExtend = (ScElClassField.ScElFieldExtend)vmClassField.getFieldExtend();
							Integer depth = fieldExtend.getRealDepth();
							String scKey = fieldExtend.getScKey();
							for (int i = 0; i < depth; i++) {
								String subFieldName = vmClassField.getFieldName() + ScElClassField.DEPTH_SUFFIX[i];
								Object propValue = beanWrapper.getPropertyValue(subFieldName);
								ScElement scElement = scElementService.getScElement(scKey, Convert.toLong(propValue));
								WriteCellData<String> contentCellData = new WriteCellData<>("");
								contentCellData.setStringValue(scElement!=null?scElement.getTitle():Convert.toStr(propValue));
								contentDataList.add(contentCellData);
							}
						}else if(VmConstants.FIELD_TYPE_ADDR.equals(vmClassField.getFieldType())){
							for (String suffix : Arrays.asList(AddrClassField.PROVINCE_ID_SUFFIX, AddrClassField.CITY_ID_SUFFIX, AddrClassField.AREA_ID_SUFFIX)) {
								WriteCellData<String> contentCellData = new WriteCellData<>("");
								Object propValue = beanWrapper.getPropertyValue(vmClassField.getFieldName() + suffix);
								if(AddrClassField.PROVINCE_ID_SUFFIX.equals(suffix)){
									AddrProvince addrProvince = Db.getById(Convert.toLong(propValue), AddrProvince.class);
									contentCellData.setStringValue(addrProvince!=null?addrProvince.getName():"");
								}else if(AddrClassField.CITY_ID_SUFFIX.equals(suffix)){
									AddrCity addrCity = Db.getById(Convert.toLong(propValue), AddrCity.class);
									contentCellData.setStringValue(addrCity!=null?addrCity.getName():"");
								} else {
									AddrArea addrArea = Db.getById(Convert.toLong(propValue), AddrArea.class);
									contentCellData.setStringValue(addrArea!=null?addrArea.getName():"");
								}
								contentDataList.add(contentCellData);
							}
						}else{
							WriteCellData<String> contentCellData = new WriteCellData<>("");
							String propValueStr = "";
							if(VmConstants.FIELD_TYPE_SELECT.equals(vmClassField.getFieldType())) {
								Object propValue = beanWrapper.getPropertyValue(vmClassField.getFieldName());
								SelectClassField.SelectFieldExtend fieldExtend = (SelectClassField.SelectFieldExtend) vmClassField.getFieldExtend();
								Optional<Map.Entry<String, Integer>> firstEntry = fieldExtend.getOptionsMap().entrySet().stream()
										.filter(entry -> entry.getValue() != null && entry.getValue().equals(propValue)).findFirst();
								propValueStr = firstEntry.map(Map.Entry::getKey).orElse("");
							} else if(VmConstants.FIELD_TYPE_DATE.equals(vmClassField.getFieldType())) {
								DateClassField dateClassField = (DateClassField)vmClassField;
								Object propValue = beanWrapper.getPropertyValue(vmClassField.getFieldName());
								propValueStr = propValue!=null?DateUtil.format((Date) propValue, dateClassField.getJavaDateFormat()):"";
							} else if(VmConstants.FIELD_TYPE_USER.equals(vmClassField.getFieldType())) {
								Object propValue = beanWrapper.getPropertyValue(vmClassField.getRealFieldName());
								if(propValue!=null) {
									UgrpUser ugrpUser = gcsUgrpService.getUgrpUser(Convert.toLong(propValue));
									propValueStr = ugrpUser.getId()!=0L?ugrpUser.getNickname():"";
								}
							} else if(VmConstants.FIELD_TYPE_GROUP.equals(vmClassField.getFieldType())) {
								Object propValue = beanWrapper.getPropertyValue(vmClassField.getRealFieldName());
								if(propValue!=null) {
									UgrpGroup ugrpGroup = gcsUgrpService.getUgrpGroup(Convert.toLong(propValue));
									propValueStr = ugrpGroup.getId()!=0L?ugrpGroup.getName():"";
								}
							} else if(VmConstants.FIELD_TYPE_LINK_USER.equals(vmClassField.getFieldType())) {
								Long domainId = Convert.toLong(beanWrapper.getPropertyValue(BaseDomain.ID));
								List<GcsUserLink> userLinks = gcsUserLinkService.listRelUserLinks(vmClass.getDomainName(), domainId, vmClassField.getFieldName());
								List<String> usernameList = userLinks.stream().map(GcsUserLink::getNickname).collect(Collectors.toList());
								propValueStr = String.join("|", usernameList);
							} else if(beanWrapper.isReadableProperty(vmClassField.getFieldName())){
								Object propValue = beanWrapper.getPropertyValue(vmClassField.getFieldName());
								propValueStr = Convert.toStr(propValue, "");
							}
							contentCellData.setStringValue(propValueStr);
							contentDataList.add(contentCellData);
						}
					}
					// 写入一行数据
					excelWriter.write(Collections.singletonList(contentDataList), writeSheet);
				}

				// 进度更新
				progressInfo.setStatus(GcsProgressInfo.STATUS_ACTIVE);
				progressInfo.setCursor(progressInfo.getCursor() + pageResult.getRecords().size());
				progressInfo.setPercent(
						BigDecimal.valueOf(progressInfo.getCursor())
								.divide(BigDecimal.valueOf(progressInfo.getTotal()), 4, RoundingMode.HALF_UP)
								.multiply(new BigDecimal(100)).setScale(2, RoundingMode.HALF_UP)
				);
				brokerMessagingTemplate.convertAndSend("/topic/tenant_" + vmClass.getDomainName(), progressInfo);

				// 准备下一页数据
				page.setCurrent(page.getCurrent() + 1);
				pageResult = vmClass.getDomainMapper().selectPage(page, queryWrapper);
			}

			// 进度完成
			progressInfo.setStatus(GcsProgressInfo.STATUS_SUCCESS);
			progressInfo.setCursor(progressInfo.getTotal());
			progressInfo.setPercent(new BigDecimal(100));
			brokerMessagingTemplate.convertAndSend("/topic/tenant_" + vmClass.getDomainName(), progressInfo);

			// 响应Excel到流
			excelWriter.finish();
			EasyExcelService.writeToResponse(response, excelStream);
		} catch (Exception e) {
			e.printStackTrace();
			throw new RuntimeException("导出Excel失败", e);
		}
	}
}
