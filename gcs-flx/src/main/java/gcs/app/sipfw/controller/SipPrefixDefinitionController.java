package gcs.app.sipfw.controller;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import gcs.app.sipfw.domain.SipPrefixDefinition;
import gcs.app.sipfw.mapper.SipPrefixDefinitionMapper;
import gcs.app.sipfw.model.CallDirection;
import gcs.app.sipfw.model.SipDefinitionSnapshot.SipQaFailRecord;
import gcs.app.sipfw.service.SipDefinitionSnapshotService;
import gcs.base.BaseController;
import gcs.base.core.bean.GcsResult;
import gcs.base.core.condition.GcsCondition;
import gcs.base.core.validators.ValidatorUtils;
import gcs.base.tools.GcsBindingUtils;
import gcs.modules.log.annotation.GLog;
import org.apache.shiro.authz.annotation.RequiresPermissions;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;
import java.util.Optional;

@RestController
@RequestMapping("/sipPrefixDefinition")
public class SipPrefixDefinitionController extends ServiceImpl<SipPrefixDefinitionMapper, SipPrefixDefinition> {

    private final SipDefinitionSnapshotService snapshotService;

    public SipPrefixDefinitionController(SipDefinitionSnapshotService snapshotService) {
        this.snapshotService = snapshotService;
    }

    @RequestMapping(value = "/index", method = {RequestMethod.GET, RequestMethod.POST})
    @RequiresPermissions("sipPrefixDefinition:index")
    public GcsResult index(@RequestBody(required = false) Optional<GcsCondition> payload) {
        GcsCondition gcsCondition = payload.orElse(new GcsCondition());
        IPage<SipPrefixDefinition> page = this.page(
                gcsCondition.buildQueryPage(),
                gcsCondition.buildQueryWrapper(SipPrefixDefinition.class)
        );
        page.convert(SipPrefixDefinitionController::_entityMap);
        return GcsResult.successResult().wrapPage(page);
    }

    @GetMapping("/show/{id}")
    @RequiresPermissions("sipPrefixDefinition:show")
    public GcsResult show(@PathVariable("id") Long id) {
        SipPrefixDefinition entity = this.getById(id);
        if (entity == null) {
            return GcsResult.notExistResult().put("id", id);
        }
        return GcsResult.successResult().putAll(_entityMap(entity));
    }

    @PostMapping("/save")
    @RequiresPermissions("sipPrefixDefinition:save")
    public GcsResult save(@RequestBody Map<String, Object> jsonMap) {
        SipPrefixDefinition entity = GcsBindingUtils.bind(jsonMap, SipPrefixDefinition.class);
        ValidatorUtils.validateObj(entity);
        this.save(entity);
        snapshotService.invalidateAndReload();
        return GcsResult.successResult().putAll(_entityMap(entity));
    }

    @PutMapping("/update/{id}")
    @RequiresPermissions("sipPrefixDefinition:update")
    public GcsResult update(@PathVariable("id") Long id, @RequestBody Map<String, Object> jsonMap) {
        SipPrefixDefinition entity = this.getById(id);
        if (entity == null) {
            return GcsResult.notExistResult().put("id", id);
        }
        GcsBindingUtils.bind(jsonMap, entity);
        ValidatorUtils.validateObj(entity);
        this.updateById(entity);
        snapshotService.invalidateAndReload();
        return GcsResult.successResult().putAll(_entityMap(entity));
    }

    @DeleteMapping("/delete/{id}")
    @RequiresPermissions("sipPrefixDefinition:delete")
    public GcsResult delete(@PathVariable("id") Long id) {
        boolean isSuccess = this.removeById(id);
        if (!isSuccess) {
            return GcsResult.notExistResult().put("id", id);
        }
        snapshotService.invalidateAndReload();
        return GcsResult.successResult().put("id", id);
    }

    @GetMapping("/qaFails")
    public GcsResult qaFails(String caller, String direction, String ip) {
        CallDirection callDirection = CallDirection.valueOf(direction != null ? direction : "INBOUND");
        List<SipQaFailRecord> records = snapshotService.getQaFailRecords(caller, callDirection, ip);
        return GcsResult.successResult().put("records", records);
    }

    @DeleteMapping("/qaFails")
    public GcsResult deleteQaFail(String caller, String direction, String ip, int index) {
        CallDirection callDirection = CallDirection.valueOf(direction != null ? direction : "INBOUND");
        boolean ok = snapshotService.deleteQaFailRecord(caller, callDirection, ip, index);
        return ok ? GcsResult.successResult() : GcsResult.notExistResult();
    }

    @DeleteMapping("/qaFails/all")
    public GcsResult clearQaFails(String caller, String direction, String ip) {
        CallDirection callDirection = CallDirection.valueOf(direction != null ? direction : "INBOUND");
        snapshotService.clearQaFailRecords(caller, callDirection, ip);
        return GcsResult.successResult();
    }

    public static Map<String, Object> _entityMap(SipPrefixDefinition entity) {
        return BaseController._entityMap(entity);
    }
}
