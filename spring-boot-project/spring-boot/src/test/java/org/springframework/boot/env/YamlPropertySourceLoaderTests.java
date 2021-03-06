/*
 * Copyright 2012-2018 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.springframework.boot.env;

import java.util.ArrayList;
import java.util.List;

import org.junit.Test;

import org.springframework.core.env.EnumerablePropertySource;
import org.springframework.core.env.PropertySource;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;
import org.springframework.util.ObjectUtils;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for {@link YamlPropertySourceLoader}.
 *
 * @author Dave Syer
 * @author Phillip Webb
 * @author Andy Wilkinson
 */
public class YamlPropertySourceLoaderTests {

	private YamlPropertySourceLoader loader = new YamlPropertySourceLoader();

	@Test
	public void load() throws Exception {
		ByteArrayResource resource = new ByteArrayResource(
				"foo:\n  bar: spam".getBytes());
		PropertySource<?> source = this.loader.load("resource", resource, null,
				(profile) -> true);
		assertThat(source).isNotNull();
		assertThat(source.getProperty("foo.bar")).isEqualTo("spam");
	}

	@Test
	public void orderedItems() throws Exception {
		StringBuilder yaml = new StringBuilder();
		List<String> expected = new ArrayList<>();
		for (char c = 'a'; c <= 'z'; c++) {
			yaml.append(c + ": value" + c + "\n");
			expected.add(String.valueOf(c));
		}
		ByteArrayResource resource = new ByteArrayResource(yaml.toString().getBytes());
		EnumerablePropertySource<?> source = (EnumerablePropertySource<?>) this.loader
				.load("resource", resource, null, (profile) -> true);
		assertThat(source).isNotNull();
		assertThat(source.getPropertyNames())
				.isEqualTo(expected.toArray(new String[] {}));
	}

	@Test
	public void mergeItems() throws Exception {
		StringBuilder yaml = new StringBuilder();
		yaml.append("foo:\n  bar: spam\n");
		yaml.append("---\n");
		yaml.append("foo:\n  baz: wham\n");
		ByteArrayResource resource = new ByteArrayResource(yaml.toString().getBytes());
		PropertySource<?> source = this.loader.load("resource", resource, null,
				(profile) -> true);
		assertThat(source).isNotNull();
		assertThat(source.getProperty("foo.bar")).isEqualTo("spam");
		assertThat(source.getProperty("foo.baz")).isEqualTo("wham");
	}

	@Test
	public void timestampLikeItemsDoNotBecomeDates() throws Exception {
		ByteArrayResource resource = new ByteArrayResource("foo: 2015-01-28".getBytes());
		PropertySource<?> source = this.loader.load("resource", resource, null,
				(profile) -> true);
		assertThat(source).isNotNull();
		assertThat(source.getProperty("foo")).isEqualTo("2015-01-28");
	}

	@Test
	public void loadOriginAware() throws Exception {
		Resource resource = new ClassPathResource("test-yaml.yml", getClass());
		PropertySource<?> source = this.loader.load("resource", resource, null,
				(profile) -> true);
		EnumerablePropertySource<?> enumerableSource = (EnumerablePropertySource<?>) source;
		for (String name : enumerableSource.getPropertyNames()) {
			System.out.println(name + " = " + enumerableSource.getProperty(name));
		}
	}

	@Test
	public void loadSpecificProfile() throws Exception {
		StringBuilder yaml = new StringBuilder();
		yaml.append("foo:\n  bar: spam\n");
		yaml.append("---\n");
		yaml.append("spring:\n  profiles: foo\n");
		yaml.append("foo:\n  bar: wham\n");
		ByteArrayResource resource = new ByteArrayResource(yaml.toString().getBytes());
		PropertySource<?> source = this.loader.load("resource", resource, "foo",
				(profile) -> true);
		assertThat(source).isNotNull();
		assertThat(source.getProperty("foo.bar")).isEqualTo("wham");
	}

	@Test
	public void loadWithAcceptProfile() throws Exception {
		StringBuilder yaml = new StringBuilder();
		yaml.append("---\n");
		yaml.append("spring:\n  profiles: yay,foo\n");
		yaml.append("foo:\n  bar: bang\n");
		yaml.append("---\n");
		yaml.append("spring:\n  profiles: yay,!foo\n");
		yaml.append("foo:\n  bar: wham\n");
		ByteArrayResource resource = new ByteArrayResource(yaml.toString().getBytes());
		PropertySource<?> source = this.loader.load("resource", resource, "yay",
				(profiles) -> ObjectUtils.containsElement(profiles, "!foo"));
		assertThat(source).isNotNull();
		assertThat(source.getProperty("foo.bar")).isEqualTo("wham");
	}

}
