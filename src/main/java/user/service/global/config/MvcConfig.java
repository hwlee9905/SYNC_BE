package user.service.global.config;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;
import user.service.global.filter.PostRequestInterceptor;

@Configuration
public class MvcConfig implements WebMvcConfigurer {

    @Autowired
    private PostRequestInterceptor postRequestInterceptor;

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(postRequestInterceptor);
    }
}
