package hiconic.rx.hibernate.annotations;

import java.lang.annotation.Documented;
import java.lang.annotation.Inherited;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Retention(RetentionPolicy.RUNTIME)
@Target(value={java.lang.annotation.ElementType.METHOD})
@Inherited
@Documented
public @interface PersistenceService {
	TransactionMode value();
}
