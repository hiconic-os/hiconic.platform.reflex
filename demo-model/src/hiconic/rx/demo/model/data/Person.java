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
package hiconic.rx.demo.model.data;

import java.util.Date;

import com.braintribe.model.generic.GenericEntity;
import com.braintribe.model.generic.reflection.EntityType;
import com.braintribe.model.generic.reflection.EntityTypes;

public interface Person extends GenericEntity {
	EntityType<Person> T = EntityTypes.T(Person.class);
	
	String name = "name";
	String lastName = "lastName";
	String birthday = "birthday";
	String email = "email";
	String gender = "gender";
	
	String getName();
	void setName(String name);
	
	String getLastName();
	void setLastName(String lastName);
	
	Date getBirthday();
	void setBirthday(Date birthday);
	
	String getEmail();
	void setEmail(String email);
	
	Gender getGender();
	void setGender(Gender gender);
}
