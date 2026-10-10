package gcs.modules.others.controller;

import cn.hutool.json.JSONArray;
import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONUtil;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.toolkit.Db;
import gcs.base.core.bean.GcsResult;
import gcs.base.BaseController;
import gcs.modules.others.domain.AddrArea;
import gcs.modules.others.domain.AddrCity;
import gcs.modules.others.domain.AddrProvince;
import gcs.modules.others.mapper.AddrAreaMapper;
import gcs.modules.others.mapper.AddrCityMapper;
import gcs.modules.others.mapper.AddrProvinceMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.ClassPathResource;
import org.springframework.web.bind.annotation.*;

import java.io.File;
import java.io.FileInputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/addr")
public class AddrController extends BaseController {

    private Logger logger = LoggerFactory.getLogger(getClass());

    @Autowired
    private AddrProvinceMapper addrProvinceMapper;
    @Autowired
    private AddrCityMapper addrCityMapper;
    @Autowired
    private AddrAreaMapper addrAreaMapper;

    @RequestMapping(value = "/nodes", method = {RequestMethod.GET, RequestMethod.POST})
    public GcsResult nodes(@RequestParam(required = false) Long id) {
        GcsResult gcsResult = GcsResult.successResult();
        List<Map<String, Object>> rstList = new ArrayList<>();
        if(id == null) {
            // 返回所有省份列表
            List<AddrProvince> nodesList = Db.list(Wrappers.lambdaQuery(AddrProvince.class)
                    .orderByAsc(AddrProvince::getId));
            rstList = nodesList.stream().map(AddrController::_entityMap).collect(Collectors.toList());
        } else {
            // 返回所有城市列表 || 地区列表
            List<?> subNodes = new ArrayList<>();
            // 尝试通过ID获取省份 || 城市
            AddrProvince addrProvince = Db.getById(id, AddrProvince.class);
            AddrCity addrCity = Db.getById(id, AddrCity.class);
            if(addrProvince != null) {
                // 返回省份下所有城市
                subNodes = Db.list(Wrappers.lambdaQuery(AddrCity.class)
                        .eq(AddrCity::getAddrProvinceId, addrProvince.getId())
                        .orderByAsc(AddrCity::getId));
            } else if(addrCity != null) {
                // 返回城市下所有区域
                subNodes = Db.list(Wrappers.lambdaQuery(AddrArea.class)
                        .eq(AddrArea::getAddrCityId, addrCity.getId())
                        .orderByAsc(AddrArea::getId));
            }
            rstList = subNodes.stream().map(AddrController::_entityMap).collect(Collectors.toList());
        }
        return gcsResult.singleData(rstList);
    }

    /**
     * 初始化数据
     * @return
     */
    @RequestMapping(value = "/initNodes", method = {RequestMethod.GET, RequestMethod.POST})
    public GcsResult initNodes() {
        // 检查数据
        Long provinceCount = addrProvinceMapper.selectCount(Wrappers.emptyWrapper());
        if (provinceCount > 1) {
            return GcsResult.successResult("存在数据，不能初始化");
        }

        try {
            // 读取JSON数据文件
            ClassPathResource resource = new ClassPathResource("addrs/all.json");
            File jsonFile = resource.getFile();

            // 读取文件内容
            try (FileInputStream fis = new FileInputStream(jsonFile)) {
                byte[] data = new byte[(int) jsonFile.length()];
                fis.read(data);
                String jsonContent = new String(data, StandardCharsets.UTF_8);

                // 解析JSON数组
                JSONArray jsonArray = JSONUtil.parseArray(jsonContent);

                for (Object obj: jsonArray) {
                    JSONObject item = (JSONObject) obj;
                    Long adcode = item.getLong("adcode");
                    String name = item.getStr("name");
                    Long parent = item.getLong("parent");

                    if (parent != null) { // 处理中国地区数据
                        String adcodeStr = adcode.toString();
                        // 处理省份（编码以0000结尾）
                        if (adcode.toString().endsWith("0000")) {
                            AddrProvince addrProvince = addrProvinceMapper.selectById(adcode);
                            if (addrProvince == null) {
                                addrProvince = new AddrProvince();
                                addrProvince.setId(adcode);
                                addrProvince.setName(name);
                                addrProvinceMapper.insert(addrProvince);
                            }
                        }
                        // 处理城市（编码以00结尾）
                        else if (adcodeStr.endsWith("00")) {
                            AddrProvince addrProvince = addrProvinceMapper.selectById(parent);
                            AddrCity addrCity = new AddrCity();
                            addrCity.setId(adcode);
                            addrCity.setName(name);
                            addrCity.setAddrProvinceId(addrProvince.getId());
                            addrCityMapper.insert(addrCity);
                        }
                        // 处理地区或特殊城市
                        else {
                            String parentStr = parent.toString();
                            if (parentStr.endsWith("0000")) {
                                // 直接隶属于省份的城市
                                AddrProvince addrProvince = addrProvinceMapper.selectById(parent);
                                AddrCity addrCity = new AddrCity();
                                addrCity.setId(adcode);
                                addrCity.setName(name);
                                addrCity.setAddrProvinceId(addrProvince.getId());
                                addrCityMapper.insert(addrCity);
                            } else {
                                // 隶属于城市的地区
                                AddrCity addrCity = addrCityMapper.selectById(parent);
                                AddrArea addrArea = new AddrArea();
                                addrArea.setId(adcode);
                                addrArea.setName(name);
                                addrArea.setAddrCityId(addrCity.getId());
                                addrAreaMapper.insert(addrArea);
                            }
                        }
                    }
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
            return GcsResult.failedResult("数据初始化失败: " + e.getMessage());
        }

        // 确保至少保留一个初始地址（上海市）
        Long finalProvinceCount = addrProvinceMapper.selectCount(Wrappers.emptyWrapper());

        if (finalProvinceCount < 1) {
            AddrProvince shanghai = new AddrProvince();
            shanghai.setId(310000L);
            shanghai.setName("上海市");
            addrProvinceMapper.insert(shanghai);
        }
        return GcsResult.successResult();
    }

    public static Map<String, Object> _entityMap(Object entity) {
        Map<String, Object> result = new HashMap<>();
        if (entity instanceof AddrProvince) {
            AddrProvince province = (AddrProvince) entity;
            result.put("id", province.getId());
            result.put("name", province.getName());
            result.put("seqOrder", province.getSeqOrder());
            result.put("level", 1);
            result.put("type", "province");
        } else if (entity instanceof AddrCity) {
            AddrCity city = (AddrCity) entity;
            result.put("id", city.getId());
            result.put("name", city.getName());
            result.put("seqOrder", city.getSeqOrder());
            result.put("level", 2);
            result.put("type", "city");
            result.put("pId", city.getAddrProvinceId());
        } else if (entity instanceof AddrArea) {
            AddrArea area = (AddrArea) entity;
            result.put("id", area.getId());
            result.put("name", area.getName());
            result.put("seqOrder", area.getSeqOrder());
            result.put("level", 3);
            result.put("type", "area");
            result.put("pId", area.getAddrCityId());
        } else {
            result = BaseController._entityMap(entity);
        }
        return result;
    }
}
