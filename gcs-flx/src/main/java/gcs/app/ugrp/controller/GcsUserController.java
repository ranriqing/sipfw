package gcs.app.ugrp.controller;

import cn.hutool.core.convert.Convert;
import cn.hutool.core.lang.Assert;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.baomidou.mybatisplus.extension.toolkit.Db;
import gcs.app.ugrp.beans.ResetPasswordForm;
import gcs.app.ugrp.domain.GcsRole;
import gcs.app.ugrp.domain.GcsUser;
import gcs.app.ugrp.domain.WechatUser;
import gcs.app.ugrp.mapper.GcsUserMapper;
import gcs.app.ugrp.service.GcsRasaService;
import gcs.base.BaseController;
import gcs.modules.log.annotation.GLog;
import gcs.base.core.GcsHolder;
import gcs.base.core.bean.GcsResult;
import gcs.base.core.condition.GcsCondition;
import gcs.base.core.spi.UserCcProfile;
import gcs.base.core.spi.UserCcProfileService;
import gcs.base.core.validators.ValidatorUtils;
import gcs.base.shiro.PasswordHashAdapter;
import gcs.base.tools.GcsBindingUtils;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang.RandomStringUtils;
import org.apache.shiro.authz.annotation.RequiresPermissions;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Slf4j
@RestController
@RequestMapping("/gcsUser")
public class GcsUserController extends ServiceImpl<GcsUserMapper, GcsUser> {

	@Autowired
	private GcsRasaService gcsRasaService;

	@Autowired
	private PasswordHashAdapter passwordHashAdapter;

	@RequestMapping(value = "/index", method = {RequestMethod.GET, RequestMethod.POST})
	@RequiresPermissions("gcsUser:index")
	public GcsResult index(@RequestBody(required = false) Optional<GcsCondition> payload){
		GcsCondition gcsCondition = payload.orElse(new GcsCondition());
		IPage<GcsUser> page = this.page(
			gcsCondition.buildQueryPage(),
			gcsRasaService.query(
				gcsCondition.buildQueryWrapper(GcsUser.class),
				GcsUser.class.getSimpleName(), new HashMap<>()
			)
		);
		IPage<Map<String, Object>> result = page.convert(GcsUserController::_entityMap);
		_putCcFields(result.getRecords());
		return GcsResult.successResult().wrapPage(result);
	}

	@GetMapping("/show/{id}")
	@RequiresPermissions("gcsUser:show")
	public GcsResult show(@PathVariable("id") Long id){
		GcsUser entity = this.getById(id);
		if(entity == null){
			return GcsResult.notExistResult().put("id", id);
		}
		return GcsResult.successResult().putAll(_entityMapWithCcFields(entity));
	}

	@GLog("保存用户")
	@PostMapping("/save")
	@RequiresPermissions("gcsUser:save")
	@Transactional(rollbackFor = Exception.class)
	public GcsResult save1(@RequestBody Map<String, Object> jsonMap){
		GcsUser entity = GcsBindingUtils.bind(jsonMap, GcsUser.class);
		entity.setSalt(RandomStringUtils.randomAlphanumeric(20));
		entity.setIsActive(true);
		ValidatorUtils.validateObj(entity);
		this.save(entity);
		// 工号等档案字段（表单里的 ccJobNum/ccJobPwd/选线/外线），与用户同一事务
		_writeCcProfile(entity.getId(), jsonMap);
		return GcsResult.successResult().putAll(_entityMapWithCcFields(entity));
	}

	@GLog("更新用户")
	@PutMapping("/update/{id}")
	@RequiresPermissions("gcsUser:update")
	@Transactional(rollbackFor = Exception.class)
	public GcsResult update(@PathVariable("id") Long id, @RequestBody Map<String, Object> jsonMap){
		GcsUser entity = this.getById(id);
		if(entity == null){
			return GcsResult.notExistResult().put("id", id);
		}
		GcsBindingUtils.bind(jsonMap, entity);
		ValidatorUtils.validateObj(entity);
		this.updateById(entity);
		_writeCcProfile(id, jsonMap);
		return GcsResult.successResult().putAll(_entityMapWithCcFields(entity));
	}

	@GLog("删除用户")
	@DeleteMapping("/delete/{id}")
	@RequiresPermissions("gcsUser:delete")
	public GcsResult delete(@PathVariable("id") Long id){
		Boolean isSuccess = this.removeById(id);
		if(!isSuccess){
			return GcsResult.notExistResult().put("id", id);
		}
		return GcsResult.successResult().put("id", id);
	}

	/** ----- 业务方法 ----- */
	@GLog("更新密码")
	@PutMapping("/updatePassword")
	public GcsResult updatePassword(@RequestBody ResetPasswordForm form){
		Long entityId = form.getUserId();
		String newPassword = form.getNewPassword();
		Assert.notBlank(newPassword, "新密码不为能空");
		GcsUser entity = this.getById(form.getUserId());
		if(entity == null){
			return GcsResult.notExistResult().put("id", entityId);
		}
		// 更新密码
		String passwordHash = passwordHashAdapter.hash(newPassword, null);
		// 防御：避免写入被截断的奇数长度hex（会导致 Shiro Hex.decode 报错）
		if(passwordHash != null && (passwordHash.length() % 2 != 0)){
			throw new IllegalStateException("password_hash 长度为奇数，可能被数据库截断，请检查 gcs_user.password_hash 字段长度");
		}
		GcsUser updateBean = new GcsUser();
		updateBean.setId(entity.getId());
		updateBean.setPasswordHash(passwordHash);
		updateBean.setIsUnsafe(true);                 // 标记需要改密码
		updateBean.setAuthcCheckDate(new java.util.Date()); // 使旧 token 失效（如有）
		updateBean.setRemark(form.getRemark());       // 同时更新备注
		this.updateById(updateBean);
		return GcsResult.successResult().putAll(_entityMapWithCcFields(entity));
	}

	/** ----- 工具方法 ----- */
	public static Map<String, Object> _entityMap(GcsUser entity){
		GcsRole gcsRole = Db.getById(entity.getGcsRoleId(), GcsRole.class);
		// GcsGroup gcsGroup = Db.getById(entity.getGcsGroupId(), GcsGroup.class);
		String wxOpenId = entity.getWxOpenId();
		Map<String, Object> entityMap = BaseController._entityMap(entity);
		entityMap.put("gcsRoleCaption", gcsRole!=null?gcsRole.getName():null);
		// entityMap.put("gcsGroupCaption", gcsGroup!=null?gcsGroup.getName():null);
		if(wxOpenId!=null){
			WechatUser wechatUser = Db.getOne(Wrappers.lambdaQuery(WechatUser.class).eq(WechatUser::getWxOpenId, wxOpenId));
			if(wechatUser!=null){
				entityMap.put("wxUserId", wechatUser.getWxUnionId());
				entityMap.put("wxNickname", wechatUser.getWxNickname());
				entityMap.put("wxHeadImgUrl", wechatUser.getWxHeadImgUrl());
				entityMap.put("wxSubscribe", wechatUser.getWxSubscribe());
				entityMap.put("wxSubscribeTime", wechatUser.getWxSubscribeTime());
			}
		}
		return entityMap;
	}

	/** {@link #_entityMap(GcsUser)} + 呼叫中心档案字段（工号等）。 */
	private static Map<String, Object> _entityMapWithCcFields(GcsUser entity){
		Map<String, Object> entityMap = _entityMap(entity);
		_putCcFields(Collections.singletonList(entityMap));
		return entityMap;
	}

	/**
	 * 把随用户表单提交的呼叫中心档案字段（工号 / 工号密码 / 选线 / 外线）转交给呼叫中心写入。
	 *
	 * <p>软查找而不是构造注入：呼叫中心未启用时跳过，用户照常保存 —— 与
	 * {@code ProfileController} 读分机状态同一约定。但取到 Bean 后校验失败（工号已被他人占用）
	 * 必须抛出，让整次保存回滚：工号是话机「工号+密码」登录的取单人依据
	 * （{@code dialplan/auth_user.lua} 按 {@code cc_job_num} 查 userid），重复会串号。</p>
	 *
	 * <p>表单没带任何 cc* 字段时（非呼叫中心环境）不做任何调用，已有档案不受影响。</p>
	 */
	private static void _writeCcProfile(Long userId, Map<String, Object> jsonMap) {
		UserCcProfile profile = UserCcProfile.fromJsonMap(jsonMap);
		if (profile.isEmpty()) {
			return;
		}
		UserCcProfileService service = _ccProfileService();
		if (service == null) {
			return;
		}
		service.write(userId, profile);
	}

	/**
	 * 给用户数据行补上呼叫中心档案字段（四个键恒出现，无档案时值为 null）。
	 *
	 * <p>回显是必须的：用户编辑弹窗会把这些字段连同其余字段一起提交，缺了回显就会在保存时
	 * 把已有工号/密码清空。</p>
	 */
	private static void _putCcFields(List<Map<String, Object>> rows) {
		if (rows == null || rows.isEmpty()) {
			return;
		}
		UserCcProfileService service = _ccProfileService();
		Map<Long, Map<String, Object>> fieldsMap = Collections.emptyMap();
		if (service != null) {
			List<Long> userIds = new ArrayList<>(rows.size());
			for (Map<String, Object> row : rows) {
				userIds.add(Convert.toLong(row.get("id")));
			}
			fieldsMap = service.displayFields(userIds);
		}
		for (Map<String, Object> row : rows) {
			Map<String, Object> fields = fieldsMap.get(Convert.toLong(row.get("id")));
			row.putAll(fields == null ? UserCcProfile.emptyDisplayFields() : fields);
		}
	}

	/**
	 * 软查找呼叫中心提供的档案读写实现；未启用（或调用出错）时返回 null，调用方按「不涉及」处理。
	 */
	private static UserCcProfileService _ccProfileService() {
		try {
			Map<String, UserCcProfileService> providers = GcsHolder.getBeansOfType(UserCcProfileService.class);
			if (providers == null || providers.isEmpty()) {
				return null;
			}
			return providers.values().iterator().next();
		} catch (Exception e) {
			log.debug("呼叫中心档案服务不可用（忽略）: {}", e.getMessage());
			return null;
		}
	}
}
