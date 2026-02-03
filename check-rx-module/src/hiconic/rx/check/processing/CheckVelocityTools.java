// ============================================================================
package hiconic.rx.check.processing;

import java.io.StringWriter;
import java.util.Arrays;

import com.braintribe.model.time.TimeSpan;
import com.braintribe.model.time.TimeUnit;
import com.braintribe.utils.lcd.Lazy;
import com.braintribe.utils.xml.XmlTools;
import com.vladsch.flexmark.ext.tables.TablesExtension;
import com.vladsch.flexmark.html.HtmlRenderer;
import com.vladsch.flexmark.parser.Parser;
import com.vladsch.flexmark.util.ast.Document;
import com.vladsch.flexmark.util.data.MutableDataHolder;
import com.vladsch.flexmark.util.data.MutableDataSet;

import hiconic.rx.check.model.api.response.CheckResponse;
import hiconic.rx.check.model.api.response.CrAggregatable;
import hiconic.rx.check.model.api.response.CrAggregation;

/**
 * Tooling functions which are called from the check bundle velocity template.
 * 
 * @author christina.wilpernig
 */
public class CheckVelocityTools {

	public static final MutableDataHolder FLEXMARK_OPTIONS = new MutableDataSet() //
			.set(Parser.EXTENSIONS, Arrays.asList(TablesExtension.create()));
	
	private final Lazy<Parser> markdownParser = new Lazy<>(() -> Parser.builder(FLEXMARK_OPTIONS).build());
	private final Lazy<HtmlRenderer> htmlRenderer = new Lazy<>(() -> HtmlRenderer.builder(FLEXMARK_OPTIONS).build());

	public String getIdPrefix(CrAggregation aggregation) {
		switch (aggregation.getKind()) {
			case bundle:
				return "Bundle";
			case coverage:
				return "Coverage";
			case label:
				return "Label";
			case node:
				return "Node";
			case processorName:
				return "Processor Name";
			case status:
				return "Status";
			case latency:
				return "Latency";
			default:
				throw new IllegalStateException("Unknown CheckAggregationKind: " + aggregation.getKind());
		}
	}
	
	public boolean isResponse(Object object) {
		return (object instanceof CheckResponse);
	}
	
	public String getDetailsPreview(String details) {
		return details.length() < 50 ? details : details.substring(0, 50) + "...";
	}
	
	public String getIdentification(CrAggregatable a) {
		return CheckUtils.getIdentification(a);
	}
	
	public String getPrettyElapsedTime(double elapsedTime) {
		TimeSpan ts = TimeSpan.create(elapsedTime, TimeUnit.milliSecond);
		TimeUnit floor = ts.floorUnit();
		
		if (floor == TimeUnit.milliSecond) {
			ts.setValue(Math.round(elapsedTime));
		}
		
		return ts.formatWithFloorUnitAndSubUnit();
	}
	
	public String parseMarkdown(String markdown) {
		Parser parser = markdownParser.get();
		Document document = parser.parse(markdown);
		
		HtmlRenderer renderer = htmlRenderer.get();
		
		StringWriter writer = new StringWriter();
		renderer.render(document, writer);
		
		return writer.toString();
	}
	
	public String escape(String text) {
		return XmlTools.escape(text);
	}
}
