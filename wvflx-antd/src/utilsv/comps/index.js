import { OTHER } from '@/constants'
import AbstractComp from './AbstractComp';

// 自动导入当前目录下所有.js并创建相应实例
const compInstances = {};
const compClasses = {};
[
  require.context('.', false, /\.js$/)
].forEach(context => {
  context.keys().forEach(key => {
    if (key === './index.js') return; // 排除当前文件

    // 提取文件名作为标识（如 './FlowComp.js' -> 'FlowComp'）
    const fileName = key.split('/').pop().replace(/\.js$/, '');
    const CompClass = context(key).default;

    // 验证导入的是否是AbstractComp的子类
    if (CompClass && CompClass.prototype instanceof AbstractComp) {
      compClasses[fileName] = CompClass;
      compInstances[fileName] = new CompClass();
    }
  });
})

// 组件工厂类，管理单例实例
class CompFactory {
  // 获取所有支持的组件类型
  static getAllSupportedTypes() {
    return Object.values(compClasses).reduce((types, CompClass) => {
      return [...types, ...CompClass.getSupportedTypes()];
    }, []);
  }

  // 判断是否支持某个组件类型
  static isSupportedType(compType) {
    return Object.values(compClasses).some(CompClass =>
      CompClass.getSupportedTypes().includes(compType)
    );
  }

  // 获取所有实例
  static getAllInstances() {
    return { ...compInstances };
  }

  // 根据组件类型获取单例实例
  static getInstanceByType(domainName, compType) {
    compType = compType?compType:OTHER.PART_QUERY
    // 1：优先查找domain精确匹配 + compType匹配的组件
    let [fileName, CompClass] = Object.entries(compClasses).find(([key, cls]) => {
      key || true; return cls.getSupportedDomain() === domainName && cls.getSupportedTypes().includes(compType);
    }) || [];

    // 2：若第一步没找到，查找domain为true + compType匹配的组件
    if (!CompClass) {
      [fileName, CompClass] = Object.entries(compClasses).find(([key, cls]) => {
        key || true; return cls.getSupportedDomain() === true && cls.getSupportedTypes().includes(compType);
      }) || [];
    }

    if (fileName && CompClass) {
      return compInstances[fileName];
    }

    throw new Error(`不支持的组件类型: ${compType}`);
  }
}

export default CompFactory;

