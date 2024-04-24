package hiconic.rx.hibernate.processing;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import org.hibernate.Session;
import org.hibernate.query.Query;

import com.braintribe.gm.model.reason.Maybe;
import com.braintribe.gm.model.reason.Reasons;
import com.braintribe.gm.model.reason.essential.NotFound;
import com.braintribe.model.generic.GenericEntity;
import com.braintribe.model.generic.reflection.EntityType;
import com.braintribe.model.generic.reflection.GenericModelType;
import com.braintribe.model.service.api.ServiceRequest;
import com.braintribe.model.service.api.result.Neutral;

import hiconic.rx.hibernate.service.api.PersistenceContext;
import hiconic.rx.hibernate.service.api.PersistenceServiceProcessor;
import hiconic.rx.hibernate.service.api.QueryProcessorArg;

public class QueryProcessor<P extends ServiceRequest, R> implements PersistenceServiceProcessor<P, R> {

	private final String queryString;
	private final boolean isNative;
	private final boolean update;
	private final boolean resultRequired;
	
	public QueryProcessor(String queryString, QueryProcessorArg... args) {
		Set<QueryProcessorArg> attributes = Set.of(args); 
		
		this.queryString = queryString;
		this.isNative = attributes.contains(QueryProcessorArg.NATIVE);
		this.update = attributes.contains(QueryProcessorArg.UPDATE);
		this.resultRequired = attributes.contains(QueryProcessorArg.RESULT_REQUIRED);
	}

	@Override
	public Maybe<R> process(PersistenceContext context, Session session, P request) {
		Query<?> query = isNative? // 
				session.createNativeQuery(queryString): //
				session.createQuery(queryString);
		
		EntityType<GenericEntity> entityType = request.entityType();
		
		for (String name: query.getParameterMetadata().getNamedParameterNames()) {
			Object value = entityType.getProperty(name).get(request);
			query.setParameter(name, value);
		}
		GenericModelType evaluatesTo = entityType.getEvaluatesTo();

		if (update) {
			int updated = query.executeUpdate();
			
			final Object result;
			
			switch (evaluatesTo.getTypeCode()) {
			case booleanType:
				result = updated > 0;
				break;
			case enumType:
				if (evaluatesTo == Neutral.T)
					result = Neutral.NEUTRAL;
				else
					throw new UnsupportedOperationException("Return type of " + entityType.getTypeSignature() + " is not supported for update statements by StatementProcessor");
				break;
			case integerType:
				result = updated; 
				break;
			case longType:
				result = (long)updated;
				break;
				
			default:
				throw new UnsupportedOperationException("Return type of " + entityType.getTypeSignature() + " is not supported for update statements by StatementProcessor");
			}
			
			return Maybe.complete((R)result);
		}
		else {
			List<?> results = query.getResultList();

			if (resultRequired && results.isEmpty())
				return Reasons.build(NotFound.T).text("No suitable " + evaluatesTo.getTypeSignature() + " found")
						.toMaybe();

			switch (evaluatesTo.getTypeCode()) {

			case setType:
				return Maybe.complete((R) new LinkedHashSet<>(results));

			case listType:
				return Maybe.complete((R) results);

			default:
				switch (results.size()) {
				case 0:
					return Maybe.complete(null);
				case 1:
					return Maybe.complete((R) results.get(0));
				default:
					throw new IllegalStateException("Unique query returned multiple results.");
				}

			}
		}
	}
}
