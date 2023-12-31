/*
 * Copyright 2012-2023 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package net.nicoll.boot.config.diff.analyzer;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;

import net.nicoll.boot.config.loader.AetherDependencyResolver;
import net.nicoll.boot.config.loader.ConfigurationMetadataLoader;
import net.nicoll.boot.metadata.MetadataUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.springframework.boot.configurationmetadata.ConfigurationMetadataGroup;
import org.springframework.boot.configurationmetadata.ConfigurationMetadataProperty;
import org.springframework.boot.configurationmetadata.ConfigurationMetadataRepository;
import org.springframework.util.ClassUtils;

/**
 * @author Stephane Nicoll
 */
public class EnumDefaultValueAnalyzer {

	private static final String NEW_LINE = System.getProperty("line.separator");

	// @formatter:off
	private static final List<String> EXCLUDES = Arrays.asList(
			"management.influx.metrics.export.api-version", // auto-detected
			"management.metrics.export.ganglia.addressing-mode", // deprecated
			"management.metrics.export.graphite.protocol", // deprecated
			"management.metrics.export.influx.api-version", // deprecated
			"management.metrics.export.influx.consistency", // deprecated
			"management.metrics.export.prometheus.pushgateway.shutdown-operation", // deprecated
			"management.metrics.export.simple.mode", // deprecated
			"management.metrics.export.statsd.flavor", // deprecated
			"management.metrics.export.statsd.protocol", // deprecated
			"management.server.ssl.client-auth", // no default
			"server.forward-headers-strategy",
			"server.ssl.client-auth", // no default
			"server.reactive.session.cookie.same-site",
			"server.servlet.session.cookie.same-site",
			"spring.artemis.mode", // no default
			"spring.banner.image.pixelmode", // removed
			"spring.batch.initialize-schema", // deprecated
			"spring.batch.jdbc.isolation-level-for-create", // auto-detected
			"spring.cache.type", // no default
			"spring.cassandra.request.consistency", // deprecated
			"spring.cassandra.request.serial-consistency", // deprecated
			"spring.config.activate.on-cloud-platform", // no default
			"spring.data.cassandra.request.consistency", // no default
			"spring.data.cassandra.request.serial-consistency", // no default
			"spring.data.couchbase.consistency", // removed
			"spring.data.redis.client-type", // auto-detected
			"spring.datasource.embedded-database-connection", // auto-detected
			"spring.datasource.initialization-mode", // removed
			"spring.gson.field-naming-policy", // no default
			"spring.gson.long-serialization-policy", // no default
			"spring.jackson.default-property-inclusion", // no default
			"spring.jms.listener.acknowledge-mode", // no default
			"spring.jms.template.delivery-mode", // no default
			"spring.jooq.sql-dialect", // no default
			"spring.jpa.database", // no default
			"spring.kafka.listener.ack-mode", // no default
			"spring.main.web-application-type", // no default
			"spring.main.cloud-platform", // no default
			"spring.mvc.locale-resolver", // removed
			"spring.mvc.message-codes-resolver-format", // no default
			"spring.netty.leak-detection", // auto-detected
			"spring.rabbitmq.listener.direct.acknowledge-mode", // no default
			"spring.rabbitmq.listener.simple.acknowledge-mode", // no default
			"spring.rabbitmq.publisher-confirm-type",
			"spring.redis.client-type",
			"spring.rsocket.server.ssl.client-auth"
	);
	// @formatter:on

	private static final Logger logger = LoggerFactory.getLogger(EnumDefaultValueAnalyzer.class);

	public static void main(String[] args) throws Exception {
		ConfigurationMetadataLoader loader = new ConfigurationMetadataLoader(
				AetherDependencyResolver.withAllRepositories());
		ConfigurationMetadataRepository repo = loader.loadRepository("3.0.0-SNAPSHOT");
		List<ConfigurationMetadataGroup> groups = MetadataUtils.sortGroups(repo.getAllGroups().values());
		List<ConfigurationMetadataProperty> matchingProperties = new ArrayList<>();
		List<String> excludes = new ArrayList<>(EXCLUDES);
		for (ConfigurationMetadataGroup group : groups) {
			List<ConfigurationMetadataProperty> properties = MetadataUtils
				.sortProperties(group.getProperties().values());
			for (ConfigurationMetadataProperty property : properties) {
				if (property.getDefaultValue() == null && isEnum(property.getType())) {
					if (excludes.contains(property.getId())) {
						excludes.remove(property.getId());
						System.out.println("Validate that " + property.getId() + " has still no default value.");
					}
					else {
						matchingProperties.add(property);
					}
				}
			}
		}
		matchingProperties.sort(Comparator.comparing(ConfigurationMetadataProperty::getId));
		StringBuilder sb = new StringBuilder();
		if (!excludes.isEmpty()) {
			sb.append(NEW_LINE).append(NEW_LINE);
			sb.append("WARNING: excludes list is not up to date. The following " + "properties no longer exist:")
				.append(NEW_LINE);
			for (String exclude : excludes) {
				sb.append("\t").append(exclude).append(NEW_LINE);
			}
		}
		sb.append(NEW_LINE).append(NEW_LINE);
		if (matchingProperties.isEmpty()) {
			sb.append("All other enums have default values");
		}
		else {
			for (ConfigurationMetadataProperty property : matchingProperties) {
				sb.append("  {").append(NEW_LINE);
				sb.append("    \"name\": \"")
					.append(property.getId())
					.append("\",")
					.append(NEW_LINE)
					.append("    \"defaultValue\": ")
					.append("TODO")
					.append(NEW_LINE)
					.append("  },")
					.append(NEW_LINE);
			}
		}
		System.out.println(sb);

	}

	private static boolean isEnum(String type) {
		if (type == null) {
			return false;
		}
		if (type.startsWith("java.util") || type.startsWith("java.lang")) {
			return false;
		}
		try {
			Class<?> target = ClassUtils.forName(type, EnumDefaultValueAnalyzer.class.getClassLoader());
			return target.isEnum();
		}
		catch (ClassNotFoundException ex) {
			logger.info("Type {} not on classpath", type);
		}
		return false;
	}

}
