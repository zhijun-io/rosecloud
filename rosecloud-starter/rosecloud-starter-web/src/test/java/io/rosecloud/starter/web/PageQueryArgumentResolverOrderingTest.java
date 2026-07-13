package io.rosecloud.starter.web;

import io.rosecloud.common.core.model.PageQuery;
import org.junit.jupiter.api.Test;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class PageQueryArgumentResolverOrderingTest {

    @RestController
    static class SampleController {
        volatile PageQuery received;

        @GetMapping("/p")
        public String p(PageQuery q) {
            received = q;
            return "ok";
        }
    }

    @Test
    void resolverWinsOverDefaultModelAttributeBinding() throws Exception {
        SampleController controller = new SampleController();
        MockMvc mockMvc = MockMvcBuilders.standaloneSetup(controller)
                .setCustomArgumentResolvers(new PageQueryArgumentResolver())
                .build();

        mockMvc.perform(get("/p").param("page", "3").param("size", "7")
                        .param("keyword", "k").param("sort", "createTime:DESC"))
                .andExpect(status().isOk());

        PageQuery q = controller.received;
        assertThat(q).isNotNull();
        assertThat(q.getPage()).isEqualTo(3);
        assertThat(q.getSize()).isEqualTo(7);
        assertThat(q.getKeyword()).isEqualTo("k");
        assertThat(q.getSorts()).hasSize(1);
        assertThat(q.getSorts().get(0).property()).isEqualTo("createTime");
        assertThat(q.getSorts().get(0).direction().name()).isEqualTo("DESC");
    }
}
