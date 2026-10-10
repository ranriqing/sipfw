package gcs.modules.log.controller;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import gcs.modules.log.domain.GcsLog;
import gcs.modules.log.mapper.GcsLogMapper;
import gcs.app.ugrp.service.GcsRasaService;
import gcs.base.core.bean.GcsResult;
import gcs.base.core.condition.GcsCondition;
import org.apache.shiro.authz.annotation.RequiresPermissions;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;


@RestController
@RequestMapping("/gcsLog")
public class GcsLogController extends ServiceImpl<GcsLogMapper, GcsLog> {

	@Autowired
	private GcsRasaService gcsRasaService;

	@RequestMapping(value = "/index", method = {RequestMethod.GET, RequestMethod.POST})
	@RequiresPermissions("gcsLog:index")
	public GcsResult index(@RequestBody(required = false) Optional<GcsCondition> payload){
		GcsCondition gcsCondition = payload.orElse(new GcsCondition());
		IPage<GcsLog> page = this.page(
			gcsCondition.buildQueryPage(),
			gcsRasaService.query(
				gcsCondition.buildQueryWrapper(GcsLog.class),
				GcsLog.class.getSimpleName(), new HashMap<>()
			)
		);
		page.convert(GcsLogController::_entityMap);
		return GcsResult.successResult().wrapPage(page);
	}

	/** 工具方法 */
	public static Map<String, Object> _entityMap(GcsLog entity){
		Map<String, Object> entityMap = new HashMap<>();
		entityMap.put("id", entity.getId());
		entityMap.put("operation", entity.getOperation());
		return entityMap;
	}
}
