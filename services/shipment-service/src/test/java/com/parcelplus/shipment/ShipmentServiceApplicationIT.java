package com.parcelplus.shipment;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.JdbcTemplate;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@Import(TestcontainersConfiguration.class)
@SpringBootTest
class ShipmentServiceApplicationIT {

	@Autowired
	JdbcTemplate jdbc;

	@Test
	void contextLoads() {
	}

	@Test
	void flywayHistoryContainsSuccessfulV1AndV2() {
		List<String> versions = jdbc.queryForList(
				"SELECT version FROM flyway_schema_history WHERE success = true", String.class);

		assertThat(versions).contains("1", "2");
	}

	@Test
	void outboxHasPartialIndexOnUnpublishedRows() {
		String indexDef = jdbc.queryForObject(
				"SELECT indexdef FROM pg_indexes WHERE indexname = 'idx_outbox_unpublished'", String.class);

		assertThat(indexDef).contains("(created_at)").contains("WHERE (published_at IS NULL)");
	}
}
