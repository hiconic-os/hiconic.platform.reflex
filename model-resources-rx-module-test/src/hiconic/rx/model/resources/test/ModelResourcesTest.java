// ============================================================================
// Licensed under the Apache License, Version 2.0
// ============================================================================
package hiconic.rx.model.resources.test;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.InputStream;
import java.nio.charset.StandardCharsets;

import org.junit.Test;

import com.braintribe.model.processing.query.fluent.EntityQueryBuilder;
import com.braintribe.model.processing.session.api.persistence.PersistenceGmSession;
import com.braintribe.model.resource.Resource;
import com.braintribe.model.resource.source.ResourceSource;
import com.braintribe.model.resourceapi.stream.GetBinaryResponse;
import com.braintribe.model.resourceapi.stream.GetResource;

import hiconic.rx.access.module.api.AccessContract;
import hiconic.rx.model.resources.api.ModelResourcesContract;
import hiconic.rx.model.resources.test.wire.contract.ModelResourcesTestContract;
import hiconic.rx.test.common.AbstractRxTest;

public class ModelResourcesTest extends AbstractRxTest {

	@Test
	public void publishesOnlyResourceGraphAndStreamsPackagedPayload() throws Exception {
		Resource reference = resolveExportContract(ModelResourcesTestContract.class).reference();
		assertThat(String.valueOf((Object) reference.getId())).isEqualTo("test.model-resource");
		assertThat(reference.getPartition()).isEqualTo(ModelResourcesContract.ACCESS_ID);
		assertThat(reference.getResourceSource()).isNull();

		AccessContract accesses = resolveExportContract(AccessContract.class);
		PersistenceGmSession session = accesses.systemSessionFactory().newSession(ModelResourcesContract.ACCESS_ID);
		Resource stored = session.query().entity(Resource.T, "test.model-resource").require();
		assertThat(stored.getResourceSource()).isNotNull();
		assertThat(session.query().entities(EntityQueryBuilder.from(Resource.T).done()).list()).hasSize(1);
		assertThat(session.query().entities(EntityQueryBuilder.from(ResourceSource.T).done()).list()).hasSize(1);

		GetResource request = GetResource.T.create();
		request.setDomainId(ModelResourcesContract.ACCESS_ID);
		request.setResource(reference);
		GetBinaryResponse response = request.eval(evaluator).get();
		try (InputStream in = response.getResource().openStream()) {
			assertThat(new String(in.readAllBytes(), StandardCharsets.UTF_8)).isEqualTo("packaged model resource\n");
		}
	}
}
