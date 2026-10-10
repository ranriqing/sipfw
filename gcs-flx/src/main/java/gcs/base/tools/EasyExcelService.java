package gcs.base.tools;

import cn.hutool.core.io.FileUtil;
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
import com.alibaba.excel.write.builder.ExcelWriterSheetBuilder;
import com.alibaba.excel.write.metadata.WriteSheet;
import com.alibaba.excel.write.metadata.style.WriteCellStyle;
import com.alibaba.excel.write.metadata.style.WriteFont;
import com.alibaba.excel.write.style.HorizontalCellStyleStrategy;
import gcs.base.tools.easyexcel.GcsHorizontalCellStyleStrategy;
import org.apache.commons.lang3.tuple.MutableTriple;
import org.apache.poi.openxml4j.exceptions.NotOfficeXmlFileException;
import org.apache.poi.ss.usermodel.IndexedColors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.io.Resource;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;
import org.springframework.core.io.support.ResourcePatternResolver;

import javax.servlet.http.HttpServletResponse;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.URLEncoder;
import java.util.*;
import java.util.function.Consumer;

/**
 * EasyExcel文件读写服务类
 * 基于阿里巴巴EasyExcel框架实现Excel文件的读取、写入、格式处理等操作
 * 官方文档: https://easyexcel.opensource.alibaba.com/docs/current/
 */
public class EasyExcelService {
    // 日志记录器
    private static final Logger log = LoggerFactory.getLogger(EasyExcelService.class);

    // 单元格样式常量 - 表头样式
    public static final WriteCellStyle HEAD_CELL_STYLE;
    // 单元格样式常量 - 普通单元格样式
    public static final WriteCellStyle NORMAL_CELL_STYLE;
    // 单元格样式常量 - 灰色单元格样式
    public static final WriteCellStyle GREY_CELL_STYLE;
    // 单元格样式常量 - 橙色单元格样式
    public static final WriteCellStyle ORANGE_CELL_STYLE;
    // 单元格样式常量 - 红色单元格样式
    public static final WriteCellStyle RED_CELL_STYLE;

    /**
     * 静态初始化块 - 初始化所有单元格样式
     */
    static {
        // 初始化表头样式
        WriteCellStyle headCellStyle = new WriteCellStyle();
        WriteFont headWriteFont = new WriteFont();
        headWriteFont.setBold(true); // 加粗
        headWriteFont.setColor(IndexedColors.BLACK.getIndex()); // 黑色字体
        headWriteFont.setFontHeightInPoints((short) 12); // 12号字体
        headCellStyle.setWriteFont(headWriteFont);
        HEAD_CELL_STYLE = headCellStyle;

        // 初始化普通单元格样式
        WriteCellStyle normalCellStyle = new WriteCellStyle();
        WriteFont normalWriteFont = new WriteFont();
        normalWriteFont.setColor(IndexedColors.BLACK.getIndex()); // 黑色字体
        normalWriteFont.setFontHeightInPoints((short) 12); // 12号字体
        normalCellStyle.setWriteFont(normalWriteFont);
        NORMAL_CELL_STYLE = normalCellStyle;

        // 初始化灰色单元格样式
        WriteCellStyle greyCellStyle = new WriteCellStyle();
        WriteFont greyWriteFont = new WriteFont();
        greyWriteFont.setColor(IndexedColors.GREY_50_PERCENT.getIndex()); // 灰色字体
        greyWriteFont.setFontHeightInPoints((short) 12); // 12号字体
        greyCellStyle.setWriteFont(greyWriteFont);
        GREY_CELL_STYLE = greyCellStyle;

        // 初始化橙色单元格样式
        WriteCellStyle orangeCellStyle = new WriteCellStyle();
        WriteFont orangeWriteFont = new WriteFont();
        orangeWriteFont.setColor(IndexedColors.ORANGE.getIndex()); // 橙色字体
        orangeWriteFont.setFontHeightInPoints((short) 12); // 12号字体
        orangeCellStyle.setWriteFont(orangeWriteFont);
        ORANGE_CELL_STYLE = orangeCellStyle;

        // 初始化红色单元格样式
        WriteCellStyle redCellStyle = new WriteCellStyle();
        WriteFont redWriteFont = new WriteFont();
        redWriteFont.setColor(IndexedColors.RED.getIndex()); // 红色字体
        redWriteFont.setFontHeightInPoints((short) 12); // 12号字体
        redCellStyle.setWriteFont(redWriteFont);
        RED_CELL_STYLE = redCellStyle;
    }

    public static void writeToResponse(HttpServletResponse response, File excelFile) throws IOException {
        writeToResponse(response, excelFile, "export.xlsx");
    }
    public static void writeToResponse(HttpServletResponse response, File excelFile, String filename) throws IOException {
        writeToResponse(response, FileUtil.readBytes(excelFile), filename);
    }
    public static void writeToResponse(HttpServletResponse response, ByteArrayOutputStream excelStream) throws IOException {
        writeToResponse(response, excelStream, "export.xlsx");
    }
    public static void writeToResponse(HttpServletResponse response, ByteArrayOutputStream excelStream, String filename) throws IOException {
        writeToResponse(response, excelStream.toByteArray(), filename);
    }
    public static void writeToResponse(HttpServletResponse response, byte[] bytes, String filename) throws IOException {
        response.setHeader("Content-Disposition", "attachment; filename=\"" + URLEncoder.encode(filename, "UTF-8") + ".xlsx\"");
        response.setContentType("application/excel");
        response.getOutputStream().write(bytes);
        response.flushBuffer();
    }

    /**
     * 清除Excel文件的原有格式，使用统一格式重新生成
     * 用途：标准化Excel文件格式，移除可能存在的复杂格式影响
     *
     * @param sourceFile 源Excel文件
     * @return 清除格式后的新Excel文件
     */
    public static File cleanExcelStyle(File sourceFile) {
        try {
            // 创建临时文件作为目标文件
            File targetFile = File.createTempFile(Long.toString(System.currentTimeMillis()), ".xlsx");

            // 创建ExcelWriter，设置统一的表头和内容样式
            ExcelWriter excelWriter = EasyExcel.write(targetFile.getAbsolutePath())
                    .registerWriteHandler(new HorizontalCellStyleStrategy(HEAD_CELL_STYLE, NORMAL_CELL_STYLE))
                    .build();

            // 创建ExcelReader读取源文件
            ExcelReader excelReader = EasyExcel.read(sourceFile).build();
            List<ReadSheet> readSheets = excelReader.excelExecutor().sheetList();
            for (ReadSheet sheet : readSheets) {
                // 创建对应的写入工作表
                WriteSheet writeSheet = EasyExcel.writerSheet(sheet.getSheetName()).build();

                // 创建读取工作表，注册数据监听器
                ReadSheet readSheet = EasyExcel.readSheet(sheet.getSheetNo())
                        .registerReadListener(new AnalysisEventListener<Map<Integer, String>>() {
                            // 批量处理的数量
                            private final Integer BATCH_COUNT = 50;
                            // 临时存储批量数据
                            private List<Map<Integer, String>> stepDataList = ListUtils.newArrayListWithExpectedSize(BATCH_COUNT);

                            /**
                             * 每读取一行数据触发
                             * @param dataMap 一行数据，key为列索引，value为单元格值
                             * @param context 分析上下文
                             */
                            @Override
                            public void invoke(Map<Integer, String> dataMap, AnalysisContext context) {
                                stepDataList.add(dataMap);
                                // 达到批量处理数量时写入数据
                                if (stepDataList.size() >= BATCH_COUNT) {
                                    saveData();
                                    stepDataList = ListUtils.newArrayListWithExpectedSize(BATCH_COUNT);
                                }
                            }

                            /**
                             * 所有数据读取完成后触发
                             * @param context 分析上下文
                             */
                            @Override
                            public void doAfterAllAnalysed(AnalysisContext context) {
                                // 处理剩余数据
                                saveData();
                            }

                            /**
                             * 将批量数据写入到新Excel文件
                             */
                            private void saveData() {
                                excelWriter.write(stepDataList, writeSheet);
                            }
                        }).build();
                // 读取当前工作表
                excelReader.read(readSheet);
            }

            // 关闭读取器和写入器，释放资源
            excelReader.finish();
            excelWriter.finish();

            return targetFile;
        } catch (IOException e) {
            log.error("清除Excel格式失败", e);
            throw new RuntimeException("清除Excel格式失败", e);
        }
    }

    /**
     * Excel读取示例方法
     * 演示如何读取Excel文件内容并进行简单处理
     */
    public void readExample() {
        try {
            // 创建资源解析器，用于获取类路径下的资源文件
            ResourcePatternResolver resolver = new PathMatchingResourcePatternResolver();
            // 获取示例Excel文件资源
            Resource resource = resolver.getResource("easyexcel/phone.xls");

            // 定义表头字段映射关系：Excel表头文本 -> 目标字段名
            Map<String, String> mapperMap = new HashMap<>();
            mapperMap.put("电话", "phone");
            mapperMap.put("备注", "remark");
            // 获取所有需要映射的表头文本
            List<String> mapperTitles = new ArrayList<>(mapperMap.keySet());
            // 表头可能所在的行号
            int headRowNumber = 5;
            // 标记是否已找到表头行（使用数组是为了在匿名内部类中修改）
            boolean[] headRowMark = {false};
            // 存储列索引与目标字段名的映射关系
            Map<Integer, String> headRowColumnMap = new HashMap<>();

            // 读取Excel文件，注册数据监听器
            EasyExcel.read(resource.getInputStream(), new AnalysisEventListener<Map<Integer, String>>() {
                // 批量处理的数量
                private final Integer BATCH_COUNT = 50;
                // 工作表总行数
                private Integer sheetTotalRow = 0;
                // 临时存储批量数据的三元组：行索引 -> 原始数据 -> 转换后的数据
                private List<MutableTriple<Integer, Map<Integer, String>, Map<String, Object>>> stepDataList =
                        ListUtils.newArrayListWithExpectedSize(BATCH_COUNT);

                /**
                 * 读取表头行时触发
                 * @param headMap 表头数据，key为列索引，value为表头文本
                 * @param context 分析上下文
                 */
                @Override
                public void invokeHeadMap(Map<Integer, String> headMap, AnalysisContext context) {
                    // 如果还未找到表头行
                    if (!headRowMark[0]) {
                        List<String> rowHeadValues = new ArrayList<>(headMap.values());
                        // 判断当前行是否包含需要映射的表头文本
                        boolean isHeadRow = rowHeadValues.stream().anyMatch(mapperTitles::contains);

                        if (isHeadRow) {
                            // 标记已找到表头行
                            headRowMark[0] = true;
                            // 建立列索引与目标字段名的映射关系
                            for (Map.Entry<Integer, String> entry : headMap.entrySet()) {
                                Integer colIndex = entry.getKey();
                                String colName = entry.getValue();
                                String colField = mapperMap.get(colName);
                                if (colField != null) {
                                    headRowColumnMap.put(colIndex, colField);
                                } else {
                                    headRowColumnMap.put(colIndex, colName);
                                }
                            }
                        }
                    } else {
                        // 已找到表头行，后续表头行作为数据行处理
                        ReadRowHolder rowHolder = context.readRowHolder();
                        MutableTriple<Integer, Map<Integer, String>, Map<String, Object>> mutableTriple =
                                MutableTriple.of(rowHolder.getRowIndex(), headMap, null);
                        stepDataList.add(mutableTriple);

                        // 获取工作表总行数
                        if (sheetTotalRow == null) {
                            ReadSheetHolder sheetHolder = context.readSheetHolder();
                            sheetTotalRow = sheetHolder.getApproximateTotalRowNumber();
                        }
                    }
                }

                /**
                 * 读取数据行时触发
                 * @param dataMap 行数据，key为列索引，value为单元格值
                 * @param context 分析上下文
                 */
                @Override
                public void invoke(Map<Integer, String> dataMap, AnalysisContext context) {
                    ReadRowHolder rowHolder = context.readRowHolder();
                    // 创建三元组存储行索引、原始数据和转换后的数据
                    MutableTriple<Integer, Map<Integer, String>, Map<String, Object>> mutableTriple =
                            MutableTriple.of(rowHolder.getRowIndex(), dataMap, null);
                    stepDataList.add(mutableTriple);

                    // 获取工作表总行数
                    if (sheetTotalRow == null) {
                        ReadSheetHolder sheetHolder = context.readSheetHolder();
                        sheetTotalRow = sheetHolder.getApproximateTotalRowNumber();
                    }

                    // 达到批量处理数量时处理数据
                    if (stepDataList.size() >= BATCH_COUNT) {
                        saveData();
                        stepDataList = ListUtils.newArrayListWithExpectedSize(BATCH_COUNT);
                    }
                }

                /**
                 * 所有数据读取完成后触发
                 * @param context 分析上下文
                 */
                @Override
                public void doAfterAllAnalysed(AnalysisContext context) {
                    // 处理剩余数据
                    saveData();
                }

                /**
                 * 处理读取过程中发生的异常
                 * @param exception 异常对象
                 * @param context 分析上下文
                 */
                @Override
                public void onException(Exception exception, AnalysisContext context) {
                    log.warn("行处理异常: {}", exception.getMessage());
                }

                /**
                 * 处理批量数据
                 * 将原始数据转换为映射后的字段数据，并进行简单的业务处理
                 */
                private void saveData() {
                    for (MutableTriple<Integer, Map<Integer, String>, Map<String, Object>> dataTriple : stepDataList) {
                        // 构建映射后的行数据
                        Map<String, Object> rowData = new HashMap<>();
                        for (Map.Entry<Integer, String> entry : dataTriple.getMiddle().entrySet()) {
                            Integer dataKey = entry.getKey();
                            String dataValue = entry.getValue();
                            // 获取映射后的字段名，若没有映射则使用列索引
                            String mappedDataKey = headRowColumnMap.getOrDefault(dataKey, dataKey.toString());
                            rowData.put(mappedDataKey, dataValue);
                        }
                        // 存储转换后的数据
                        dataTriple.setRight(rowData);

                        // 简单的业务处理：提取并验证电话号码
                        String phone = (String) dataTriple.getRight().get("phone");
                        if (phone != null) {
                            phone = phone.trim();
                            if (phone.matches("\\d+")) {
                                System.out.println(phone);
                            }
                        }
                    }
                }
            }).sheet().headRowNumber(headRowNumber).doRead();
        } catch (Exception e) {
            // 捕获并处理不同类型的异常
            if (e instanceof ExcelCommonException) {
                log.warn("ExcelCommonException", e);
            } else if (e instanceof NotOfficeXmlFileException) {
                log.warn("NotOfficeXmlFileException", e);
            } else {
                log.warn("Exception", e);
            }
        }
    }

    /**
     * Excel导入示例方法
     * 演示如何读取Excel文件，处理数据，并生成带有标记的新Excel文件
     *
     * @param stepConsumer 批量数据处理的回调函数
     * @return 处理后的带有标记的Excel文件
     */
    public File importExample(Consumer<List<MutableTriple<Integer, Map<Integer, String>, Map<String, Object>>>> stepConsumer) {
        try {
            // 创建临时文件存储处理后的结果
            File markedFile = File.createTempFile(Long.toString(System.currentTimeMillis()), ".xlsx");
            // 创建资源解析器，获取示例Excel文件
            ResourcePatternResolver resolver = new PathMatchingResourcePatternResolver();
            Resource resource = resolver.getResource("easyexcel/phone.xls");

            // 定义表头字段映射关系
            Map<String, String> mapperMap = new HashMap<>();
            mapperMap.put("电话", "phone");
            mapperMap.put("备注", "remark");
            List<String> mapperTitles = new ArrayList<>(mapperMap.keySet());
            int headRowNumber = 5;
            boolean[] headRowMark = {false};
            Map<Integer, String> headRowColumnMap = new HashMap<>();

            // 创建ExcelReader读取源文件
            ExcelReader excelReader = EasyExcel.read(resource.getInputStream()).build();
            List<ReadSheet> readSheets = excelReader.excelExecutor().sheetList();
            if (readSheets.isEmpty()) {
                throw new RuntimeException("Excel文件中没有工作表");
            }
            // 获取第一个工作表
            ReadSheet sheet0 = readSheets.get(0);

            // 用于存储单元格样式的列表
            List<WriteCellStyle> contentCellStyleList = new ArrayList<>();
            // 自定义单元格样式策略
            GcsHorizontalCellStyleStrategy cellStyleStrategy = new GcsHorizontalCellStyleStrategy(HEAD_CELL_STYLE, contentCellStyleList);

            // 创建ExcelWriter写入处理后的文件
            ExcelWriter excelWriter = EasyExcel.write(markedFile.getAbsolutePath())
                    .registerWriteHandler(cellStyleStrategy)
                    .build();
            WriteSheet writeSheet = EasyExcel.writerSheet(sheet0.getSheetName()).build();

            // 创建读取工作表，注册数据监听器
            ReadSheet readSheet = EasyExcel.readSheet(sheet0.getSheetNo())
                    .headRowNumber(headRowNumber)
                    .registerReadListener(new AnalysisEventListener<Map<Integer, String>>() {
                        // 批量处理的数量
                        private final Integer BATCH_COUNT = 30;
                        // 工作表总行数
                        private Integer sheetTotalRow = 0;
                        // 临时存储批量数据的三元组
                        private List<MutableTriple<Integer, Map<Integer, String>, Map<String, Object>>> stepDataList =
                                ListUtils.newArrayListWithExpectedSize(BATCH_COUNT);

                        /**
                         * 读取表头行时触发
                         */
                        @Override
                        public void invokeHeadMap(Map<Integer, String> headMap, AnalysisContext context) {
                            if (!headRowMark[0]) {
                                List<String> rowHeadValues = new ArrayList<>(headMap.values());
                                boolean isHeadRow = rowHeadValues.stream().anyMatch(mapperTitles::contains);

                                if (isHeadRow) {
                                    headRowMark[0] = true;
                                    // 建立列索引与字段名的映射
                                    for (Map.Entry<Integer, String> entry : headMap.entrySet()) {
                                        Integer colIndex = entry.getKey();
                                        String colName = entry.getValue();
                                        String colField = mapperMap.get(colName);
                                        if (colField != null) {
                                            headRowColumnMap.put(colIndex, colField);
                                        } else {
                                            headRowColumnMap.put(colIndex, colName);
                                        }
                                    }
                                    // 添加表头样式并写入表头
                                    contentCellStyleList.add(HEAD_CELL_STYLE);
                                    excelWriter.write(Collections.singletonList(headMap), writeSheet);
                                } else {
                                    // 非目标表头行，使用普通样式
                                    contentCellStyleList.add(NORMAL_CELL_STYLE);
                                    excelWriter.write(Collections.singletonList(headMap), writeSheet);
                                }
                            } else {
                                // 已找到表头行，后续表头行作为数据行处理
                                ReadRowHolder rowHolder = context.readRowHolder();
                                MutableTriple<Integer, Map<Integer, String>, Map<String, Object>> mutableTriple =
                                        MutableTriple.of(rowHolder.getRowIndex(), headMap, null);
                                stepDataList.add(mutableTriple);

                                if (sheetTotalRow == null) {
                                    ReadSheetHolder sheetHolder = context.readSheetHolder();
                                    sheetTotalRow = sheetHolder.getApproximateTotalRowNumber();
                                }
                            }
                        }

                        /**
                         * 读取数据行时触发
                         */
                        @Override
                        public void invoke(Map<Integer, String> dataMap, AnalysisContext context) {
                            ReadRowHolder rowHolder = context.readRowHolder();
                            MutableTriple<Integer, Map<Integer, String>, Map<String, Object>> mutableTriple =
                                    MutableTriple.of(rowHolder.getRowIndex(), dataMap, null);
                            stepDataList.add(mutableTriple);

                            if (sheetTotalRow == null) {
                                ReadSheetHolder sheetHolder = context.readSheetHolder();
                                sheetTotalRow = sheetHolder.getApproximateTotalRowNumber();
                            }

                            // 达到批量处理数量时处理数据
                            if (stepDataList.size() >= BATCH_COUNT) {
                                saveData();
                                stepDataList = ListUtils.newArrayListWithExpectedSize(BATCH_COUNT);
                            }
                        }

                        /**
                         * 所有数据读取完成后触发
                         */
                        @Override
                        public void doAfterAllAnalysed(AnalysisContext context) {
                            saveData();
                        }

                        /**
                         * 处理读取过程中发生的异常
                         */
                        @Override
                        public void onException(Exception exception, AnalysisContext context) {
                            log.warn("行 {} 异常: {}", context.readRowHolder().getRowIndex() + 1, exception.getMessage());
                        }

                        /**
                         * 处理批量数据
                         * 转换数据格式，根据数据有效性标记不同样式，并调用回调函数
                         */
                        private void saveData() {
                            // 封装步长数据，转换为映射后的字段格式
                            for (MutableTriple<Integer, Map<Integer, String>, Map<String, Object>> dataTriple : stepDataList) {
                                Map<String, Object> rowData = new HashMap<>();
                                for (Map.Entry<Integer, String> entry : dataTriple.getMiddle().entrySet()) {
                                    Integer dataKey = entry.getKey();
                                    String dataValue = entry.getValue();
                                    String mappedDataKey = headRowColumnMap.getOrDefault(dataKey, dataKey.toString());
                                    rowData.put(mappedDataKey, dataValue);
                                }
                                dataTriple.setRight(rowData);
                            }

                            // 导入步长数据，根据电话号码有效性设置不同样式
                            for (MutableTriple<Integer, Map<Integer, String>, Map<String, Object>> dataTriple : stepDataList) {
                                Map<Integer, String> dataMap = dataTriple.getMiddle();
                                String phone = (String) dataTriple.getRight().get("phone");

                                if (phone != null) {
                                    phone = phone.trim();
                                    // 验证电话号码格式
                                    if (phone.matches("\\d+")) {
                                        contentCellStyleList.add(NORMAL_CELL_STYLE);
                                        excelWriter.write(Collections.singletonList(dataMap), writeSheet);
                                        System.out.println(phone);
                                    } else {
                                        // 无效电话号码，使用红色样式标记
                                        contentCellStyleList.add(RED_CELL_STYLE);
                                        excelWriter.write(Collections.singletonList(dataMap), writeSheet);
                                    }
                                } else {
                                    // 无电话号码，使用红色样式标记
                                    contentCellStyleList.add(RED_CELL_STYLE);
                                    excelWriter.write(Collections.singletonList(dataMap), writeSheet);
                                }
                            }

                            // 调用回调函数处理批量数据
                            if (stepConsumer != null) {
                                stepConsumer.accept(stepDataList);
                            }

                            // 延缓导入速度，避免资源占用过高
                            try {
                                Thread.sleep(100);
                            } catch (InterruptedException e) {
                                Thread.currentThread().interrupt();
                                log.warn("线程休眠被中断", e);
                            }
                        }
                    }).build();

            // 读取工作表数据
            excelReader.read(readSheet);
            // 关闭读取器和写入器
            excelReader.finish();
            excelWriter.finish();

            return markedFile;
        } catch (Exception e) {
            // 异常处理
            if (e instanceof ExcelCommonException) {
                log.warn("ExcelCommonException", e);
            } else if (e instanceof NotOfficeXmlFileException) {
                log.warn("NotOfficeXmlFileException", e);
            } else {
                log.warn("Exception", e);
            }
            return null;
        }
    }

    /**
     * Excel写入示例方法
     * 演示如何创建并写入Excel文件，包含表头和数据内容
     */
    public void writeExample() {
        try {
            // 创建临时文件存储写入结果
            File tmpFile = File.createTempFile(Long.toString(System.currentTimeMillis()), ".xlsx");
            // ExcelWriter excelWriter = EasyExcel.write(excelStream).build()
            // WriteSheet writeSheet = EasyExcel.writerSheet('sheet1').build()
            // excelWriter.write([headerDataList], writeSheet)
            // excelWriter.write([contentDataList], writeSheet)
            // excelWriter.finish()

            // 写入Excel文件
            EasyExcel.write(tmpFile)
                    .sheet("sheet1") // 指定工作表名称
                    .doWrite(() -> {
                        List<List<WriteCellData<String>>> allDataList = new ArrayList<>();

                        // 构建表格头部
                        List<WriteCellData<String>> headerDataList = new ArrayList<>();
                        List<Map<String, Object>> allFields = new ArrayList<>();
                        allFields.add(createFieldMap("phone", "电话", true));
                        allFields.add(createFieldMap("remark", "备注", false));

                        // 设置表头单元格样式
                        for (Map<String, Object> field : allFields) {
                            WriteCellData<String> headerCellData = new WriteCellData<>(field.get("fieldName").toString());
                            if ((boolean) field.get("required")) {
                                headerCellData.setWriteCellStyle(HEAD_CELL_STYLE);
                            } else {
                                headerCellData.setWriteCellStyle(NORMAL_CELL_STYLE);
                            }
                            headerDataList.add(headerCellData);
                        }
                        allDataList.add(headerDataList);

                        // 构建表格内容数据
                        List<Map<String, String>> instanceList = new ArrayList<>();
                        Map<String, String> instance1 = new HashMap<>();
                        instance1.put("phone", "13800010001");
                        instance1.put("remark", "投诉号码");
                        instanceList.add(instance1);

                        Map<String, String> instance2 = new HashMap<>();
                        instance2.put("phone", "13800010002");
                        instance2.put("remark", "无意向号码");
                        instanceList.add(instance2);

                        Map<String, String> instance3 = new HashMap<>();
                        instance3.put("phone", "13800010003");
                        instance3.put("remark", "");
                        instanceList.add(instance3);

                        // 构建内容单元格数据
                        for (Map<String, String> instance : instanceList) {
                            List<WriteCellData<String>> contentDataList = new ArrayList<>();
                            for (Map<String, Object> fieldMap : allFields) {
                                String fieldName = (String) fieldMap.get("field");
                                WriteCellData<String> contentCellData = new WriteCellData<>(instance.get(fieldName));
                                contentDataList.add(contentCellData);
                            }
                            allDataList.add(contentDataList);
                        }

                        return allDataList;
                    });

            // 输出文件路径
            System.out.println(tmpFile.getAbsolutePath());
        } catch (IOException e) {
            log.error("写入Excel失败", e);
            throw new RuntimeException("写入Excel失败", e);
        }
    }

    // 创建字段映射关系
    private Map<String, Object> createFieldMap(String field, String fieldName, boolean required) {
        Map<String, Object> map = new HashMap<>();
        map.put("field", field);
        map.put("fieldName", fieldName);
        map.put("required", required);
        return map;
    }

    /**
     * 无对象写入示例
     * 演示不使用Java对象，直接通过列表数据写入Excel
     */
    public void noModelWrite() {
        try {
            // 创建临时文件
            File tmpFile = File.createTempFile(Long.toString(System.currentTimeMillis()), ".xlsx");

            // 写出Excel
            EasyExcel.write(tmpFile.getAbsolutePath())
                    .registerWriteHandler(new HorizontalCellStyleStrategy(HEAD_CELL_STYLE, NORMAL_CELL_STYLE))
                    .sheet("模板") // 工作表名称
                    .head(Arrays.asList(
                            Collections.singletonList("邮编"),
                            Collections.singletonList("名称")
                    )) // 表头数据
                    .doWrite(() -> {
                        // 准备内容数据
                        List<List<String>> data = new ArrayList<>();
                        data.add(Arrays.asList("110000", "北京市"));
                        data.add(Arrays.asList("120000", "天津市"));
                        return data;
                    });

            // 输出文件路径
            System.out.println(tmpFile.getAbsolutePath());
        } catch (IOException e) {
            log.error("无模型写入Excel失败", e);
            throw new RuntimeException("无模型写入Excel失败", e);
        }
    }

    /**
     * 简单填充示例
     * 演示使用模板文件进行简单数据填充
     */
    public void simpleFill() {
        try {
            // 创建临时文件
            File tmpFile = File.createTempFile(Long.toString(System.currentTimeMillis()), ".xlsx");
            // 获取模板文件资源
            ResourcePatternResolver resolver = new PathMatchingResourcePatternResolver();
            Resource resource = resolver.getResource("easyexcel/tmpl_simple.xlsx");

            // 使用try-with-resources自动关闭输入流
            try (InputStream inputStream = resource.getInputStream()) {
                // 创建写入器并指定模板
                ExcelWriterSheetBuilder excelWriterSheetBuilder = EasyExcel.write(tmpFile.getAbsolutePath())
                        .withTemplate(inputStream)
                        .sheet();

                // 准备填充数据
                Map<String, Object> data = new HashMap<>();
                data.put("code", "110000");
                data.put("name", "北京市");
                // 执行填充
                excelWriterSheetBuilder.doFill(data);
            }

            // 输出文件路径
            System.out.println(tmpFile.getAbsolutePath());
        } catch (IOException e) {
            log.error("简单填充Excel失败", e);
            throw new RuntimeException("简单填充Excel失败", e);
        }
    }

    /**
     * 列表填充示例
     * 演示使用模板文件进行列表数据填充
     */
    public void listFill() {
        try {
            // 创建临时文件
            File tmpFile = File.createTempFile(Long.toString(System.currentTimeMillis()), ".xlsx");
            // 获取模板文件资源
            ResourcePatternResolver resolver = new PathMatchingResourcePatternResolver();
            Resource resource = resolver.getResource("easyexcel/tmpl_list.xlsx");

            // 使用try-with-resources自动关闭输入流
            try (InputStream inputStream = resource.getInputStream()) {
                // 写入并填充数据
                EasyExcel.write(tmpFile.getAbsolutePath())
                        .withTemplate(inputStream)
                        .sheet()
                        .doWrite(() -> {
                            // 准备列表数据
                            List<Map<String, Object>> data = new ArrayList<>();

                            Map<String, Object> item1 = new HashMap<>();
                            item1.put("code", "110000");
                            item1.put("name", "北京市");
                            data.add(item1);

                            Map<String, Object> item2 = new HashMap<>();
                            item2.put("code", "120000");
                            item2.put("name", "天津市");
                            data.add(item2);

                            return data;
                        });
            }

            // 输出文件路径
            System.out.println(tmpFile.getAbsolutePath());
        } catch (IOException e) {
            log.error("列表填充Excel失败", e);
            throw new RuntimeException("列表填充Excel失败", e);
        }
    }
}
