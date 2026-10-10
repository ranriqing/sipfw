package gcs.base.core;

import gcs.base.YmlConfig;
import org.springframework.beans.BeansException;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.stereotype.Component;

import java.util.Map;

@Component
public class GcsHolder implements ApplicationContextAware {
	public static ApplicationContext applicationContext;
	private static YmlConfig _ymlConfig;

	@Override
	public void setApplicationContext(ApplicationContext applicationContext) throws BeansException {
		GcsHolder.applicationContext = applicationContext;
	}

	public static YmlConfig getYmlConfig(){
		if(_ymlConfig==null){
			_ymlConfig = applicationContext.getBean(YmlConfig.class);
		}
		return _ymlConfig;
	}

	public static boolean containsBean(String name) {
		return applicationContext.containsBean(name);
	}

	public static boolean isSingleton(String name) {
		return applicationContext.isSingleton(name);
	}

	public static Object getBean(String name) {
		return applicationContext.getBean(name);
	}

	public static <T> T getBean(Class<T> requiredType) {
		return applicationContext.getBean(requiredType);
	}

	public static <T> T getBean(String name, Class<T> requiredType) {
		return applicationContext.getBean(name, requiredType);
	}

	public static <T> Map<String, T> getBeansOfType(Class<T> requiredType) {
		return applicationContext.getBeansOfType(requiredType);
	}

	public static Class<? extends Object> getType(String name) {
		return applicationContext.getType(name);
	}

}