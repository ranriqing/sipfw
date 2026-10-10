package gcs.base.tools;

import gcs.app.ugrp.service.GcsShiroService;
import gcs.base.core.GcsHolder;
import gcs.base.shiro.spi.SpiUser;
import org.apache.shiro.SecurityUtils;
import org.apache.shiro.session.Session;
import org.apache.shiro.subject.Subject;

public class GcsShiroUtils {

	private static GcsShiroService _gcsShiroService;

	/** Subject相关 */
	public static boolean isLogin() {
		return SecurityUtils.getSubject().getPrincipal() != null;
	}

	public static Subject getSubject() {
		return SecurityUtils.getSubject();
	}

	public static SpiUser getCurrentUser() {
		return getGcsShiroService().currentUser();
	}

	/** Session相关 */
	public static Session getSession() {
		return SecurityUtils.getSubject().getSession();
	}

	public static Object getSessionAttribute(Object key) {
		return getSession().getAttribute(key);
	}

	public static void setSessionAttribute(Object key, Object value) {
		getSession().setAttribute(key, value);
	}

	// 工具方法
	private static GcsShiroService getGcsShiroService() {
		if(_gcsShiroService == null){
			_gcsShiroService = GcsHolder.getBean(GcsShiroService.class);
		}
		return _gcsShiroService;
	}
}
