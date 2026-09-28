// ============================================================================
// Copyright BRAINTRIBE TECHNOLOGY GMBH, Austria, 2002-2022
//
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
package hiconic.rx.model.browser.processing;

import java.io.IOException;
import java.io.InputStream;
import java.io.PrintWriter;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.Predicate;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import com.braintribe.common.lcd.Pair;
import com.braintribe.model.bvd.time.Now;
import com.braintribe.model.generic.value.EnumReference;
import com.braintribe.model.meta.GmCustomType;
import com.braintribe.model.meta.GmEntityType;
import com.braintribe.model.meta.GmEnumConstant;
import com.braintribe.model.meta.GmEnumType;
import com.braintribe.model.meta.GmLinearCollectionType;
import com.braintribe.model.meta.GmListType;
import com.braintribe.model.meta.GmMapType;
import com.braintribe.model.meta.GmMetaModel;
import com.braintribe.model.meta.GmProperty;
import com.braintribe.model.meta.GmSetType;
import com.braintribe.model.meta.GmType;
import com.braintribe.model.meta.info.GmPropertyInfo;
import com.braintribe.model.processing.meta.oracle.EntityTypeOracle;
import com.braintribe.model.processing.meta.oracle.EnumTypeOracle;
import com.braintribe.model.processing.meta.oracle.ModelOracle;
import com.braintribe.model.processing.meta.oracle.PropertyOracle;
import com.braintribe.model.processing.meta.oracle.TypeOracle;
import com.braintribe.utils.lcd.ReflectionTools;
import com.braintribe.utils.lcd.StringTools;

import hiconic.rx.module.api.service.ConfiguredModel;
import hiconic.rx.module.api.service.ConfiguredModels;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

/**
 *
 * @author Dirk Scheffler
 *
 */

public class ModelBrowserServlet extends HttpServlet {

	private ConfiguredModels configuredModels;

	public void setConfiguredModels(ConfiguredModels configuredModels) {
		this.configuredModels = configuredModels;
	}

	@Override
	protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {

		String model = req.getParameter("model");

		String typeSignature = req.getParameter("type");

		String search = Optional.ofNullable(req.getParameter("search")).orElse("");

		resp.setContentType("text/html");
		resp.setCharacterEncoding("UTF-8");

		PrintWriter writer = resp.getWriter();
		writer.println("<!doctype html>");
		writer.println("<html>");
		writer.println("<head>");
		writer.println("<meta charset='UTF-8'>");
		writer.println("<title>Model Browser</title>");
		writer.println("<link href='https://fonts.googleapis.com/css2?family=Roboto&display=swap' rel='stylesheet'>");
		writer.println("<style>");
		writer.println(styles());
		writer.println("</style>");
		writer.println("</head>");
		writer.println("<body>");

		ModelContextualBrowsing contextualBrowsing = new ModelContextualBrowsing();

		Map<String, GmMetaModel> availableModels = new LinkedHashMap<>();
		configuredModels.list().stream() //
				.map(ConfiguredModel::modelOracle) //
				.map(ModelOracle::getGmMetaModel) //
				.forEach(modelRoot -> collectModels(modelRoot, availableModels));
		contextualBrowsing.models = new ArrayList<>(availableModels.values());
		contextualBrowsing.writer = writer;
		contextualBrowsing.search = buildCamelCaseExpansionFilter(search);
		contextualBrowsing.searchText = search;

		if (model != null) {
			ConfiguredModel configuredModel = configuredModels.byName(model);
			if (configuredModel == null) {
				resp.sendError(HttpServletResponse.SC_NOT_FOUND, "No configured model found with name '" + model + "'.");
				return;
			}
			ModelOracle modelOracle = configuredModel.modelOracle();
			contextualBrowsing.model = modelOracle.getGmMetaModel();
			contextualBrowsing.modelOracle = modelOracle;

			if (typeSignature != null && !typeSignature.isEmpty()) {
				TypeOracle typeOracle = modelOracle.findTypeOracle(typeSignature);

				if (typeOracle instanceof EnumTypeOracle) {
					EnumTypeOracle enumTypeOracle = (EnumTypeOracle) typeOracle;
					contextualBrowsing.enumTypeOracle = enumTypeOracle;
					contextualBrowsing.enumType = enumTypeOracle.asGmEnumType();
				} else if (typeOracle instanceof EntityTypeOracle) {
					EntityTypeOracle entityTypeOracle = (EntityTypeOracle) typeOracle;
					contextualBrowsing.entityTypeOracle = entityTypeOracle;
					contextualBrowsing.entityType = entityTypeOracle.asGmEntityType();
				}
			}
		}

		contextualBrowsing.render();
		writer.println("</body>");
		writer.println("</html>");
	}

	private String styles() throws IOException {
		try (InputStream in = ModelBrowserServlet.class.getResourceAsStream("/model-browser/styles.css")) {
			if (in == null)
				throw new IOException("Model Browser stylesheet '/model-browser/styles.css' is missing from the classpath.");

			return new String(in.readAllBytes(), StandardCharsets.UTF_8);
		}
	}

	private Predicate<String> buildCamelCaseExpansionFilter(String search) {
		String lowerCasedSearch = search.toLowerCase();

		if (lowerCasedSearch.equals(search)) {
			StringBuilder builder = new StringBuilder();
			builder.append(".*");
			builder.append(Pattern.quote(search));
			builder.append(".*");

			Pattern pattern = Pattern.compile(builder.toString());
			return s -> pattern.matcher(s.toLowerCase()).find();
		} else {
			List<String> parts = StringTools.splitCamelCase(search);
			StringBuilder builder1 = new StringBuilder();

			builder1.append(".*");

			builder1.append(parts.stream().map(Pattern::quote).collect(Collectors.joining("[a-z0-9_\\-]*")));

			builder1.append(".*");

			Pattern pattern1 = Pattern.compile(builder1.toString());

			StringBuilder builder2 = new StringBuilder();

			builder2.append(parts.stream().map(String::toLowerCase).map(Pattern::quote).collect(Collectors.joining("[^-]*-")));

			builder2.append(".*");

			Pattern pattern2 = Pattern.compile(builder2.toString());

			return s -> pattern1.matcher(s).find() || pattern2.matcher(s).find();
		}
	}

	private static void collectModels(GmMetaModel model, Map<String, GmMetaModel> models) {
		if (models.putIfAbsent(model.getName(), model) != null)
			return;

		model.getDependencies().forEach(dependency -> collectModels(dependency, models));
	}

	private class ModelContextualBrowsing {
		private GmEnumType enumType;
		private GmMetaModel model;
		private List<GmMetaModel> models;
		private ModelOracle modelOracle;
		private EntityTypeOracle entityTypeOracle;
		private GmEntityType entityType;
		private EnumTypeOracle enumTypeOracle;
		private PrintWriter writer;
		private Predicate<String> search;
		private String searchText;

		private void renderModel() throws IOException {
			Pair<String, String> splitModelName = splitModelName(model.getName());
			String name = splitModelName.first();
			String groupId = splitModelName.second();

			writer.println("<table class='header'");
			writer.print("<tr>");
			writer.print("<td class='property-name main-subject'>");
			writer.print("Model");
			writer.print("</td>");
			writer.print("<td class='main-subject'>");
			writer.print(name);
			writer.print("</td>");
			writer.print("</tr>");
			writer.print("<tr>");
			writer.print("<td class='property-name'>");
			writer.print("GroupId");
			writer.print("</td>");
			writer.print("<td>");
			writer.print(groupId);
			writer.print("</td>");
			writer.print("</tr>");

			writer.print("<tr>");
			writer.print("<td class='property-name'>");
			writer.print("Direct Model Dependencies");
			writer.print("</td>");
			writer.print("<td>");
			boolean firstX = true;
			for (GmMetaModel modelDependency : model.getDependencies()) {
				if (firstX)
					firstX = false;
				else
					writer.print(", ");
				renderModel(modelDependency);
			}
			writer.print("</td>");
			writer.print("</tr>");

			writer.print("<tr>");
			writer.print("<td class='property-name'>");
			writer.print("Model Dependencies");
			writer.print("</td>");
			writer.print("<td>");

			firstX = true;

			List<GmMetaModel> modelDependencies = getModelDependencies(model);

			for (GmMetaModel modelDependency : modelDependencies) {
				if (firstX)
					firstX = false;
				else
					writer.print(", ");
				renderModel(modelDependency);
			}
			writer.print("</td>");
			writer.print("</tr>");

			writer.println("</table><br/>");

			writer.println("<form method='GET'>");
			writer.println("<input type='hidden' name='model' value='" + model.getName() + "'/>");
			writer.println("<span class='minor-text'>Filter</span> <input autofocus name='search' value='" + html(searchText) + "'/>");
			writer.println("</form>");

			Predicate<GmCustomType> filter = t -> search.test(ReflectionTools.getSimpleName(t.getTypeSignature()));

			List<GmCustomType> types = modelOracle.getTypes() //
					.onlyDeclared() //
					.asGmTypes() //
					.filter(filter) //
					.sorted(Comparator.comparing(t -> ReflectionTools.getSimpleName(t.getTypeSignature()))) //
					.collect(Collectors.toList());

			writer.println("<table class='property-table'>");
			if (!types.isEmpty()) {

				writer.println("<tr><th>Declared Type</th><th>Namespace</th></tr>");
				for (GmType type : types) {
					String typeSignature = type.getTypeSignature();
					writer.println("<tr>");
					Pair<String, String> nameParts = splitTypeName(typeSignature);
					String namespace = nameParts.second();

					writer.print("<td class='type-col'>");
					renderType(type);
					writer.print("</td>");
					writer.print("<td class='namespace-col'>");
					writer.print(namespace);
					writer.print("</td>");
					writer.println("</tr>");
				}
			}

			List<GmCustomType> inheritedTypes = modelOracle.getTypes() //
					.onlyInherited() //
					.asGmTypes() //
					.filter(filter) //
					.sorted(Comparator.comparing(t -> ReflectionTools.getSimpleName(t.getTypeSignature()))) //
					.collect(Collectors.toList());

			if (!inheritedTypes.isEmpty()) {
				if (!types.isEmpty())
					writer.println("<tr class='spacer-row'></tr>");
				writer.println("<tr><th>Inherited Type</th><th>Namespace</th><th>Declaring Model</th></tr>");
				for (GmType type : inheritedTypes) {
					String typeSignature = type.getTypeSignature();
					writer.println("<tr>");
					Pair<String, String> nameParts = splitTypeName(typeSignature);
					String namespace = nameParts.second();

					writer.print("<td class='type-col'>");
					renderType(type);
					writer.print("</td>");
					writer.print("<td class='namespace-col'>");
					writer.print(namespace);
					writer.print("</td>");
					writer.print("<td class='model-col'>");
					renderModel(type.getDeclaringModel());
					writer.print("</td>");
					writer.println("</tr>");
				}
			}
			writer.println("</table>");
		}

		public void render() throws IOException {
			writer.println("<div>");
			writer.println("<div class='header'>");
			if (model != null) {
				if (entityType != null || enumType != null) {
					writer.print("You are currently browsing model ");
					renderModel(model);
					writer.print(". ");
				}
				writer.print("Pick another <a href='?'>model</a>.");
			} else {
				writer.println("Pick a Model");
			}
			writer.println("</div>");

			writer.println("<div class='view'>");
			if (entityType != null) {
				renderEntityType();
			} else if (enumType != null) {
				renderEnumType();
			} else if (model != null) {
				renderModel();
			} else {
				renderModels();
			}
			writer.println("</div>");
			writer.println("</div>");
		}

		private void renderEnumType() {
			Pair<String, String> splitTypeName = splitTypeName(enumType.getTypeSignature());
			String typeName = splitTypeName.first();
			String namespace = splitTypeName.second();
			String modelName = enumType.getDeclaringModel().getName();
			String contextModelName = model.getName();

			writer.println("<table class='header'");
			writer.print("<tr>");
			writer.print("<td class='property-name main-subject'>");
			writer.print("Enum Type");
			writer.print("</td>");
			writer.print("<td class='main-subject'>");
			writer.print(typeName);
			writer.print("</td>");
			writer.print("</tr>");
			writer.print("<tr>");
			writer.print("<td class='property-name'>");
			writer.print("Namespace");
			writer.print("</td>");
			writer.print("<td>");
			writer.print(namespace);
			writer.print("</td>");
			writer.print("</tr>");

			writer.print("<tr>");
			writer.print("<td class='property-name'>");
			writer.print("Declared By");
			writer.print("</td>");
			writer.print("<td>");
			writer.print("<a href='?model=" + modelName + "'>" + splitModelName(modelName).first() + "</a>");
			writer.print("</td>");
			writer.print("</tr>");

			TypeUsage typeUsage = new TypeUsage(enumType);
			List<GmEntityType> usages = typeUsage.usages;

			writer.print("<tr>");
			writer.print("<td class='property-name'>");
			writer.print("Direct Usages");
			writer.print("</td>");
			writer.print("<td>");
			if (usages.isEmpty()) {
				writer.print("n/a");
			} else {
				boolean firstX = true;
				for (GmType subType : usages) {
					if (firstX)
						firstX = false;
					else
						writer.print(", ");
					renderType(subType);
				}
			}
			writer.print("</td>");
			writer.print("</tr>");

			writer.println("</table><br/>");

			List<GmEnumConstant> constants = enumTypeOracle.getConstants().asGmEnumConstants().collect(Collectors.toList());

			if (!constants.isEmpty()) {
				writer.println("<table class='property-table'>");
				writer.println("<tr><th>Constant</th></tr>");
				for (GmEnumConstant constant : constants) {
					String name = constant.getName();
					writer.print("<tr>");
					writer.print("<td class='property-col'>");
					writer.print(name);
					writer.print("</td>");
					writer.print("</tr>");
				}
				writer.println("</table>");
			}
		}

		private void renderModels() throws IOException {
			Predicate<GmMetaModel> filter = m -> search.test(splitModelName(m.getName()).first());

			List<GmMetaModel> filteredModels = models.stream().filter(filter).distinct()
					.sorted(Comparator.comparing(m -> splitModelName(m.getName()).first()))
					.collect(Collectors.toList());

			writer.println(
					"<form><span class='minor-text'>Filter</span> <input autofocus type='text' name='search' value='" + html(searchText) + "'/></form>");

			writer.println("<table class='property-table'>");
			writer.print("<tr><th>Model</th><th>Group</th></tr>");

			for (GmMetaModel model : filteredModels) {
				writer.print("<tr>");
				String qualifiedName = model.getName();
				Pair<String, String> nameParts = splitModelName(qualifiedName);
				String name = nameParts.first();
				String namespace = nameParts.second();

				writer.print("<td>");
				writer.print("<a href='?model=" + qualifiedName + "'>" + name + "</a>");
				writer.print("</td>");
				writer.print("<td>");
				writer.print(namespace);
				writer.print("</td>");
				writer.println("</tr>");
			}
		}

		private void renderModel(GmMetaModel aModel) {
			writer.print("<a href='");
			writer.print("?model=");
			writer.print(aModel.getName());
			writer.print("'>");
			writer.print(splitModelName(aModel.getName()).first());
			writer.print("</a>");
		}

		private void renderType(GmType type) {
			String typeSignature = type.getTypeSignature();
			switch (type.typeKind()) {
				case ENTITY:
				case ENUM:
					writer.print("<a href='");
					writer.print("?model=");
					writer.print(model.getName());
					writer.print("&type=");
					writer.print(typeSignature);
					writer.print("'>");
					writer.print(ReflectionTools.getSimpleName(typeSignature));
					writer.print("</a>");
					break;
				case LIST:
					writer.print("<span class='collection-type'>list</span> <span class='minor-text'>of</span> ");
					renderType(((GmListType) type).getElementType());
					break;
				case MAP:
					GmMapType mapType = (GmMapType) type;
					writer.print("<span class='collection-type'>map</span> <span class='minor-text'>from</span> ");
					renderType(mapType.getKeyType());
					writer.print(" <span class='minor-text'>to</span> ");
					renderType(mapType.getValueType());
					break;
				case SET:
					writer.print("<span class='collection-type'>set</span> <span class='minor-text'>of</span> ");
					renderType(((GmSetType) type).getElementType());
					break;
				default:
					writer.print("<span class='base-type'>");
					writer.print(typeSignature);
					writer.print("</span>");
					break;

			}
		}

		private class TypeUsage {
			List<GmEntityType> usages = new ArrayList<>();
			GmCustomType type;

			public TypeUsage(GmCustomType type) {
				this.type = type;
				modelOracle.getTypes().onlyEntities().<GmEntityType> asGmTypes().forEach(this::scanType);
				usages.sort(Comparator.comparing(t -> ReflectionTools.getSimpleName(t.getTypeSignature())));
			}

			private void scanType(GmEntityType candidate) {
				if (_scanType(candidate)) {
					usages.add(candidate);
				}
			}

			private boolean _scanType(GmEntityType candidate) {
				if (candidate.getEvaluatesTo() == type)
					return true;

				for (GmProperty property : candidate.getProperties()) {
					GmType propertyType = property.getType();
					if (propertyType == type) {
						return true;
					}

					switch (propertyType.typeKind()) {
						case LIST:
						case SET:
							GmLinearCollectionType collectionType = (GmLinearCollectionType) propertyType;

							if (collectionType.getElementType() == type)
								return true;

							break;
						case MAP:
							GmMapType mapType = (GmMapType) propertyType;

							if (mapType.getKeyType() == type)
								return true;

							if (mapType.getValueType() == type)
								return true;

							break;
						default:
							break;
					}
				}

				return false;
			}
		}

		private void renderEntityType() throws IOException {
			Pair<String, String> splitTypeName = splitTypeName(entityType.getTypeSignature());
			String typeName = splitTypeName.first();
			String namespace = splitTypeName.second();
			String modelName = entityType.getDeclaringModel().getName();
			String contextModelName = model.getName();

			writer.println("<table class='header'");
			writer.print("<tr>");
			writer.print("<td class='property-name main-subject'>");
			writer.print("Entity Type");
			writer.print("</td>");
			writer.print("<td class='main-subject'>");
			writer.print(typeName);
			writer.print("</td>");
			writer.print("</tr>");
			writer.print("<tr>");
			writer.print("<td class='property-name'>");
			writer.print("Namespace");
			writer.print("</td>");
			writer.print("<td>");
			writer.print(namespace);
			writer.print("</td>");
			writer.print("</tr>");

			writer.print("<tr>");
			writer.print("<td class='property-name'>");
			writer.print("Declared By");
			writer.print("</td>");
			writer.print("<td>");
			writer.print("<a href='?model=" + modelName + "'>" + splitModelName(modelName).first() + "</a>");
			writer.print("</td>");
			writer.print("</tr>");

			writer.print("<tr>");
			writer.print("<td class='property-name'>");
			writer.print("Direct Super Types");
			writer.print("</td>");
			writer.print("<td>");
			boolean firstX = true;
			List<GmEntityType> superTypes = entityType.getSuperTypes();
			if (superTypes.isEmpty()) {
				writer.print("n/a");
			} else {
				for (GmEntityType superType : superTypes) {
					if (firstX)
						firstX = false;
					else
						writer.print(", ");
					renderType(superType);
				}
			}

			writer.print("</td>");
			writer.print("</tr>");

			writer.print("<tr>");
			writer.print("<td class='property-name'>");
			writer.print("Super Types");
			writer.print("</td>");
			writer.print("<td>");

			Comparator<GmType> comparator = Comparator.comparing(t -> ReflectionTools.getSimpleName(t.getTypeSignature()));

			firstX = true;
			List<GmType> transitiveSuperTypes = entityTypeOracle.getSuperTypes().transitive().asGmTypes().stream().sorted(comparator)
					.collect(Collectors.toList());

			if (transitiveSuperTypes.isEmpty()) {
				writer.print("n/a");
			} else {
				for (GmType superType : transitiveSuperTypes) {
					if (firstX)
						firstX = false;
					else
						writer.print(", ");
					renderType(superType);
				}
			}

			writer.print("</td>");
			writer.print("</tr>");

			writer.print("<tr>");
			writer.print("<td class='property-name'>");
			writer.print("Sub Types");
			writer.print("</td>");
			writer.print("<td>");

			List<GmType> subTypes = entityTypeOracle.getSubTypes().transitive().asGmTypes().stream().sorted(comparator).collect(Collectors.toList());

			if (subTypes.isEmpty()) {
				writer.print("n/a");
			} else {
				firstX = true;
				for (GmType subType : subTypes) {
					if (firstX)
						firstX = false;
					else
						writer.print(", ");
					renderType(subType);
				}
			}
			writer.print("</td>");
			writer.print("</tr>");

			GmType evaluatesTo = entityTypeOracle.getEvaluatesTo().orElse(null);
			if (evaluatesTo != null) {
				writer.print("<tr>");
				writer.print("<td class='property-name'>");
				writer.print("Evaluates To");
				writer.print("</td>");
				writer.print("<td>");
				renderType(evaluatesTo);
				writer.print("</td>");
				writer.print("</tr>");
			}

			TypeUsage typeUsage = new TypeUsage(entityType);
			List<GmEntityType> usages = typeUsage.usages;

			writer.print("<tr>");
			writer.print("<td class='property-name'>");
			writer.print("Direct Usages");
			writer.print("</td>");
			writer.print("<td>");
			if (usages.isEmpty()) {
				writer.print("n/a");
			} else {
				firstX = true;
				for (GmType subType : usages) {
					if (firstX)
						firstX = false;
					else
						writer.print(", ");
					renderType(subType);
				}
			}
			writer.print("</td>");
			writer.print("</tr>");

			writer.println("</table><br/>");

			writer.println("<form method='GET'>");
			writer.println("<input type='hidden' name='model' value='" + model.getName() + "'/>");
			writer.println("<input type='hidden' name='type' value='" + entityType.getTypeSignature() + "'/>");
			writer.println("<span class='minor-text'>Filter</span> <input autofocus name='search' value='" + html(searchText) + "'/>");
			writer.println("</form>");

			Predicate<PropertyOracle> filter = p -> search.test(p.getName());

			List<PropertyOracle> properties = entityTypeOracle.getProperties() //
					.onlyDeclared() //
					.asPropertyOracles() //
					.filter(filter).sorted(Comparator.comparing(o -> o.asGmProperty().getName())).collect(Collectors.toList());

			writer.println("<table class='property-table'>");
			if (!properties.isEmpty()) {
				writer.println("<tr><th>Declared Property</th><th>Type</th><th>Default</th></tr>");
				for (PropertyOracle property : properties) {
					String name = property.getName();
					GmType type = property.asGmProperty().getType();
					writer.print("<tr>");
					writer.print("<td class='property-col'>");
					writer.print(name);
					writer.print("</td>");

					writer.print("<td class='type-col'>");
					renderType(type);
					writer.print("</td>");

					writer.print("<td>");
					Object initializer = property.getGmPropertyInfos().stream().map(GmPropertyInfo::getInitializer).filter(i -> i != null).findFirst()
							.orElse(null);
					if (initializer != null) {
						final String valueAsStr;
						if (initializer instanceof EnumReference) {
							EnumReference enumReference = (EnumReference) initializer;
							valueAsStr = enumReference.constant().name();
						} else if (initializer instanceof Now) {
							valueAsStr = "now()";
						} else {
							valueAsStr = String.valueOf(initializer);
						}
						writer.print(valueAsStr);
					}
					writer.print("</td>");

					writer.print("</tr>");
				}
			}

			List<PropertyOracle> inheritedProperties = entityTypeOracle.getProperties() //
					.onlyInherited() //
					.asPropertyOracles() //
					.filter(filter) //
					.sorted(Comparator.comparing(o -> o.asGmProperty().getName())) //
					.collect(Collectors.toList());

			if (!inheritedProperties.isEmpty()) {
				if (!properties.isEmpty())
					writer.println("<tr class='spacer-row'></tr>");
				writer.println("<tr><th>Inherited Property</th><th>Type</th><th>Default</th><th>Declared By</th></tr>");
				for (PropertyOracle property : inheritedProperties) {
					String name = property.getName();
					GmType type = property.asGmProperty().getType();
					writer.print("<tr>");
					writer.print("<td class='property-col'>");
					writer.print(name);
					writer.print("</td>");

					writer.print("<td class='type-col'>");
					renderType(type);
					writer.print("</td>");

					writer.print("<td>");
					// Object initializer = property.getGmPropertyInfos().stream().map(GmPropertyInfo::getInitializer).filter(i -> i !=
					// null).findFirst().orElse(null);
					Object initializer = property.asGmProperty().getInitializer();
					if (initializer != null) {
						final String valueAsStr;
						if (initializer instanceof EnumReference) {
							EnumReference enumReference = (EnumReference) initializer;
							valueAsStr = enumReference.constant().name();
						} else if (initializer instanceof Now) {
							valueAsStr = "now()";
						} else {
							valueAsStr = String.valueOf(initializer);
						}
						writer.print(valueAsStr);
					}
					writer.print("</td>");

					writer.print("<td class='type-col'>");
					boolean first = true;
					// List<GmEntityType> declaringTypes = property.getGmPropertyInfos().stream() //
					// .map(i -> (GmEntityType)i.declaringTypeInfo()) //
					// .collect(Collectors.toList());
					List<GmEntityType> declaringTypes = Collections.singletonList(property.asGmProperty().getDeclaringType());

					for (GmEntityType declaringType : declaringTypes) {
						if (first)
							first = false;
						else
							writer.print(", ");

						renderType(declaringType);
					}
					writer.print("</td>");
					writer.print("</tr>");
				}
			}
			writer.println("</table>");
		}

		private List<GmEntityType> determineSuperTypes(GmEntityType contextType) {
			Set<GmEntityType> superTypes = new LinkedHashSet<>();
			collectSuperTypes(superTypes, contextType);
			return superTypes.stream().sorted(Comparator.comparing(t -> ReflectionTools.getSimpleName(t.getTypeSignature())))
					.collect(Collectors.toList());
		}

		private void collectSuperTypes(Set<GmEntityType> superTypes, GmEntityType type) {
			if (!superTypes.add(type))
				return;

			for (GmEntityType superType : type.getSuperTypes()) {
				collectSuperTypes(superTypes, superType);
			}
		}

		private Set<GmCustomType> getAllTypes(GmCustomType customType) {
			Set<GmCustomType> customTypes = new HashSet<>();

			getAllTypes(customTypes, customType, false);

			return customTypes;
		}

		private void getAllTypes(Set<GmCustomType> customTypes, GmCustomType customType, boolean includeSelf) {
			if (includeSelf && !customTypes.add(customType))
				return;

			if (customType instanceof GmEntityType) {
				GmEntityType entityType = (GmEntityType) customType;
				for (GmEntityType superType : entityType.getSuperTypes()) {
					getAllTypes(customTypes, superType, true);
				}
			}
		}

		private List<GmMetaModel> getModelDependencies(GmMetaModel metaModel) {
			Set<GmMetaModel> models = new HashSet<>();

			getModelDependencies(models, metaModel, false);

			return models.stream() //
					.sorted(Comparator.comparing(m -> splitModelName(m.getName()).first())).collect(Collectors.toList());
		}

		private void getModelDependencies(Set<GmMetaModel> models, GmMetaModel metaModel, boolean includeSelf) {
			if (includeSelf && !models.add(metaModel))
				return;

			for (GmMetaModel dep : metaModel.getDependencies()) {
				getModelDependencies(models, dep, true);
			}
		}

	}

	private static Pair<String, String> splitTypeName(String nsName) {
		return splitNamespacedName(nsName, '.');
	}

	private static Pair<String, String> splitModelName(String nsName) {
		return splitNamespacedName(nsName, ':');
	}

	private static Pair<String, String> splitNamespacedName(String nsName, char delimiter) {
		int split = nsName.lastIndexOf(delimiter);
		if (split < 0)
			return Pair.of(nsName, "");

		String namespace = nsName.substring(0, split);
		String name = nsName.substring(split + 1);

		return Pair.of(name, namespace);
	}

	private static String html(String value) {
		return value.replace("&", "&amp;") //
				.replace("<", "&lt;") //
				.replace(">", "&gt;") //
				.replace("\"", "&quot;") //
				.replace("'", "&#39;");
	}

}
