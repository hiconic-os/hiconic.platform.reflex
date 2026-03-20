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
package hiconic.rx.platform.wire.space;

import com.braintribe.codec.marshaller.api.Marshaller;
import com.braintribe.codec.marshaller.bin.Bin2Marshaller;
import com.braintribe.codec.marshaller.common.BasicConfigurableMarshallerRegistry;
import com.braintribe.codec.marshaller.jse.JseMarshaller;
import com.braintribe.codec.marshaller.json.JsonStreamMarshaller;
import com.braintribe.codec.marshaller.stax.StaxMarshaller;
import com.braintribe.codec.marshaller.yaml.YamlMarshaller;
import com.braintribe.wire.api.annotation.Managed;

import hiconic.rx.module.api.wire.RxMarshallingContract;

@Managed
public class RxMarshallingSpace implements RxMarshallingContract {

	@Override
	@Managed
	public BasicConfigurableMarshallerRegistry marshallers() {
		BasicConfigurableMarshallerRegistry bean = new BasicConfigurableMarshallerRegistry();
		bean.registerMarshaller("application/json", jsonMarshaller());
		bean.registerMarshaller("text/yaml", yamlMarshaller());
		bean.registerMarshaller("application/yaml", yamlMarshaller());
		bean.registerMarshaller("gm/jse", jseMarshaller());
		bean.registerMarshaller("gm/xml", xmlMarshaller());
		bean.registerMarshaller("gm/bin", binMarshaller());
		return bean;
	}

	@Managed
	private StaxMarshaller xmlMarshaller() {
		StaxMarshaller bean = new StaxMarshaller();
		return bean;
	}

	@Managed
	private JseMarshaller jseMarshaller() {
		JseMarshaller bean = new JseMarshaller();
		return bean;
	}

	@Override
	@Managed
	public JsonStreamMarshaller jsonMarshaller() {
		JsonStreamMarshaller bean = new JsonStreamMarshaller();
		bean.setUseBufferingDecoder(true);
		return bean;
	}

	@Override
	@Managed
	public YamlMarshaller yamlMarshaller() {
		return new YamlMarshaller();
	}

	@Override
	@Managed
	public Marshaller binMarshaller() {
		Bin2Marshaller bean = new Bin2Marshaller();
		return bean;
	}

}
