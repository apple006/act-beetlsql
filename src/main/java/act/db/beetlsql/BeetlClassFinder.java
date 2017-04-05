package act.db.beetlsql;

import act.app.App;
import act.app.DbServiceManager;
import act.app.event.AppEventId;
import act.db.DbService;
import act.db.EntityClassRepository;
import act.util.AnnotatedClassFinder;
import act.util.SubClassFinder;
import org.beetl.sql.core.annotatoin.Table;
import org.beetl.sql.core.mapper.BaseMapper;
import org.osgl.$;
import org.osgl.exception.UnexpectedException;
import org.osgl.util.Generics;

import javax.inject.Inject;
import javax.inject.Singleton;
import javax.persistence.Entity;
import java.lang.reflect.Type;
import java.util.List;

import static act.app.DbServiceManager.dbId;

/**
 * Find classes with annotation `Table`
 */
@Singleton
public class BeetlClassFinder {

    private final EntityClassRepository repo;
    private final App app;

    @Inject
    public BeetlClassFinder(EntityClassRepository repo, App app) {
        this.repo = $.notNull(repo);
        this.app = $.notNull(app);
    }

    @AnnotatedClassFinder(Table.class)
    public void foundEntity(Class<?> modelClass) {
        repo.registerModelClass(modelClass);
    }

    @AnnotatedClassFinder(Entity.class)
    public void foundEntity2(Class<?> modelClass) {
        repo.registerModelClass(modelClass);
    }

    @SubClassFinder(noAbstract = false, callOn = AppEventId.PRE_START)
    public void foundMapper(Class<? extends BaseMapper> mapperClass) {
        DbServiceManager dbServiceManager = app.dbServiceManager();
        Class<?> modelClass = modelClass(mapperClass);
        DbService dbService = dbServiceManager.dbService(dbId(modelClass));
        if (dbService instanceof BeetlSqlService) {
            ((BeetlSqlService) dbService).prepareMapperClass(mapperClass, modelClass);
        } else {
            throw new UnexpectedException("mapper class cannot be landed to a BeetlSqlService");
        }
    }

    static Class<?> modelClass(Class<? extends BaseMapper> mapperClass) {
        List<Type> paramTypes = Generics.typeParamImplementations(mapperClass, BaseMapper.class);
        if (paramTypes.size() != 1) {
            throw new UnexpectedException("Cannot determine parameter type of %s", mapperClass);
        }
        Type type = paramTypes.get(0);
        if (!(type instanceof Class)) {
            throw new UnexpectedException("Cannot determine parameter type of %s", mapperClass);
        }
        return $.cast(type);
    }

}