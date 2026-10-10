package gcs.app;

import cn.hutool.core.convert.Convert;
import cn.hutool.core.lang.Console;
import cn.hutool.core.lang.Dict;
import gcs.base.core.bean.GcsResult;
import gcs.modules.others.controller.AddrController;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;

import java.util.*;


@Controller
public class ApplicationController {

    @Value("${app.version}")
    private String appVersion;

    @Autowired
    private AddrController addrController;

    @ResponseBody
    @RequestMapping(value = {"/", "/application", "/application/index"}, method = { RequestMethod.GET, RequestMethod.POST })
    public GcsResult index(
            @RequestParam Map<String, Object> queryParams,
            @RequestBody(required = false) Map<String, Object> bodyParams) throws Exception {

        /** 合并参数 */
        Dict params = new Dict(queryParams);
        if (bodyParams != null) {
            params.putAll(bodyParams);
        }
        Console.log("请求参数：", params);

        /** 初始化地址 */
        if(Convert.toBool(params.get("initAddr"), false)){
            addrController.initNodes();
        }

        return GcsResult.successResult("Welcome to Gcs!").put("version", appVersion);
    }

    @ResponseBody
    @RequestMapping(value = {"/application/clientInteractive"}, method = { RequestMethod.GET, RequestMethod.POST })
    public GcsResult clientInteractive(
            @RequestParam Map<String, Object> queryParams,
            @RequestBody(required = false) Map<String, Object> bodyParams) throws Exception {
        /** 合并参数 */
        Dict params = new Dict(queryParams);
        if (bodyParams != null) {
            params.putAll(bodyParams);
        }
        // Console.log("请求参数：", params);

        return GcsResult.successResult().put("success", true);
    }
}
