package gcs.base.tools;

import cn.hutool.core.collection.ListUtil;
import cn.hutool.core.convert.Convert;
import cn.hutool.core.io.IoUtil;
import cn.hutool.core.text.csv.CsvReadConfig;
import cn.hutool.core.text.csv.CsvReader;
import cn.hutool.core.text.csv.CsvRow;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.io.ClassPathResource;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.stream.Collectors;

public class GcsCsvUtils {
	private static final Logger logger = LoggerFactory.getLogger(GcsCsvUtils.class);

	private static List<String> NULL_STRS = ListUtil.list(false, "n/a", "N/A");

	public static Charset UTF_8_CHARSET = StandardCharsets.UTF_8;
	public static Charset GB2312_CHARSET = Charset.forName("GB2312");

	public static List<Map<String, String>> readClassPathResourceCsv(String headerRecognizeZh, String resourcePath){
		return readClassPathResourceCsv(headerRecognizeZh, new HashMap<>(), resourcePath, false);
	}
	public static List<Map<String, String>> readClassPathResourceCsv(String headerRecognizeZh, Map<String, String> headerZh2EnMap, String resourcePath){
		return readClassPathResourceCsv(headerRecognizeZh, headerZh2EnMap, resourcePath, false);
	}
	public static List<Map<String, String>> readClassPathResourceCsv(String headerRecognizeZh, Map<String, String> headerZh2EnMap, String resourcePath, Boolean debug){
		return readClassPathResourceCsv(headerRecognizeZh, headerZh2EnMap, resourcePath, UTF_8_CHARSET, debug);
	}
	public static List<Map<String, String>> readClassPathResourceCsv(String headerRecognizeZh, Map<String, String> headerZh2EnMap, String resourcePath, Charset charset, Boolean debug){
		ClassPathResource csvResource = new ClassPathResource(resourcePath);
		InputStream inputStream = null;
		InputStreamReader inputStreamReader = null;
		CsvReader csvReader = null;
		// 存储所有行的Map数据
		List<Map<String, String>> dataList = new ArrayList<>();
		try {
			inputStream = csvResource.getInputStream();
			inputStreamReader = new InputStreamReader(inputStream, charset);
			CsvReadConfig config = CsvReadConfig.defaultConfig();
			// config.setSkipHeader(true); // 忽略首行
			// config.setSeparator('\t'); // 自定义分隔符（默认逗号）
			csvReader = new CsvReader(inputStreamReader, config);
			// 第一步：读取表头行
			Iterator<CsvRow> iterator = csvReader.iterator();
			List<String> headerList = null;
			while (iterator.hasNext()) {
				CsvRow headerRow = iterator.next();
				headerList = headerRow.getRawList().stream().map(obj -> Convert.toStr(obj).trim()).collect(Collectors.toList());
				if(headerList.contains(headerRecognizeZh)){
					logger.info("读取到表头：{}", headerList);
					break;
				}
			}
			// 第二步：读取数据行
			if (headerList != null && !headerList.isEmpty()) {
				while (iterator.hasNext()) {
					CsvRow csvRow = iterator.next();
					Map<String, String> rowMap = new HashMap<>();
					// 遍历表头的每一列，匹配中英文映射并赋值
					for (int i = 0; i < headerList.size(); i++) {
						String zhHeader = headerList.get(i).trim();
						String enKey = headerZh2EnMap.get(zhHeader);
						if (enKey != null && i < csvRow.size()) {
							String value = csvRow.get(i).trim();
							if(!NULL_STRS.contains(value)){
								rowMap.put(enKey, value);
							}
						} else if(i < csvRow.size()){
							String value = csvRow.get(i).trim();
							if(!NULL_STRS.contains(value)){
								rowMap.put(zhHeader, value);
							}
							logger.warn("警告：表头[{}]无对应英文映射", zhHeader);
						}
					}
					dataList.add(rowMap);
					// 打印测试：输出当前行的Map数据
					if(Convert.toBool(debug, false)){
						logger.info("");
						logger.info("行号：{}", csvRow.getOriginalLineNumber());
						logger.info("数据：{}", rowMap);
						System.out.println("------------------------");
					}
				}
			}
			// 后续可对dataList进行业务处理
			logger.info("CSV读取完成，共获取{}行数据", dataList.size());
		} catch (Exception e) {
			logger.error("读取CSV文件失败：", e);
		} finally {
			IoUtil.close(csvReader);
			IoUtil.close(inputStreamReader);
			IoUtil.close(inputStream);
		}
		return dataList;
	}
}
