package org.owasp.webgoat.path_traversal;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mockito;
import org.owasp.webgoat.plugins.LessonTest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.security.core.token.Sha512DigestUtils;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.Matchers.containsString;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@RunWith(SpringJUnit4ClassRunner.class)
public class ProfileUploadRetrievalTest extends LessonTest {

    @Autowired
    private PathTraversal pathTraversal;

    @Before
    public void setup() {
        Mockito.when(webSession.getCurrentLesson()).thenReturn(pathTraversal);
        this.mockMvc = MockMvcBuilders.webAppContextSetup(this.wac).build();
        Mockito.when(webSession.getUserName()).thenReturn("unit-test");
    }

    @Test
    public void solve() throws Exception {
        //Look at the response
        mockMvc.perform(MockMvcRequestBuilders.get("/PathTraversal/random-picture"))
                .andExpect(status().is(200))
                .andExpect(header().exists("Location"))
                .andExpect(header().string("Location", containsString("?id=")))
                .andExpect(content().contentTypeCompatibleWith(MediaType.IMAGE_JPEG));

        //Browse the directories
        mockMvc.perform(MockMvcRequestBuilders.get("/PathTraversal/random-picture?id=../../"))
                .andExpect(status().is(200))
                .andExpect(content().string(containsString("/path-traversal-secret.jpg")))
                .andExpect(content().contentTypeCompatibleWith(MediaType.IMAGE_JPEG));

        //Retrieve the secret file (note: .jpg is added by the server)
        mockMvc.perform(MockMvcRequestBuilders.get("/PathTraversal/random-picture?id=../../path-traversal-secret"))
                .andExpect(status().is(200))
                .andExpect(content().string("You found it submit the SHA-512 hash of your username as answer"))
                .andExpect(content().contentTypeCompatibleWith(MediaType.IMAGE_JPEG));

        //Post flag
        mockMvc.perform(MockMvcRequestBuilders.post("/PathTraversal/random").param("secret", Sha512DigestUtils.shaHex("unit-test")))
                .andExpect(status().is(200))
                .andExpect(jsonPath("$.assignment", equalTo("ProfileUploadRetrieval")))
                .andExpect(jsonPath("$.lessonCompleted", is(true)));
    }

    @Test
    public void shouldReceiveRandomPicture() throws Exception {
        mockMvc.perform(MockMvcRequestBuilders.get("/PathTraversal/random-picture"))
                .andExpect(status().is(200))
                .andExpect(header().exists("Location"))
                .andExpect(content().contentTypeCompatibleWith(MediaType.IMAGE_JPEG));
    }

    @Test
    public void unknownFileShouldGiveDirectoryContents() throws Exception {
        mockMvc.perform(MockMvcRequestBuilders.get("/PathTraversal/random-picture?id=test"))
                .andExpect(status().is(200))
                .andExpect(content().string(containsString("cats/8.jpg")))
                .andExpect(content().contentTypeCompatibleWith(MediaType.IMAGE_JPEG));
    }
}