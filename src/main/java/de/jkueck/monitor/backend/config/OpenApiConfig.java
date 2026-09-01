import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI monitorOpenAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("Monitor Backend API")
                        .description("API für den Feuerwehr-Einsatzmonitor")
                        .version("1.2.0"));
    }
}