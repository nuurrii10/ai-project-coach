package at.fhtw.aiprojectcoach.controller;

import at.fhtw.aiprojectcoach.dto.CreateProjectRequest;
import at.fhtw.aiprojectcoach.dto.ProjectResponse;
import at.fhtw.aiprojectcoach.exception.ResourceNotFoundException;
import at.fhtw.aiprojectcoach.service.ProjectService;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import tools.jackson.databind.ObjectMapper;

import java.time.LocalDateTime;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class ProjectControllerTest {

    private final ProjectService projectService = Mockito.mock(ProjectService.class);
    private final MockMvc mockMvc = MockMvcBuilders.standaloneSetup(new ProjectController(projectService)).build();
    private final ObjectMapper objectMapper = new ObjectMapper();

    private final ProjectResponse campusConnect =
            new ProjectResponse(1L, "Campus Connect", "Study group app", LocalDateTime.now());

    @Test
    void createProjectReturnsCreatedProject() throws Exception {
        when(projectService.createProject(any(CreateProjectRequest.class))).thenReturn(campusConnect);

        mockMvc.perform(post("/api/projects")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                new CreateProjectRequest("Campus Connect", "Study group app"))))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.name").value("Campus Connect"));
    }

    @Test
    void createProjectRejectsBlankName() throws Exception {
        mockMvc.perform(post("/api/projects")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new CreateProjectRequest("  ", null))))
                .andExpect(status().isBadRequest());
    }

    @Test
    void getAllProjectsReturnsList() throws Exception {
        when(projectService.getAllProjects()).thenReturn(List.of(campusConnect));

        mockMvc.perform(get("/api/projects"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].name").value("Campus Connect"));
    }

    @Test
    void getProjectByIdReturnsProject() throws Exception {
        when(projectService.getProjectById(1L)).thenReturn(campusConnect);

        mockMvc.perform(get("/api/projects/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Campus Connect"));
    }

    @Test
    void getProjectByIdReturns404WhenMissing() throws Exception {
        when(projectService.getProjectById(99L)).thenThrow(new ResourceNotFoundException("Project with id 99 not found"));

        mockMvc.perform(get("/api/projects/99"))
                .andExpect(status().isNotFound());
    }
}
