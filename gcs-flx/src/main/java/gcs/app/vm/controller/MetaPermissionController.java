package gcs.app.vm.controller;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import gcs.app.vm.domain.MetaDomain;
import gcs.app.vm.domain.MetaPermission;
import gcs.app.vm.mapper.MetaPermissionMapper;
import gcs.base.BaseController;
import gcs.base.core.bean.GcsResult;
import gcs.base.core.condition.GcsCondition;
import gcs.base.core.validators.ValidatorUtils;
import gcs.base.tools.GcsBindingUtils;
import gcs.modules.log.annotation.GLog;
import org.apache.shiro.authz.annotation.RequiresPermissions;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

@RestController
@RequestMapping("/metaPermission")
public class MetaPermissionController extends ServiceImpl<MetaPermissionMapper, MetaPermission> {

    @RequestMapping(value = "/index", method = {RequestMethod.GET, RequestMethod.POST})
    @RequiresPermissions("metaPermission:index")
    public GcsResult index(@RequestBody(required = false) Optional<GcsCondition> payload) {
        GcsCondition gcsCondition = payload.orElse(new GcsCondition());
        IPage<MetaPermission> page = this.page(
                gcsCondition.buildQueryPage(),
                gcsCondition.buildQueryWrapper(MetaPermission.class)
        );
        return GcsResult.successResult().wrapPage(page);
    }

    @GetMapping("/show/{id}")
    @RequiresPermissions("metaPermission:show")
    public GcsResult show(@PathVariable("id") Long id) {
        MetaPermission entity = this.getById(id);
        if (entity == null) {
            return GcsResult.notExistResult().put("id", id);
        }
        return GcsResult.successResult().putAll(_entityMap(entity));
    }

    @GLog("保存权限元数据")
    @PostMapping("/save")
    @RequiresPermissions("metaPermission:save")
    public GcsResult save1(@RequestBody java.util.Map<String, Object> jsonMap) {
        MetaPermission entity = GcsBindingUtils.bind(jsonMap, MetaPermission.class);
        ValidatorUtils.validateObj(entity);
        this.save(entity);
        return GcsResult.successResult().putAll(_entityMap(entity));
    }

    @GLog("更新权限元数据")
    @PutMapping("/update/{id}")
    @RequiresPermissions("metaPermission:update")
    public GcsResult update(@PathVariable("id") Long id, @RequestBody java.util.Map<String, Object> jsonMap) {
        MetaPermission entity = this.getById(id);
        if (entity == null) {
            return GcsResult.notExistResult().put("id", id);
        }
        GcsBindingUtils.bind(jsonMap, entity);
        ValidatorUtils.validateObj(entity);
        this.updateById(entity);
        return GcsResult.successResult().putAll(_entityMap(entity));
    }

    @GLog("删除权限元数据")
    @DeleteMapping("/delete/{id}")
    @RequiresPermissions("metaPermission:delete")
    public GcsResult delete(@PathVariable("id") Long id) {
        MetaPermission entity = this.getById(id);
        if (entity == null) {
            return GcsResult.notExistResult().put("id", id);
        }
        boolean ok = this.removeById(id);
        if (!ok) {
            return GcsResult.notExistResult().put("id", id);
        }
        return GcsResult.successResult().put("id", id);
    }

    /** ----- 工具方法 ----- */
    public static Map<String, Object> _entityMap(MetaPermission entity){
        return BaseController._entityMap(entity);
    }
}

