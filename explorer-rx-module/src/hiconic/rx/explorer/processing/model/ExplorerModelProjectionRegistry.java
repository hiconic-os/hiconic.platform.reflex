// ============================================================================
// Licensed under the Apache License, Version 2.0 (the "License");
// you may not use this file except in compliance with the License.
// You may obtain a copy of the License at
//
//     http://www.apache.org/licenses/LICENSE-2.0
//
// Unless required by applicable law or agreed to in writing, software
// distributed under the License is distributed on an "AS IS" BASIS,
// WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
// See the License for the specific language governing permissions and
// limitations under the License.
// ============================================================================
package hiconic.rx.explorer.processing.model;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import com.braintribe.model.generic.GenericEntity;
import com.braintribe.model.meta.GmMetaModel;
import com.braintribe.model.meta.data.display.Group;
import com.braintribe.model.meta.data.display.GroupAssignment;
import com.braintribe.model.processing.meta.configuration.ConfigurationModels;
import com.braintribe.model.processing.meta.editor.BasicModelMetaDataEditor;
import com.braintribe.model.processing.meta.editor.ModelMetaDataEditor;

import hiconic.rx.module.api.service.ConfiguredModel;

/**
 * Composes models returned to Explorer with Explorer-only display metadata.
 * <p>
 * The source model remains untouched. Display policy of one client must not become metadata of every consumer of an
 * access model. The transient projection and its enrichment are only exposed through Explorer's model-environment
 * reflection endpoint.
 */
public class ExplorerModelProjectionRegistry {

	private static final String ENRICHMENT_MODEL_NAME = "hiconic.platform.reflex:explorer-model-enrichment";

	private final Map<String, GmMetaModel> projections = new ConcurrentHashMap<>();
	private final GmMetaModel enrichment = buildEnrichment();

	public GmMetaModel acquire(ConfiguredModel source) {
		return projections.computeIfAbsent(source.name(), ignored -> buildProjection(source));
	}

	private GmMetaModel buildProjection(ConfiguredModel source) {
		GmMetaModel sourceModel = source.modelOracle().getGmMetaModel();
		GmMetaModel projection = ConfigurationModels.create(source.name() + "-explorer-projection")
				.addDependency(sourceModel)
				.addDependency(enrichment)
				.get();
		projection.setVersion(sourceModel.getVersion());
		return projection;
	}

	private static GmMetaModel buildEnrichment() {
		GmMetaModel model = ConfigurationModels.create(ENRICHMENT_MODEL_NAME)
				.addDependency(GenericEntity.T.getModel())
				.get();
		ModelMetaDataEditor editor = new BasicModelMetaDataEditor(model);

		Group technical = Group.T.create();
		technical.setGlobalId("group:Technical");
		technical.setName("Technical");
		technical.setCollapsed(true);

		editor.onEntityType(GenericEntity.T).addPropertyMetaData(GenericEntity.id, technicalAssignment(technical));
		editor.onEntityType(GenericEntity.T).addPropertyMetaData(GenericEntity.globalId, technicalAssignment(technical));
		editor.onEntityType(GenericEntity.T).addPropertyMetaData(GenericEntity.partition, technicalAssignment(technical));

		return model;
	}

	private static GroupAssignment technicalAssignment(Group technical) {
		GroupAssignment assignment = GroupAssignment.T.create();
		assignment.setGroup(technical);
		assignment.setImportant(true);
		assignment.setConflictPriority(1d);
		return assignment;
	}
}
