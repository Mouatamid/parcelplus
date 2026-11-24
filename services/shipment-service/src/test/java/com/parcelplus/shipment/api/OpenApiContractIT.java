package com.parcelplus.shipment.api;

import com.parcelplus.shipment.TestcontainersConfiguration;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.test.web.servlet.MockMvc;
import org.yaml.snakeyaml.DumperOptions;
import org.yaml.snakeyaml.LoaderOptions;
import org.yaml.snakeyaml.Yaml;
import org.yaml.snakeyaml.constructor.SafeConstructor;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Contract drift test: the spec springdoc generates from the code must equal the committed
 * contracts/openapi/shipment-service.yaml. After an intentional API change, regenerate the file with
 * ./mvnw verify -Dopenapi.update=true -Dit.test=OpenApiContractIT
 * then review the diff and commit it together with the code.
 */
@Import(TestcontainersConfiguration.class)
@SpringBootTest
@AutoConfigureMockMvc
class OpenApiContractIT {

    // Failsafe runs with the module directory as working directory.
    private static final Path CONTRACT = Path.of("../../contracts/openapi/shipment-service.yaml");

    @Autowired
    MockMvc mvc;

    @Test
    void generatedSpecMatchesCommittedContract() throws Exception {
        String generatedYaml = mvc.perform(get("/v3/api-docs.yaml"))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString(StandardCharsets.UTF_8);
        Map<String, Object> generated = withoutServers(parse(generatedYaml));

        if (Boolean.getBoolean("openapi.update")) {
            Files.writeString(CONTRACT, dump(generated), StandardCharsets.UTF_8);
            return;
        }

        assertThat(CONTRACT).as("committed contract file").exists();
        Map<String, Object> committed = withoutServers(parse(Files.readString(CONTRACT, StandardCharsets.UTF_8)));

        assertThat(generated)
                .as("generated OpenAPI differs from %s. If the API change is intentional, run: "
                        + "./mvnw verify -Dopenapi.update=true -Dit.test=OpenApiContractIT", CONTRACT)
                .isEqualTo(committed);
    }

    // Compare trees, not text: key order and whitespace do not matter, values and structure do.
    private static Map<String, Object> parse(String yaml) {
        Yaml parser = new Yaml(new SafeConstructor(new LoaderOptions()));
        Map<String, Object> tree = parser.load(yaml);
        return tree;
    }

    // "servers" holds the test's random port, so it is not part of the contract comparison.
    private static Map<String, Object> withoutServers(Map<String, Object> tree) {
        tree.remove("servers");
        return tree;
    }

    private static String dump(Map<String, Object> tree) throws IOException {
        DumperOptions options = new DumperOptions();
        options.setDefaultFlowStyle(DumperOptions.FlowStyle.BLOCK);
        options.setIndent(2);
        options.setIndicatorIndent(0);
        return new Yaml(options).dump(tree);
    }
}
