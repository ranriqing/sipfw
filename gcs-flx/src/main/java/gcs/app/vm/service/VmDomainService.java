package gcs.app.vm.service;

import cn.hutool.core.convert.Convert;
import cn.hutool.core.lang.Assert;
import cn.hutool.core.lang.Dict;
import cn.hutool.json.JSONUtil;
import com.alibaba.excel.EasyExcel;
import com.alibaba.excel.ExcelReader;
import com.alibaba.excel.ExcelWriter;
import com.alibaba.excel.context.AnalysisContext;
import com.alibaba.excel.event.AnalysisEventListener;
import com.alibaba.excel.exception.ExcelCommonException;
import com.alibaba.excel.metadata.data.WriteCellData;
import com.alibaba.excel.read.metadata.ReadSheet;
import com.alibaba.excel.read.metadata.holder.ReadRowHolder;
import com.alibaba.excel.read.metadata.holder.ReadSheetHolder;
import com.alibaba.excel.util.ListUtils;
import com.alibaba.excel.write.metadata.WriteSheet;
import com.alibaba.excel.write.metadata.style.WriteCellStyle;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.toolkit.Db;
import gcs.app.ugrp.domain.GcsGroup;
import gcs.app.ugrp.domain.GcsUser;
import gcs.app.ugrp.domain.WechatUser;
import gcs.app.ugrp.service.GcsShiroService;
import gcs.app.ugrp.service.GcsUgrpService;
import gcs.app.vm.bean.VmClass;
import gcs.app.vm.bean.VmClassField;
import gcs.app.vm.component.fields.*;
import gcs.app.vm.constants.VmConstants;
import gcs.app.vm.utils.VmUtils;
import gcs.base.BaseDomain;
import gcs.base.YmlConfig;
import gcs.base.core.GcsConstants;
import gcs.base.core.GcsHolder;
import gcs.base.core.bean.GcsResult;
import gcs.base.shiro.spi.SpiUser;
import gcs.base.tools.EasyExcelService;
import gcs.base.tools.GcsNumberUtils;
import gcs.base.tools.easyexcel.GcsHorizontalCellStyleStrategy;
import gcs.base.tools.easyexcel.GcsProgressInfo;
import gcs.modules.config.domain.ScElement;
import gcs.modules.config.service.ScElementService;
import gcs.modules.others.domain.AddrArea;
import gcs.modules.others.domain.AddrCity;
import gcs.modules.others.domain.AddrProvince;
import org.apache.commons.io.FilenameUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.tuple.MutableTriple;
import org.apache.poi.openxml4j.exceptions.NotOfficeXmlFileException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.messaging.simp.SimpMessageSendingOperations;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.IOException;
import java.math.BigDecimal;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

@Service
public class VmDomainService {
    protected Logger logger = LoggerFactory.getLogger(this.getClass());

    public static String AMOUNT_TOTAL = "total";
    public static String AMOUNT_INVALID = "invalid";
    public static String AMOUNT_SUCCESS = "success";

    @Autowired
    private YmlConfig ymlConfig;

    @Autowired
    private GcsShiroService gcsShiroService;

    @Autowired
    private ScElementService scElementService;

    @Autowired
    private SimpMessageSendingOperations brokerMessagingTemplate;

    @Autowired
    private VmClassService vmClassService;

    /**
     * 验证Excel文件
     * @param multipartFile
     * @return
     */
    public GcsResult validateFile(VmClass vmClass, MultipartFile multipartFile){
        Assert.notNull(vmClass, "vmClass must not be null");
        GcsResult gcsResult;
        // 参数校验
        if (multipartFile == null || multipartFile.isEmpty()) {
            return GcsResult.failedResult("没有找到要导入的文件");
        }
        if (multipartFile.getSize() > 1024 * 1024 * 10) {
            return GcsResult.failedResult("要导入的文件大小不能超过10M");
        }
        // 保存Excel文件
        String excelDir = ymlConfig.getTempStorageRootDir();
        String fileName = new SimpleDateFormat("yyyyMMdd_HHmmss_S").format(new Date());
        String fileSuffix = FilenameUtils.getExtension(multipartFile.getOriginalFilename());
        File excelFile = new File(excelDir, fileName + "." + fileSuffix);
        try {
            multipartFile.transferTo(excelFile);
        } catch (IOException e) {
            logger.error("文件保存失败", e);
            return GcsResult.failedResult("文件保存失败");
        }

        // 获取导入字段
        List<VmClassField> domainFields = new ArrayList<>(vmClass.getClassFieldMap().values());
        domainFields.removeIf(field ->
            Arrays.asList(
                VmConstants.FIELD_TYPE_ONE2MANY,
                VmConstants.FIELD_TYPE_ADDR,
                VmConstants.FIELD_TYPE_LINK_USER
            ).contains(field.getFieldType())
        );
        domainFields.sort(Comparator.comparing(VmClassField::getSeqOrder));

        // Excel文件校验
        List<VmClassField> requiredFields = domainFields.stream().filter(item -> !item.getFieldConstraint().getNullable()).collect(Collectors.toList());
        List<String> requiredTitles = requiredFields.stream().map(VmClassField::getFieldTitle).collect(Collectors.toList());
        try {
            List<String> missedFieldTitles = new ArrayList<>();
            int headRowNumber = 5;
            boolean[] headRowMark = {false};
            List<String> excelHeaders = new ArrayList<>();
            EasyExcel.read(excelFile, new AnalysisEventListener<Map<Integer, String>>() {
                @Override
                public void invokeHeadMap(Map<Integer, String> headMap, AnalysisContext context) {
                    if (!headRowMark[0]) {
                        List<String> rowHeadValues = new ArrayList<>(headMap.values());
                        boolean isHeadRow = rowHeadValues.stream()
                                .anyMatch(requiredTitles::contains);

                        if (isHeadRow) {
                            headRowMark[0] = true;
                            excelHeaders.addAll(rowHeadValues);
                        }
                    }
                }

                @Override
                public void invoke(Map<Integer, String> dataMap, AnalysisContext context) {
                    // do nothing
                }

                @Override
                public void doAfterAllAnalysed(AnalysisContext context) {
                    // do nothing
                }

                @Override
                public void onException(Exception exception, AnalysisContext context) {
                    logger.warn("row exception: {}", exception.getMessage());
                }
            }).sheet().headRowNumber(headRowNumber).doRead();

            // 检查必填字段
            for (VmClassField field : requiredFields) {
                if (!excelHeaders.contains(field.getFieldTitle())) {
                    missedFieldTitles.add(field.getFieldTitle());
                }
            }

            if (!missedFieldTitles.isEmpty()) {
                return GcsResult.failedResult("字段" + missedFieldTitles + "必须存在");
            } else {
                return GcsResult.successResult().singleData(excelFile);
            }

        } catch (ExcelCommonException e) {
            logger.warn("Excel格式错误", e);
            gcsResult = GcsResult.failedResult("Excel文件类型有误");
        } catch (NotOfficeXmlFileException e) {
            logger.warn("非Office文件", e);
            gcsResult = GcsResult.failedResult("Excel文件格式错误");
        } catch (Exception e) {
            logger.warn("文件读取错误", e);
            gcsResult = GcsResult.failedResult("Excel文件读取错误");
        }
        return gcsResult;
    }

    /**
     * 异步导入数据
     */
    public void asyncImport(VmClass vmClass, MultipartFile multipartFile, File excelFile, String identifier) {
        Objects.requireNonNull(vmClass);
        Objects.requireNonNull(multipartFile);
        Objects.requireNonNull(excelFile);

        SpiUser actorUser = gcsShiroService.currentUser();
        Map<String, Long> amountMap = new HashMap<>();
        amountMap.put(AMOUNT_TOTAL, 0L);
        amountMap.put(AMOUNT_SUCCESS, 0L);
        amountMap.put(AMOUNT_INVALID, 0L);

        GcsProgressInfo progressInfo = new GcsProgressInfo();
        progressInfo.setUsername(actorUser.getUsername().toLowerCase());
        progressInfo.setIdentifier(identifier);
        progressInfo.setType(GcsProgressInfo.TYPE_IMPORT);
        progressInfo.setStatus(GcsProgressInfo.STATUS_NORMAL);
        progressInfo.setCursor(0L);
        progressInfo.setTotal(0L);
        progressInfo.setPercent(new BigDecimal(0));
        // progressInfo.setExtendMap(new HashMap<>(amountMap));

        File excelFileMarked = null;
        try {
            // 进度回调
            StepCallback stepCallback = (stepDataList, sheetTotalRow) -> {
                if (stepDataList != null && !stepDataList.isEmpty()) {
                    MutableTriple<Integer, Map<Integer, String>, Map<String, Object>> last =
                            stepDataList.get(stepDataList.size() - 1);
                    int rowIndex = last.getLeft();
                    progressInfo.setStatus(GcsProgressInfo.STATUS_ACTIVE);
                    progressInfo.setCursor(Convert.toLong(rowIndex));
                    progressInfo.setTotal(Convert.toLong(sheetTotalRow));
                    progressInfo.setPercent(GcsNumberUtils.percentage(rowIndex, sheetTotalRow, 3).multiply(new BigDecimal(100)));
                    // progressInfo.setExtendMap(new HashMap<>(amountMap));
                    brokerMessagingTemplate.convertAndSend("/topic/tenant_" + vmClass.getDomainName(), progressInfo);
                }
            };

            // 移动标记文件到上传目录
            File tmpMarkedFile = doImportInPromise(vmClass, excelFile, actorUser, amountMap, stepCallback);
            String markedFileName = FilenameUtils.getBaseName(excelFile.getName()) + "_marked.xlsx";
            excelFileMarked = new File(excelFile.getParentFile(), markedFileName);
            Files.move(Paths.get(tmpMarkedFile.getAbsolutePath()), Paths.get(excelFileMarked.getAbsolutePath()));

        } catch (Exception e) {
            logger.error("数据导入失败", e);
        } finally {
            // 发送完成通知
            progressInfo.setStatus(GcsProgressInfo.STATUS_SUCCESS);
            progressInfo.setCursor(amountMap.get(AMOUNT_TOTAL));
            progressInfo.setTotal(amountMap.get(AMOUNT_TOTAL));
            progressInfo.setPercent(new BigDecimal(100));
            progressInfo.setExtendMap(Dict.of(
                    "finished", true,
                   "totalAmount", amountMap.get(AMOUNT_TOTAL),
                   "successAmount", amountMap.get(AMOUNT_SUCCESS),
                   "invalidAmount", amountMap.get(AMOUNT_INVALID),
                   "failedAmount",amountMap.get(AMOUNT_TOTAL) - amountMap.get(AMOUNT_INVALID) - amountMap.get(AMOUNT_SUCCESS),
                   "localFilename", excelFileMarked != null ? excelFileMarked.getName() : null
            ));
            brokerMessagingTemplate.convertAndSend("/topic/tenant_" + vmClass.getDomainName(), progressInfo);
        }
    }

    /**
     * 执行数据导入的核心方法
     */
    private File doImportInPromise(VmClass vmClass, File excelFile,
                                   SpiUser actorUser, Map<String, Long> amountMap,
                                   StepCallback stepCallback) throws IOException {
        File markedFile = File.createTempFile(Long.toString(System.currentTimeMillis()), ".xlsx");

        /** 获取字段相关信息 */
        // 获取实体字段
        List<VmClassField> domainFields = new ArrayList<>(vmClass.getClassFieldMap().values());
        domainFields.removeIf(field -> !field.supportsImport() );
        domainFields.sort(Comparator.comparing(VmClassField::getSeqOrder));

        // 获取特殊字段
        List<SelectClassField> selectFields = new ArrayList<>();
        List<UserClassField> userFields = new ArrayList<>();
        List<GroupClassField> groupFields = new ArrayList<>();
        List<ScElClassField> scElFields = new ArrayList<>();
        List<LinkUserClassField> linkUserFields = new ArrayList<>();
        List<AddrClassField> addrClassFields = new ArrayList<>();
        for (VmClassField field : domainFields) {
            if (VmConstants.FIELD_TYPE_SELECT.equals(field.getFieldType())) {
                selectFields.add((SelectClassField) field);
            } else if (VmConstants.FIELD_TYPE_USER.equals(field.getFieldType())) {
                userFields.add((UserClassField) field);
            } else if (VmConstants.FIELD_TYPE_GROUP.equals(field.getFieldType())) {
                groupFields.add((GroupClassField) field);
            } else if (VmConstants.FIELD_TYPE_SC_EL.equals(field.getFieldType())) {
                scElFields.add((ScElClassField) field);
            } else if (VmConstants.FIELD_TYPE_ADDR.equals(field.getFieldType())) {
                addrClassFields.add((AddrClassField) field);
            } else if (VmConstants.FIELD_TYPE_LINK_USER.equals(field.getFieldType())) {
                linkUserFields.add((LinkUserClassField) field);
            }
        }

        // 构建选择字段的映射关系
        Map<String, Map<String, Integer>> fieldOptionsMap = new HashMap<>();
        for (SelectClassField field : selectFields) {
            SelectClassField.SelectFieldExtend fieldExtend = (SelectClassField.SelectFieldExtend)field.getFieldExtend();
            fieldOptionsMap.put(field.getFieldName(), fieldExtend.getOptionsMap());
        }

        /** 构建表头映射 */
        Map<String, String> mapperMap = new HashMap<>();
        vmClass.getClassFieldMap().forEach((fieldName, field) -> {
            if(VmConstants.FIELD_TYPE_SC_EL.equals(field.getFieldType())){
                ScElClassField.ScElFieldExtend fieldExtend = (ScElClassField.ScElFieldExtend)field.getFieldExtend();
                Integer depth = fieldExtend.getRealDepth();
                for (int i = 0; i < depth; i++) {
                    String subFieldTitle = field.getFieldTitle() + ScElClassField.DEPTH_SUFFIX[i];
                    String subFieldName = field.getFieldName() + ScElClassField.DEPTH_SUFFIX[i];
                    mapperMap.put(subFieldTitle, subFieldName);
                }
            }else if(VmConstants.FIELD_TYPE_ADDR.equals(field.getFieldType())){
                for (String suffix : Arrays.asList(AddrClassField.PROVINCE_ID_SUFFIX, AddrClassField.CITY_ID_SUFFIX, AddrClassField.AREA_ID_SUFFIX)) {
                    String titleSuffix = AddrClassField.PROVINCE_ID_SUFFIX.equals(suffix)?"省份":AddrClassField.CITY_ID_SUFFIX.equals(suffix)?"城市":"区域";
                    String subFieldTitle = field.getFieldTitle() + titleSuffix;
                    String subFieldName = field.getFieldName() + suffix;
                    mapperMap.put(subFieldTitle, subFieldName);
                }
            }else{
                mapperMap.put(field.getFieldTitle(), fieldName);
            }
        });
        List<String> mapperTitles = new ArrayList<>(mapperMap.keySet());
        Map<Integer, String> headColumnMap = new HashMap<>();

        /** 初始化Excel读写器 */
        // 初始化Excel读取器
        ExcelReader excelReader = EasyExcel.read(excelFile).build();
        ReadSheet sheet0 = excelReader.excelExecutor().sheetList().get(0);

        // 初始化Excel写出器
        List<WriteCellStyle> contentStyles = new ArrayList<>();
        GcsHorizontalCellStyleStrategy styleStrategy = new GcsHorizontalCellStyleStrategy(
                EasyExcelService.HEAD_CELL_STYLE, contentStyles
        );
        ExcelWriter excelWriter = EasyExcel.write(markedFile.getAbsolutePath())
                .registerWriteHandler(styleStrategy)
                .build();
        WriteSheet writeSheet = EasyExcel.writerSheet(sheet0.getSheetName()).build();

        /** 构建读取监听器 */
        int headRowNumber = 5;           // 头部可能行
        boolean[] headRowMark = {false}; // 头部已找出
        ReadSheet readSheet = EasyExcel.readSheet(sheet0.getSheetNo())
            .headRowNumber(headRowNumber)
            .registerReadListener(new AnalysisEventListener<Map<Integer, String>>() {
                private Logger logger1 = LoggerFactory.getLogger(this.getClass());
                private static final int BATCH_COUNT = 30;
                private Integer sheetTotalRow = 0;
                private List<MutableTriple<Integer, Map<Integer, String>, Map<String, Object>>> stepDataList =
                        ListUtils.newArrayListWithExpectedSize(BATCH_COUNT);

                @Override
                public void invokeHeadMap(Map<Integer, String> headMap, AnalysisContext context) {
                    if (!headRowMark[0]) {
                        List<String> rowHeads = new ArrayList<>(headMap.values());
                        boolean isHeadRow = rowHeads.stream().anyMatch(mapperTitles::contains);
                        if (isHeadRow) {
                            headRowMark[0] = true;
                            headMap.forEach((colIndex, colName) -> {
                                String fieldName = mapperMap.getOrDefault(colName, colName);
                                headColumnMap.put(colIndex, fieldName);
                            });
                            contentStyles.add(EasyExcelService.HEAD_CELL_STYLE);
                            excelWriter.write(Collections.singletonList(headMap), writeSheet);
                        } else {
                            contentStyles.add(EasyExcelService.NORMAL_CELL_STYLE);
                            excelWriter.write(Collections.singletonList(headMap), writeSheet);
                        }
                    } else {
                        // 头部以找出，headRowNumber数值内，剩下的行为数据行
                        ReadRowHolder rowHolder = context.readRowHolder();
                        stepDataList.add(MutableTriple.of( rowHolder.getRowIndex(), headMap, null));
                        _calcTotalRow(context);
                    }
                }

                @Override
                public void invoke(Map<Integer, String> dataMap, AnalysisContext context) {
                    ReadRowHolder rowHolder = context.readRowHolder();
                    stepDataList.add(MutableTriple.of(rowHolder.getRowIndex(), dataMap, null ));
                    _calcTotalRow(context);
                    if (stepDataList.size() >= BATCH_COUNT) {
                        _saveData();
                        stepDataList = ListUtils.newArrayListWithExpectedSize(BATCH_COUNT);
                    }
                }

                @Override
                public void doAfterAllAnalysed(AnalysisContext context) {
                    _saveData();
                }

                @Override
                public void onException(Exception exception, AnalysisContext context) {
                    logger1.warn("行 {} 解析异常: {}", context.readRowHolder().getRowIndex() + 1, exception.getMessage());
                }

                // 计算sheet总行数
                private void _calcTotalRow(AnalysisContext context) {
                    if (sheetTotalRow == 0) {
                        ReadSheetHolder sheetHolder = context.readSheetHolder();
                        sheetTotalRow = sheetHolder.getApproximateTotalRowNumber();
                    }
                }

                // 保存步长数据
                private void _saveData() {
                    // 封装步长数据
                    for (MutableTriple<Integer, Map<Integer, String>, Map<String, Object>> triple : stepDataList) {
                        Map<String, Object> rowData = new HashMap<>();
                        triple.getMiddle().forEach((colIndex, value) -> {
                            String fieldName = headColumnMap.getOrDefault(colIndex, colIndex.toString());
                            rowData.put(fieldName, value);
                        });
                        triple.setRight(rowData);
                    }

                    // 导入步长数据
                    for (MutableTriple<Integer, Map<Integer, String>, Map<String, Object>> triple : stepDataList) {
                        Map<Integer, String> dataMap = triple.getMiddle();
                        Map<String, Object> rowData = triple.getRight();
                        // Select类型转换
                        fieldOptionsMap.forEach((fieldName, options) -> {
                            Object value = rowData.get(fieldName);
                            if (value != null) {
                                rowData.put(fieldName, options.getOrDefault(value.toString(), null));
                            }
                        });
                        // User类型变化
                        for (UserClassField userField : userFields) {
                            Object nickname = rowData.get(userField.getFieldName());
                            if (nickname != null) {
                                GcsUser user = Db.getOne(Wrappers.lambdaQuery(GcsUser.class)
                                        .apply("LOWER(nickname) = LOWER({0})", nickname));
                                rowData.put(userField.getRealFieldName(), user != null ? user.getId() : null);
                            }
                        }
                        // Group类型变化
                        for (GroupClassField groupField : groupFields) {
                            Object groupName = rowData.get(groupField.getFieldName());
                            if (groupName != null) {
                                GcsGroup group = Db.getOne(Wrappers.lambdaQuery(GcsGroup.class).eq(GcsGroup::getName, groupName));
                                rowData.put(groupField.getRealFieldName(), group != null ? group.getId() : null);
                            }
                        }
                        // ScEl类型变化
                        for (ScElClassField scElField : scElFields) {
                            ScElClassField.ScElFieldExtend fieldExtend = (ScElClassField.ScElFieldExtend)scElField.getFieldExtend();
                            Integer depth = fieldExtend.getRealDepth();
                            String scKey = fieldExtend.getScKey();
                            List<ScElement> scElements = scElementService.getScElementTree(scKey);
                            ScElement currentNode = null;
                            for (int i = 0; i < depth; i++) {
                                String subFieldName = scElField.getFieldName() + ScElClassField.DEPTH_SUFFIX[i];
                                Object subFieldTitle = rowData.get(subFieldName);
                                if(i==0){
                                    currentNode = scElements.stream()
                                            .filter(e -> subFieldTitle.equals(e.getTitle()))
                                            .findFirst().orElse(null);
                                } else if (currentNode != null && currentNode.getChildren() != null) {
                                    currentNode = currentNode.getChildren().stream()
                                            .filter(e -> subFieldTitle.equals(e.getTitle()))
                                            .findFirst().orElse(null);
                                }
                                rowData.put(subFieldName, currentNode!=null ? currentNode.getId() : null);
                            }
                        }
                        for (AddrClassField addrField : addrClassFields) {
                            for (String suffix : Arrays.asList(AddrClassField.PROVINCE_ID_SUFFIX, AddrClassField.CITY_ID_SUFFIX, AddrClassField.AREA_ID_SUFFIX)) {
                                String subFieldName = addrField.getFieldName() + suffix;
                                Object propValue = rowData.get(subFieldName);
                                if(AddrClassField.PROVINCE_ID_SUFFIX.equals(suffix)){
                                    AddrProvince addrProvince = propValue!=null?Db.getOne(
                                        Wrappers.lambdaQuery(AddrProvince.class).eq(AddrProvince::getName, Convert.toStr(propValue))
                                    ):null;
                                    rowData.put(subFieldName, addrProvince!=null ? addrProvince.getId() : null);
                                }else if(AddrClassField.CITY_ID_SUFFIX.equals(suffix)){
                                    AddrCity addrCity = propValue!=null?Db.getOne(
                                            Wrappers.lambdaQuery(AddrCity.class).eq(AddrCity::getName, Convert.toStr(propValue))
                                    ):null;
                                    rowData.put(subFieldName, addrCity!=null ? addrCity.getId() : null);
                                } else {
                                    AddrArea addrArea = propValue!=null?Db.getOne(
                                            Wrappers.lambdaQuery(AddrArea.class).eq(AddrArea::getName, Convert.toStr(propValue))
                                    ):null;
                                    rowData.put(subFieldName, addrArea!=null ? addrArea.getId() : null);
                                }
                            }
                        }
                        for (LinkUserClassField linkUserField : linkUserFields){
                            Object linkUsers = rowData.get(linkUserField.getFieldName());
                            String[] linkUserArray = Convert.toStr(linkUsers, "").split("\\|");
                            List<HashMap> userInfoList = new ArrayList<>();
                            for(String nickname: linkUserArray){
                                GcsUser user = Db.getOne(Wrappers.lambdaQuery(GcsUser.class)
                                        .apply("LOWER(nickname) = LOWER({0})", nickname));
                                if(user!=null){
                                    HashMap userInfo = new HashMap();
                                    userInfo.put(BaseDomain.ID, user.getId());
                                    userInfoList.add(userInfo);
                                }
                            }
                            rowData.put(linkUserField.getFieldName(), userInfoList);
                        }
                        // 绑定实体字段
                        rowData.entrySet().removeIf(e -> StringUtils.isBlank(Convert.toStr(e.getValue())));
                        BaseDomain vmEntity = vmClass.bindVmDomain(vmClass.newVmDomain(), rowData);
                        // 数据实体校验
                        amountMap.put(VmDomainService.AMOUNT_TOTAL, amountMap.get(VmDomainService.AMOUNT_TOTAL) + 1);
                        try {
                            vmClass.validateVmDomain(vmEntity);
                        }catch (Exception e){
                            e.printStackTrace();
                            amountMap.put(VmDomainService.AMOUNT_INVALID, amountMap.get(VmDomainService.AMOUNT_INVALID) + 1);
                            contentStyles.add(EasyExcelService.GREY_CELL_STYLE);
                            excelWriter.write(Collections.singletonList(dataMap), writeSheet);
                            continue;
                        }
                        // 数据实体保存
                        BaseDomain saved = vmClass.saveVmDomain(vmEntity);
                        if (saved != null) {
                            amountMap.put(VmDomainService.AMOUNT_SUCCESS, amountMap.get(VmDomainService.AMOUNT_SUCCESS) + 1);
                            contentStyles.add(EasyExcelService.NORMAL_CELL_STYLE);
                            excelWriter.write(Collections.singletonList(dataMap), writeSheet);
                        } else {
                            contentStyles.add(EasyExcelService.RED_CELL_STYLE);
                            excelWriter.write(Collections.singletonList(dataMap), writeSheet); // 失败数据(未知失败)
                        }
                    }

                    // 导入步长回调
                    if (stepCallback != null) {
                        stepCallback.call(stepDataList, sheetTotalRow);
                    }

                    // Sleep延缓导入
                    try {
                        Thread.sleep(100);
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                    }
                }
            }).build();

        excelReader.read(readSheet);
        excelReader.finish();
        excelWriter.finish();

        return markedFile;
    }

    @FunctionalInterface
    interface StepCallback {
        void call(List<MutableTriple<Integer, Map<Integer, String>, Map<String, Object>>> stepDataList, Integer sheetTotalRow);
    }

    public static Map<String, Object> domainEntityMap(String domainName, String recordIdStr) {
        return domainEntityMap(domainName, recordIdStr, null);
    }

    public static Map<String, Object> domainEntityMap(String domainName, String recordIdStr, Map<String, Object> contextMap) {
        VmClassService vmClassService = GcsHolder.getBean(VmClassService.class);
        VmClass vmClass = vmClassService.resolveVmClass(domainName, true);
        if(vmClass!=null && StringUtils.isNotBlank(recordIdStr)){
            BaseDomain entity = (BaseDomain)vmClass.getDomainMapper().selectById(Convert.toLong(recordIdStr));
            if(entity!=null){
                return vmClass.vmDomainEntityMap(entity);
            }
        }
        return null;
    }

    /**
     * 批量回填关联字段数据（MANY_TO_ONE → {field}_entity；ONE_TO_MANY → {field}_entities）
     */
    public void enrichRelations(List<Map<String, Object>> records, VmClass vmClass, List<String> fieldNames) {
        if (records == null || records.isEmpty() || fieldNames == null || fieldNames.isEmpty()) {
            return;
        }
        for (String fieldName : fieldNames) {
            VmClassField field = vmClass.getClassFieldMap().get(fieldName);
            if (field == null) {
                continue;
            }
            if (VmConstants.FIELD_TYPE_MANY2ONE.equals(field.getFieldType())) {
                enrichManyToOne(records, field);
            } else if (VmConstants.FIELD_TYPE_ONE2MANY.equals(field.getFieldType())) {
                enrichOneToMany(records, field);
            }
        }
    }

    private void enrichManyToOne(List<Map<String, Object>> records, VmClassField field) {
        String relDomainName = field.getExtendMap().getStr(ManyToOneClassField.REL_DOMAIN_NAME);
        if (StringUtils.isBlank(relDomainName)) {
            logger.warn("关联字段[{}]的关联域为空，跳过批量加载", field.getFieldName());
            return;
        }
        VmClass relVmClass = vmClassService.resolveVmClass(relDomainName, true);
        if (relVmClass == null) {
            logger.warn("关联字段[{}]的关联域[{}]解析失败，跳过批量加载", field.getFieldName(), relDomainName);
            return;
        }
        String fkField = field.getRealFieldName(); // fieldName + "Id"
        List<Long> ids = records.stream()
            .map(r -> Convert.toLong(r.get(fkField)))
            .filter(Objects::nonNull)
            .distinct()
            .collect(Collectors.toList());
        if (ids.isEmpty()) {
            return;
        }
        List<BaseDomain> relEntities = relVmClass.getDomainMapper().selectBatchIds(ids);
        Map<Long, Map<String, Object>> relMap = new HashMap<>();
        for (BaseDomain entity : relEntities) {
            Map<String, Object> em = relVmClass.vmDomainEntityMap(entity);
            relMap.put(Convert.toLong(em.get(BaseDomain.ID)), em);
        }
        for (Map<String, Object> record : records) {
            Long fk = Convert.toLong(record.get(fkField));
            if (fk != null) {
                record.put(field.getFieldName() + "_entity", relMap.get(fk));
            }
        }
    }

    private void enrichOneToMany(List<Map<String, Object>> records, VmClassField field) {
        String relDomainName = field.getExtendMap().getStr(OneToManyClassField.REL_DOMAIN_NAME);
        String relDomainFieldName = field.getExtendMap().getStr(OneToManyClassField.REL_DOMAIN_FIELD_NAME);
        if (StringUtils.isBlank(relDomainName) || StringUtils.isBlank(relDomainFieldName)) {
            logger.warn("一对多字段[{}]关联配置不完整，跳过批量加载", field.getFieldName());
            return;
        }
        VmClass relVmClass = vmClassService.resolveVmClass(relDomainName, true);
        if (relVmClass == null) {
            logger.warn("一对多字段[{}]的关联域[{}]解析失败，跳过批量加载", field.getFieldName(), relDomainName);
            return;
        }
        String relFkField = relDomainFieldName + VmClassField.ID_SUFFIX; // 子表回指字段，如 parentId
        List<Long> parentIds = records.stream()
            .map(r -> Convert.toLong(r.get(BaseDomain.ID)))
            .filter(Objects::nonNull)
            .distinct()
            .collect(Collectors.toList());
        if (parentIds.isEmpty()) {
            return;
        }
        String relFkColumn = VmUtils.toLowerUnderscore(relFkField);
        List<BaseDomain> children = relVmClass.getDomainMapper().selectList(
            Wrappers.query(relVmClass.getDomainClass()).in(relFkColumn, parentIds)
        );
        Map<Long, List<Map<String, Object>>> groupMap = new HashMap<>();
        for (BaseDomain child : children) {
            Map<String, Object> cm = relVmClass.vmDomainEntityMap(child);
            Long parentId = Convert.toLong(cm.get(relFkField));
            groupMap.computeIfAbsent(parentId, k -> new ArrayList<>()).add(cm);
        }
        for (Map<String, Object> record : records) {
            Long id = Convert.toLong(record.get(BaseDomain.ID));
            record.put(field.getFieldName() + "_entities",
                groupMap.getOrDefault(id, Collections.emptyList()));
        }
    }
}

