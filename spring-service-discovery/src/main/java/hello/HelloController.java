package hello;

import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.RequestMapping;

import org.apache.commons.lang.builder.ToStringBuilder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.cloud.client.ServiceInstance;
import org.springframework.cloud.client.discovery.DiscoveryClient;
import org.springframework.cloud.netflix.eureka.EnableEurekaClient;
import org.springframework.cloud.netflix.feign.EnableFeignClients;
import org.springframework.cloud.netflix.feign.FeignClient;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.client.RestTemplate;

@RestController
public class HelloController {
    
    @Autowired
    private DiscoveryClient discoveryClient;
    
    @RequestMapping("/")
    public String index() {
    
    
        final StringBuilder musicServices = new StringBuilder();
        discoveryClient.getInstances("spring-service-a").forEach((ServiceInstance s) -> {
            musicServices.append(ToStringBuilder.reflectionToString(s));
        });
        discoveryClient.getInstances("spring-service-b").forEach((ServiceInstance s) -> {
            musicServices.append(ToStringBuilder.reflectionToString(s));
        });
        
    
    
        return "Greetings from Spring Boot to" + musicServices.toString();
    }
    
}
