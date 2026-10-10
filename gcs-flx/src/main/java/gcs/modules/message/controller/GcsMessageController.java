package gcs.modules.message.controller;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import gcs.app.ugrp.service.GcsRasaService;
import gcs.base.BaseController;
import gcs.base.core.bean.GcsResult;
import gcs.base.core.condition.GcsCondition;
import gcs.base.core.validators.ValidatorUtils;
import gcs.base.tools.GcsBindingUtils;
import gcs.modules.log.domain.GcsLog;
import gcs.modules.message.domain.GcsMessage;
import gcs.modules.message.mapper.GcsMessageMapper;
import gcs.modules.message.service.GcsMessageService;
import org.apache.shiro.authz.annotation.RequiresPermissions;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

@RestController
@RequestMapping("/gcsMessage")
public class GcsMessageController extends ServiceImpl<GcsMessageMapper, GcsMessage> {

	@Autowired
	private GcsMessageService gcsMessageService;

	@Autowired
	private GcsRasaService gcsRasaService;


	@RequestMapping(value = "/index", method = {RequestMethod.GET, RequestMethod.POST})
	@RequiresPermissions("gcsMessage:index")
	public GcsResult index(@RequestBody(required = false) Optional<GcsCondition> payload){
		GcsCondition gcsCondition = payload.orElse(new GcsCondition());
		IPage<GcsMessage> page = this.page(
				gcsCondition.buildQueryPage(),
				gcsRasaService.query(
						gcsCondition.buildQueryWrapper(GcsMessage.class),
						GcsMessage.class.getSimpleName(), new HashMap<>()
				)
		);
		page.convert(GcsMessageController::_entityMap);
		return GcsResult.successResult().wrapPage(page);
	}

	@GetMapping("/show/{id}")
	@RequiresPermissions("gcsMessage:show")
	public GcsResult show(@PathVariable("id") Long id){
		GcsMessage entity = this.getById(id);
		if(entity == null){
			return GcsResult.notExistResult().put("id", id);
		}
		return GcsResult.successResult().putAll(_entityMap(entity));
	}

	@PostMapping("/save")
	@RequiresPermissions("gcsMessage:save")
	public GcsResult save1(@RequestBody Map<String, Object> jsonMap){
		GcsMessage entity = GcsBindingUtils.bind(jsonMap, GcsMessage.class);
		ValidatorUtils.validateObj(entity);
		this.save(entity);
		return GcsResult.successResult().putAll(_entityMap(entity));
	}

	@PutMapping("/update/{id}")
	@RequiresPermissions("gcsMessage:update")
	public GcsResult update(@PathVariable("id") Long id, @RequestBody Map<String, Object> jsonMap){
		GcsMessage entity = this.getById(id);
		if(entity == null){
			return GcsResult.notExistResult().put("id", id);
		}
		GcsBindingUtils.bind(jsonMap, entity);
		ValidatorUtils.validateObj(entity);
		this.updateById(entity);
		return GcsResult.successResult().putAll(_entityMap(entity));
	}

	@DeleteMapping("/delete/{id}")
	@RequiresPermissions("gcsMessage:delete")
	public GcsResult delete(@PathVariable("id") Long id){
		Boolean isSuccess = this.removeById(id);
		if(!isSuccess){
			return GcsResult.notExistResult().put("id", id);
		}
		return GcsResult.successResult().put("id", id);
	}

	@PostMapping("/readAll")
	public GcsResult readAll(@RequestBody Map<String, Object> jsonMap){
		int readCount = gcsMessageService.readAll();
		return GcsResult.successResult().singleData(readCount);
	}

	/** ----- 工具方法 ----- */
	public static Map<String, Object> _entityMap(GcsMessage entity){
		return BaseController._entityMap(entity);
	}
}
