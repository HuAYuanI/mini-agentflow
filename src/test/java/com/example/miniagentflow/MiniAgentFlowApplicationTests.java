package com.example.miniagentflow;

import com.example.miniagentflow.ai.MockModelServiceClient;
import com.example.miniagentflow.ai.ModelServiceClient;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
class MiniAgentFlowApplicationTests {

    @Autowired
    private ModelServiceClient modelServiceClient;

    @Test
    void contextLoadsWithMockModelClient() {
        assertThat(modelServiceClient).isInstanceOf(MockModelServiceClient.class);
    }
}
