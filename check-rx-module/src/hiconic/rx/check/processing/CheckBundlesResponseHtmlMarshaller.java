// ============================================================================
package hiconic.rx.check.processing;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.Reader;
import java.io.UncheckedIOException;
import java.io.Writer;
import java.util.List;

import org.apache.velocity.Template;
import org.apache.velocity.VelocityContext;
import org.apache.velocity.app.VelocityEngine;

import com.braintribe.codec.marshaller.api.CharacterMarshaller;
import com.braintribe.codec.marshaller.api.GmDeserializationOptions;
import com.braintribe.codec.marshaller.api.GmSerializationOptions;
import com.braintribe.codec.marshaller.api.MarshallException;
import com.braintribe.model.service.api.result.Unsatisfied;

import hiconic.rx.check.model.bundle.api.response.CbrAggregatable;
import hiconic.rx.check.model.bundle.api.response.CbrAggregation;
import hiconic.rx.check.model.bundle.api.response.CheckBundlesResponse;

/**
 * This custom marshaller is mapped to custom mime type <code>text/html;spec=check-bundles-response</code> and reacts on incoming
 * {@link CheckBundlesResponse Check Bundle Responses}. Its responsibility is to marshall the incoming response in a styled HTML site.
 * 
 * @author christina.wilpernig
 */
public class CheckBundlesResponseHtmlMarshaller implements CharacterMarshaller {

	private VelocityEngine veloEngine;
	private Template template;

	// MARSHALLING
	@Override
	public void marshall(OutputStream out, Object value, GmSerializationOptions options) throws MarshallException {
		try {
			Writer writer = new OutputStreamWriter(out, "UTF-8");
			marshall(writer, value, options);
			writer.flush();

		} catch (IOException e) {
			throw new UncheckedIOException("Error while marshalling", e);
		}
	}

	@Override
	public void marshall(Writer writer, Object value, GmSerializationOptions options) throws MarshallException {
		if (!(value instanceof CheckBundlesResponse)) {
			marshallNonResponse(writer, value);
			return;
		}

		this.veloEngine = com.braintribe.utils.velocity.VelocityTools.newResourceLoaderVelocityEngine(true);
		this.template = this.veloEngine.getTemplate("hiconic/rx/check/processing/result.html.vm");

		CheckBundlesResponse response = (CheckBundlesResponse) value;

		VelocityContext context = new VelocityContext();
		context.put("response", response);
		context.put("overallStatus", response.getStatus());
		context.put("overallElapsedTime", response.getElapsedTimeInMs());

		List<CbrAggregatable> elements = response.getElements();

		StringBuilder aggregationListStringBuilder = new StringBuilder();
		int aggregationByCount = getAggregationList(aggregationListStringBuilder, elements);
		context.put("aggregationList", aggregationListStringBuilder.toString());

		context.put("aggregatedByCount", aggregationByCount);

		// Statistics
		ResponseStatistics statistic = new ResponseStatistics();
		statistic.createStatistic(elements);

		context.put("checkCount", statistic.getChecks());
		context.put("okCount", statistic.getOkCount());
		context.put("warnCount", statistic.getWarnCount());
		context.put("failCount", statistic.getFailCount());

		CheckBundlesVelocityTools tools = new CheckBundlesVelocityTools();
		context.put("tools", tools);

		template.merge(context, writer);
	}

	private void marshallNonResponse(Writer writer, Object value) {
		if (value instanceof Unsatisfied u) {
			try {
				writer.write(u.getWhy().stringify());

			} catch (IOException e) {
				throw new RuntimeException("", e);
			}

		} else {
			throw new IllegalArgumentException("Unsupported value type. Supported type: " + CheckBundlesResponse.T.getShortName());
		}

	}

	class ResponseStatistics {
		private int checks;
		private int okCount;
		private int warnCount;
		private int failCount;

		public void createStatistic(List<CbrAggregatable> elements) {
			for (CbrAggregatable a : elements) {
				if (a.isResult()) {
					checks++;
					switch (a.getStatus()) {
						case fail:
							failCount++;
							break;
						case ok:
							okCount++;
							break;
						case warn:
							warnCount++;
							break;
						default:
							break;
					}
				} else {
					createStatistic(((CbrAggregation) a).getElements());
				}
			}
		}

		public int getChecks() {
			return checks;
		}

		public int getOkCount() {
			return okCount;
		}

		public int getWarnCount() {
			return warnCount;
		}

		public int getFailCount() {
			return failCount;
		}
	}

	/**
	 * Returns the aggregation order of the elements in a human-readable format, like: <b>node / bundle / weight</b>
	 */
	private static int getAggregationList(StringBuilder res, List<CbrAggregatable> aggregatables) {
		int count = collectKinds(res, aggregatables, 0);
		if (count == 0)
			res.append("No aggregation defined");

		return count;
	}

	private static int collectKinds(StringBuilder res, List<CbrAggregatable> aggregatables, int count) {
		for (CbrAggregatable e : aggregatables) {
			if (e instanceof CbrAggregation) {
				if (res.length() > 0)
					res.append(" / ");

				CbrAggregation aggregation = (CbrAggregation) e;
				res.append(aggregation.getKind());
				List<CbrAggregatable> elements = aggregation.getElements();

				return collectKinds(res, elements, ++count);
			} else {
				break;
			}
		}
		return count;
	}

	// Unsupported
	@Override
	public Object unmarshall(InputStream in) throws MarshallException {
		throw new UnsupportedOperationException("Unmarshalling is not supported.");
	}

	@Override
	public Object unmarshall(InputStream in, GmDeserializationOptions options) throws MarshallException {
		throw new UnsupportedOperationException("Unmarshalling is not supported.");
	}

	@Override
	public Object unmarshall(Reader reader, GmDeserializationOptions options) throws MarshallException {
		throw new UnsupportedOperationException("Unmarshalling is not supported.");
	}
}
