package hiconic.rx.platform.loading;

import java.lang.reflect.Field;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.Set;

import com.braintribe.wire.api.annotation.Import;
import com.braintribe.wire.api.annotation.Managed;
import com.braintribe.wire.api.space.WireSpace;

import hiconic.rx.module.api.wire.RxExportContract;
import hiconic.rx.module.api.wire.RxModuleContract;

public interface RxExportContracts {
	static Set<Class<? extends RxExportContracts>> determinedImports(Class<? extends RxModuleContract> rootSpace) {
		Set<Class<? extends RxExportContracts>> exportContracts = new LinkedHashSet<>();
		Set<Class<? extends WireSpace>> visitedSpaces = new HashSet<>();
		
		determinedImports(rootSpace, exportContracts, visitedSpaces);
		
		return exportContracts;
	}
	
	private static void determinedImports(Class<? extends WireSpace> space, Set<Class<? extends RxExportContracts>> contracts, Set<Class<? extends WireSpace>> visitedSpaces) {
		if (!visitedSpaces.add(space))
			return;
		
		if (!space.isAnnotationPresent(Managed.class))
			return;

		for (Field field : space.getFields()) {
			if (field.isAnnotationPresent(Import.class)) {
				Class<?> type = field.getType();
				if (type.isInterface()) {
					if (RxExportContract.class.isAssignableFrom(type)) {
						contracts.add((Class<? extends RxExportContracts>) type);
					}
				}
				else if (type.isAssignableFrom(WireSpace.class)) {
					Class<? extends WireSpace> importedSpaceClass = (Class<? extends WireSpace>) type;
					
					determinedImports(importedSpaceClass, contracts, visitedSpaces);
				}
			}
		}
	}
}
