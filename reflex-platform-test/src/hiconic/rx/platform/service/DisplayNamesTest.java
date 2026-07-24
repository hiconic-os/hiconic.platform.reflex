package hiconic.rx.platform.service;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.Test;

import hiconic.rx.module.api.util.DisplayNames;

public class DisplayNamesTest {

	@Test
	public void derivesDisplayNamesFromTechnicalNames() {
		assertThat(DisplayNames.fromTechnicalName("fooBarX")).isEqualTo("Foo Bar X");
		assertThat(DisplayNames.fromTechnicalName("fix-fox-fancy")).isEqualTo("Fix Fox Fancy");
		assertThat(DisplayNames.fromTechnicalName("fooBar-FunFact")).isEqualTo("Foo Bar Fun Fact");
	}
}
