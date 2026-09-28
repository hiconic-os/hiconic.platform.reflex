// ============================================================================
// Licensed under the Apache License, Version 2.0
// ============================================================================
package hiconic.rx.explorer.processing;

import java.util.List;
import java.util.function.Function;

import com.braintribe.model.folder.Folder;
import com.braintribe.model.generic.GenericEntity;
import com.braintribe.model.generic.i18n.LocalizedString;
import com.braintribe.model.generic.pr.AbsenceInformation;
import com.braintribe.model.generic.reflection.EntityType;
import com.braintribe.model.generic.reflection.Property;
import com.braintribe.model.generic.reflection.StandardCloningContext;
import com.braintribe.model.processing.query.fluent.EntityQueryBuilder;
import com.braintribe.model.processing.session.api.persistence.PersistenceGmSession;
import com.braintribe.model.query.EntityQuery;
import com.braintribe.model.query.Operator;
import com.braintribe.model.query.PropertyOperand;
import com.braintribe.model.query.Restriction;
import com.braintribe.model.query.conditions.ValueComparison;
import com.braintribe.model.resource.AdaptiveIcon;
import com.braintribe.model.resource.Resource;
import com.braintribe.model.resource.source.TransientSource;
import com.braintribe.model.template.Template;
import com.braintribe.model.workbench.KnownWorkenchPerspective;
import com.braintribe.model.workbench.TemplateQueryAction;
import com.braintribe.model.workbench.WorkbenchPerspective;

import hiconic.rx.workbench.api.WorkbenchInitializer;

/** Builds the structured Explorer workbenches for RX system accesses. */
public class SystemWorkbenchInitializer implements WorkbenchInitializer {

	public record Filter(String propertyName, Object value, Operator operator) {}

	public record Entry(String name, String displayName, String entityTypeSignature, String iconName,
			List<Integer> iconSizes, boolean home, Filter filter, List<Entry> children) {
		public Entry(String name, String displayName, String entityTypeSignature) {
			this(name, displayName, entityTypeSignature, null);
		}

		public Entry(String name, String displayName, String entityTypeSignature, String iconName) {
			this(name, displayName, entityTypeSignature, iconName, List.of(16, 24, 32, 64), true, null, List.of());
		}

		public static Entry folder(String name, String displayName, Entry... children) {
			return new Entry(name, displayName, null, null, List.of(), false, null, List.of(children));
		}

		public static Entry queryFolder(String name, String displayName, String entityTypeSignature, Entry... children) {
			return new Entry(name, displayName, entityTypeSignature, null, List.of(), false, null, List.of(children));
		}

		public static Entry query(String name, String displayName, String entityTypeSignature, String iconName,
				List<Integer> iconSizes, boolean home) {
			return new Entry(name, displayName, entityTypeSignature, iconName, iconSizes, home, null, List.of());
		}

		public static Entry filteredQuery(String name, String displayName, String entityTypeSignature, String iconName,
				List<Integer> iconSizes, boolean home, String propertyName, Object value, boolean negated) {
			return new Entry(name, displayName, entityTypeSignature, iconName, iconSizes, home,
					new Filter(propertyName, value, negated ? Operator.notEqual : Operator.equal), List.of());
		}

		public static Entry likeQuery(String name, String displayName, String entityTypeSignature, String iconName,
				List<Integer> iconSizes, boolean home, String propertyName, String pattern) {
			return new Entry(name, displayName, entityTypeSignature, iconName, iconSizes, home,
					new Filter(propertyName, pattern, Operator.like), List.of());
		}
	}

	private final List<Entry> entries;
	private final List<String> obsoleteRootEntries;
	private final Function<String, Resource> resourceResolver;
	private PersistenceGmSession session;

	public SystemWorkbenchInitializer(List<Entry> entries, Function<String, Resource> resourceResolver) {
		this(entries, List.of(), resourceResolver);
	}

	public SystemWorkbenchInitializer(List<Entry> entries, List<String> obsoleteRootEntries, Function<String, Resource> resourceResolver) {
		this.entries = List.copyOf(entries);
		this.obsoleteRootEntries = List.copyOf(obsoleteRootEntries);
		this.resourceResolver = resourceResolver;
	}

	@Override
	public void initialize(PersistenceGmSession session) {
		this.session = session;
		Folder root = rootFolder(session);
		WorkbenchPerspective homePerspective = perspective(session, KnownWorkenchPerspective.homeFolder);
		Folder home = perspectiveRoot(homePerspective, KnownWorkenchPerspective.homeFolder);
		List<Folder> homeFolders = new java.util.ArrayList<>();
		root.getSubFolders().removeIf(folder -> obsoleteRootEntries.contains(folder.getName()));
		for (Entry entry : entries)
			ensureEntry(root, root, entry, homeFolders);

		homePerspective.getFolders().clear();
		homePerspective.getFolders().addAll(homeFolders);
		home.getSubFolders().clear();
		home.getSubFolders().addAll(homeFolders);
	}

	private Folder ensureEntry(Folder root, Folder parent, Entry entry, List<Folder> homeFolders) {
		Folder folder = findDirectChild(parent, entry.name());
		if (folder == null && parent != root)
			folder = findDirectChild(root, entry.name());
		if (folder == null)
			folder = entry.entityTypeSignature() == null ? plainFolder(entry) : queryFolder(entry);

		if (folder.getParent() != parent)
			folder.setParent(parent);
		root.getSubFolders().remove(folder);
		addOnce(parent, folder);
		if (entry.entityTypeSignature() == null)
			folder.setContent(null);
		ensureIcon(folder, entry);

		for (Entry child : entry.children())
			ensureEntry(root, folder, child, homeFolders);
		if (entry.home())
			homeFolders.add(folder);
		return folder;
	}

	private Folder findDirectChild(Folder parent, String name) {
		return parent.getSubFolders().stream().filter(candidate -> name.equals(candidate.getName())).findFirst().orElse(null);
	}

	private Folder plainFolder(Entry entry) {
		return session.create(Folder.T).initFolder(entry.name(), entry.displayName());
	}

	private void ensureIcon(Folder folder, Entry entry) {
		if (entry.iconName() == null || folder.getIcon() != null)
			return;

		AdaptiveIcon icon = session.create(AdaptiveIcon.T);
		icon.setName(entry.displayName() + " Icon");
		for (int size : entry.iconSizes())
			icon.getRepresentations().add(importGraph(resourceResolver.apply(entry.iconName() + "_" + size + "x" + size + ".png")));
		folder.setIcon(icon);
	}

	private <T extends GenericEntity> T importGraph(T detached) {
		return detached.clone(new StandardCloningContext() {
			@Override
			public GenericEntity supplyRawClone(EntityType<? extends GenericEntity> entityType, GenericEntity source) {
				GenericEntity clone = session.create(entityType);
				if (source instanceof TransientSource sourceWithStream && clone instanceof TransientSource cloneWithStream)
					cloneWithStream.setInputStreamProvider(sourceWithStream.getInputStreamProvider());
				return clone;
			}

			@Override
			public boolean canTransferPropertyValue(EntityType<? extends GenericEntity> entityType, Property property,
					GenericEntity source, GenericEntity clone, AbsenceInformation absenceInformation) {
				return !property.isIdentifying();
			}
		});
	}

	private Folder queryFolder(Entry entry) {
		Folder folder = session.create(Folder.T).initFolder(entry.name(), entry.displayName());

		EntityQuery query = session.create(EntityQuery.T);
		query.setEntityTypeSignature(entry.entityTypeSignature());
		if (entry.filter() != null) {
			PropertyOperand property = session.create(PropertyOperand.T);
			property.setPropertyName(entry.filter().propertyName());

			ValueComparison comparison = session.create(ValueComparison.T);
			comparison.setLeftOperand(property);
			comparison.setOperator(entry.filter().operator());
			comparison.setRightOperand(entry.filter().value());

			Restriction restriction = session.create(Restriction.T);
			restriction.setCondition(comparison);
			query.setRestriction(restriction);
		}

		Template template = session.create(Template.T);
		template.setTechnicalName(entry.name() + "-query");
		template.setName(localizedString(session, entry.displayName()));
		template.setPrototype(query);
		template.setPrototypeTypeSignature(EntityQuery.T.getTypeSignature());

		TemplateQueryAction action = session.create(TemplateQueryAction.T);
		action.setDisplayName(localizedString(session, entry.displayName()));
		action.setTemplate(template);
		folder.setContent(action);
		return folder;
	}

	private Folder rootFolder(PersistenceGmSession session) {
		WorkbenchPerspective perspective = perspective(session, KnownWorkenchPerspective.root);
		return perspectiveRoot(perspective, KnownWorkenchPerspective.root);
	}

	private Folder perspectiveRoot(WorkbenchPerspective perspective, KnownWorkenchPerspective name) {
		return perspective.getFolders().stream()
				.filter(folder -> name.toString().equals(folder.getName()))
				.findFirst()
				.orElseThrow(() -> new IllegalStateException("Workbench perspective '" + name + "' has no canonical root folder"));
	}

	private WorkbenchPerspective perspective(PersistenceGmSession session, KnownWorkenchPerspective perspective) {
		return session.query().entities(EntityQueryBuilder.from(WorkbenchPerspective.T)
				.where().property("name").eq(perspective.toString()).done()).unique();
	}

	private LocalizedString localizedString(PersistenceGmSession session, String value) {
		return session.create(LocalizedString.T).putDefault(value);
	}

	private void addOnce(Folder parent, Folder child) {
		if (parent.getSubFolders().stream().noneMatch(folder -> child.getName().equals(folder.getName())))
			parent.getSubFolders().add(child);
	}
}
