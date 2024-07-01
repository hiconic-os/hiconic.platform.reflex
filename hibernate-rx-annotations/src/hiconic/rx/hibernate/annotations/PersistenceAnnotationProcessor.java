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
package hiconic.rx.hibernate.annotations;

import java.util.List;
import java.util.Set;

import javax.annotation.processing.AbstractProcessor;
import javax.annotation.processing.RoundEnvironment;
import javax.annotation.processing.SupportedAnnotationTypes;
import javax.annotation.processing.SupportedSourceVersion;
import javax.lang.model.SourceVersion;
import javax.lang.model.element.Element;
import javax.lang.model.element.ElementKind;
import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.TypeElement;
import javax.lang.model.element.VariableElement;
import javax.lang.model.type.TypeMirror;
import javax.lang.model.util.Elements;
import javax.lang.model.util.Types;
import javax.tools.Diagnostic;

@SupportedAnnotationTypes("hiconic.rx.hibernate.annotations.PersistenceService")
@SupportedSourceVersion(SourceVersion.RELEASE_8)
public class PersistenceAnnotationProcessor extends AbstractProcessor {
	
	private static final String CLASS_NAME_SERVICE_REQUEST = "com.braintribe.model.service.api.ServiceRequest";
	private static final String CLASS_NAME_SESSION = "org.hibernate.Session";
	private static final String CLASS_NAME_PERSISTENCE_CONTEXT = "hiconic.rx.hibernate.service.api.PersistenceContext";
	private static final String CLASS_NAME_MAYBE = "com.braintribe.gm.model.reason.Maybe";

	@Override
	public boolean process(Set<? extends TypeElement> annotations, RoundEnvironment roundEnv) {
		Elements elementUtils = processingEnv.getElementUtils();
		Types typeUtils = processingEnv.getTypeUtils();
		
		TypeElement contextType = elementUtils.getTypeElement(CLASS_NAME_PERSISTENCE_CONTEXT);
		TypeElement sessionType = elementUtils.getTypeElement(CLASS_NAME_SESSION);
		TypeElement serviceRequestType = elementUtils.getTypeElement(CLASS_NAME_SERVICE_REQUEST);
		TypeElement maybeType = elementUtils.getTypeElement(CLASS_NAME_MAYBE);
		
		for (Element elem : roundEnv.getElementsAnnotatedWith(PersistenceService.class)) {
            if (elem.getKind() == ElementKind.METHOD) {
                ExecutableElement method = (ExecutableElement) elem;
                // Check the return type and parameter types
                
                List<? extends VariableElement> parameters = method.getParameters();

                if (parameters.size() != 3) {
                	processingEnv.getMessager().printMessage(Diagnostic.Kind.ERROR,
                            "Method does not match required signature (" + CLASS_NAME_PERSISTENCE_CONTEXT + ", " + CLASS_NAME_SESSION + ", ? extends " + CLASS_NAME_SERVICE_REQUEST+") Maybe<?>", elem);
                }
                
                VariableElement contextArg = parameters.get(0);
                VariableElement sessionArg = parameters.get(1);
                VariableElement requestArg = parameters.get(2);
                TypeMirror returnType = method.getReturnType();
                
                if (!typeUtils.isSameType(contextArg.asType(), contextType.asType()))
                		processingEnv.getMessager().printMessage(Diagnostic.Kind.ERROR,
                    			"First argument must be of exact type " + CLASS_NAME_PERSISTENCE_CONTEXT, contextArg);
                		
                if (!typeUtils.isSameType(sessionArg.asType(), sessionType.asType()))
                	processingEnv.getMessager().printMessage(Diagnostic.Kind.ERROR,
                			"Second argument must be of exact type " + CLASS_NAME_SESSION, sessionArg);

                if (!typeUtils.isSubtype(requestArg.asType(), serviceRequestType.asType()))
                	processingEnv.getMessager().printMessage(Diagnostic.Kind.ERROR,
                				"Third argument must be assignable to type " + CLASS_NAME_SERVICE_REQUEST, requestArg);

//                if (!typeUtils.isSameType(returnType, maybeType.asType()))
//                		processingEnv.getMessager().printMessage(Diagnostic.Kind.ERROR,
//                				"Return value must be of type " + CLASS_NAME_MAYBE, elem);
                		
//                if (!method.getReturnType().toString().equals("java.lang.String") ||
//                    method.getParameters().size() != 1 ||
//                    !method.getParameters().get(0).asType().toString().equals("int")) {
//                    processingEnv.getMessager().printMessage(Diagnostic.Kind.ERROR,
//                        "Method does not match required signature", elem);
//                }
            }
        }
		
        return true; // No further processing of this annotation type
	}
}
