package gcs.app.vm.controller;

import gcs.app.vm.service.GlobalSearchService;
import gcs.base.core.bean.GcsResult;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 全局搜索聚合端点：POST /globalSearch
 */
@RestController
@RequestMapping("/globalSearch")
public class GlobalSearchController {

    @Autowired
    private GlobalSearchService globalSearchService;

    @PostMapping
    public GcsResult search(@RequestBody GlobalSearchParam param) {
        return GcsResult.successResult().put("groups",
                globalSearchService.search(param.getKeyword(), param.getTagNames(), param.getDomainNames()));
    }
}
