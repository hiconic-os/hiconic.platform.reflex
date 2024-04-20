package hiconic.rx.hibernate.service.api;

import hiconic.rx.hibernate.annotations.PersistenceService;
import hiconic.rx.hibernate.annotations.TransactionMode;

public class AnnotationTest {

	@PersistenceService(TransactionMode.NONE)
	public void foo() {
		
	}
}
