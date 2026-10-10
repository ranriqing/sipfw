package gcs.app.vm.controller;

import com.baomidou.mybatisplus.core.metadata.IPage;
import gcs.app.vm.domain.VmFieldChange;
import gcs.app.vm.service.VmFieldChangeService;
import gcs.base.core.bean.GcsResult;
import gcs.base.core.condition.GcsCondition;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

@RestController
@RequestMapping("/vmFieldChange")
public class VmFieldChangeController {

    @Autowired
    private VmFieldChangeService vmFieldChangeService;

    @GetMapping("/list")
    public GcsResult list(@RequestParam("domainName") String domainName,
                          @RequestParam("recordId") Long recordId){
        Map<String, Object> data = new HashMap<>();
        data.put("entityList", vmFieldChangeService.listTimeline(domainName, recordId));
        return GcsResult.successResult().putAll(data);
    }

    @RequestMapping(value = "/index", method = {RequestMethod.GET, RequestMethod.POST})
    public GcsResult index(@RequestBody(required = false) Optional<GcsCondition> payload){
        GcsCondition gcsCondition = payload.orElse(new GcsCondition());
        IPage<VmFieldChange> page = vmFieldChangeService.pageIndex(gcsCondition);
        return GcsResult.successResult().wrapPage(page);
    }
}
