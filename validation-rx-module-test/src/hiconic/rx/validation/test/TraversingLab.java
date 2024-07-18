package hiconic.rx.validation.test;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.util.List;

import com.braintribe.model.generic.path.api.IListItemModelPathElement;
import com.braintribe.model.generic.path.api.IMapKeyModelPathElement;
import com.braintribe.model.generic.path.api.IMapValueModelPathElement;
import com.braintribe.model.generic.path.api.IModelPathElement;
import com.braintribe.model.generic.path.api.IPropertyModelPathElement;
import com.braintribe.model.generic.path.api.ISetItemModelPathElement;
import com.braintribe.model.generic.reflection.EntityType;
import com.braintribe.model.generic.reflection.GenericModelType;

import hiconic.rx.demo.model.data.HasPersons;
import hiconic.rx.demo.model.data.Person;

public class TraversingLab {
	public static void main(String[] args) {
		HasPersons hasPersons = HasPersons.T.create();
		
		Person p1 = Person.T.create();
		Person p2 = Person.T.create();
		
		p1.setName("John");
		p1.setLastName("Luck");
		

		p2.setName("Sarah");
		p2.setLastName("Wonder");
		
		hasPersons.getPersons().addAll(List.of(p1, p2));
		
		new EntityScan((e,t,mpe) -> {
			System.out.println(stringify(mpe) + " -> " + e);
		}).visit(hasPersons);
	}
	
	private static String stringify(IModelPathElement mpe) {
		StringBuilder builder = new StringBuilder();
		try {
			stringify(mpe, builder, 0);
		} catch (IOException e) {
			throw new UncheckedIOException(e); 
		}
		return builder.toString();
	}
	
	private static void stringify(IModelPathElement mpe, Appendable appendable, int reversePos) throws IOException {
		IModelPathElement previous = mpe.getPrevious();
		
		if (previous != null) {
			stringify(previous, appendable, reversePos + 1);
		}
		
		switch (mpe.getElementType()) {
		case Root:
		case EntryPoint:
			stringify(mpe, appendable);
			break;
			
		case ListItem:
			IListItemModelPathElement lipe = (IListItemModelPathElement)mpe;
			appendable.append('[');
			appendable.append(String.valueOf(lipe.getIndex()));
			appendable.append(']');
			break;
		case SetItem:
			ISetItemModelPathElement sipe = (ISetItemModelPathElement)mpe;
			appendable.append('{');
			stringify(sipe, appendable);
			appendable.append('}');
			break;
		case MapKey:
			IMapKeyModelPathElement mkpe = (IMapKeyModelPathElement)mpe;
			appendable.append('{');
			stringify(mkpe, appendable);
			appendable.append('}');
			break;
		case MapValue:
			IMapValueModelPathElement mvpe = (IMapValueModelPathElement)mpe;
			appendable.append('[');
			stringify(mvpe, appendable);
			appendable.append(']');
			break;
		case Property:
			IPropertyModelPathElement ppe = (IPropertyModelPathElement)mpe;
			appendable.append('.');
			appendable.append(ppe.getProperty().getName());
			break;
		default:
			break;
		}
	}
	
	private static void stringify(IModelPathElement mpe, Appendable appendable) throws IOException {
		stringify(mpe.getType(), mpe.getValue(), appendable);
	}
	
	private static void stringify(GenericModelType type, Object value, Appendable appendable) throws IOException {
		if (type.isScalar()) {
			appendable.append(String.valueOf(value));
			return;
		}
		
		if (type.isEntity()) {
			var et = (EntityType<?>)type;
			appendable.append(et.getShortName());
		}
	}
}
