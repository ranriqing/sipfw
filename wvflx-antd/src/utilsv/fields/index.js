import AbstractField from './AbstractField';
import {FIELD} from "@/constants";

// 自动导入当前目录下所有.js并创建相应实例
const fieldInstances = {};
const fieldClasses = {};
[
  require.context('.', false, /\.js$/)
].forEach(context => {
  context.keys().forEach(key => {
    if (key === './index.js') return; // 排除当前文件

    // 提取文件名作为标识（如 './IntegerField.js' -> 'IntegerField'）
    const fileName = key.split('/').pop().replace(/\.js$/, '');
    const FieldClass = context(key).default;

    // 验证导入的是否是AbstractField的子类
    if (FieldClass && FieldClass.prototype instanceof AbstractField) {
      fieldClasses[fileName] = FieldClass;
      fieldInstances[fileName] = new FieldClass();
    }
  });
})

// 字段工厂类，管理单例实例
class FieldFactory {
  // 获取所有支持的字段类型
  static getAllSupportedTypes() {
    return Object.values(fieldClasses).reduce((types, FieldClass) => {
      return [...types, ...FieldClass.getSupportedTypes()];
    }, []);
  }

  // 判断是否支持某个字段类型
  static isSupportedType(fieldType) {
    return Object.values(fieldClasses).some(FieldClass =>
      FieldClass.getSupportedTypes().includes(fieldType)
    );
  }

  // 获取所有实例
  static getAllInstances() {
    return { ...fieldInstances };
  }

  // 根据字段类型获取单例实例
  static getInstanceByType(domainName, fieldType) {
    fieldType = fieldType?fieldType:FIELD.STRING
    domainName = domainName?domainName:'None'

    // 1：优先查找domain精确匹配 + fieldType匹配的组件
    let [fileName, FieldClass] = Object.entries(fieldClasses).find(
      ([key, cls]) => { key || null; return cls.getSupportedDomain() === domainName && cls.getSupportedTypes().includes(fieldType); }
    ) || [];

    // 2：若第一步没找到，查找domain为true + fieldType匹配的组件
    if (!FieldClass) {
      [fileName, FieldClass] = Object.entries(fieldClasses).find(
        ([key, cls]) => { key || null; return cls.getSupportedDomain() === true && cls.getSupportedTypes().includes(fieldType); }
      ) || [];
    }
    if (fileName && FieldClass) {
      return fieldInstances[fileName];
    }
    return fieldInstances["StringField"];
    // throw new Error(`不支持的字段类型: ${fieldType}`);
  }
}

export default FieldFactory;

