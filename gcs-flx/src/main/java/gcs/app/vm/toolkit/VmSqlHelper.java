package gcs.app.vm.toolkit;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.core.metadata.TableInfo;
import com.baomidou.mybatisplus.core.metadata.TableInfoHelper;
import com.baomidou.mybatisplus.core.toolkit.Assert;
import com.baomidou.mybatisplus.core.toolkit.ClassUtils;
import com.baomidou.mybatisplus.core.toolkit.ExceptionUtils;
import com.baomidou.mybatisplus.core.toolkit.GlobalConfigUtils;
import com.baomidou.mybatisplus.core.toolkit.support.SFunction;
import com.baomidou.mybatisplus.extension.spi.CompatibleHelper;
import com.baomidou.mybatisplus.extension.toolkit.SqlHelper;
import gcs.app.vm.compiler.DynamicCompiler;
import org.apache.ibatis.session.SqlSession;

import java.util.Optional;

public class VmSqlHelper {

    public static <T, M extends BaseMapper<T>> M getMapper(Class<T> entityClass, SqlSession sqlSession) {
        Assert.notNull(entityClass, "entityClass can't be null!");
        TableInfo tableInfo = Optional.ofNullable(TableInfoHelper.getTableInfo(entityClass)).orElseThrow(() -> ExceptionUtils.mpe("Can not find TableInfo from Class: \"%s\".", entityClass.getName()));
        Class<?> mapperClass = ClassUtils.toClassConfident(tableInfo.getCurrentNamespace(), DynamicCompiler.getClassLoader());
        return (M) tableInfo.getConfiguration().getMapper(mapperClass, sqlSession);
    }

    public static <T, R, M extends BaseMapper<T>> R execute(Class<T> entityClass, SFunction<M, R> sFunction) {
        SqlSession sqlSession = SqlHelper.sqlSession(entityClass);
        try {
            return sFunction.apply(VmSqlHelper.getMapper(entityClass, sqlSession));
        } finally {
            CompatibleHelper.getCompatibleSet().closeSqlSession(sqlSession, GlobalConfigUtils.currentSessionFactory(entityClass));
        }
    }
}
