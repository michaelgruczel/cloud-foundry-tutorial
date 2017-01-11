package hello;

import com.netflix.hystrix.contrib.javanica.annotation.HystrixCommand;
import org.springframework.cloud.client.loadbalancer.LoadBalanced;
import org.springframework.cloud.netflix.ribbon.RibbonClient;
import org.springframework.context.annotation.Bean;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.RequestMapping;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cloud.client.ServiceInstance;
import org.springframework.cloud.client.discovery.DiscoveryClient;
import org.springframework.web.client.RestTemplate;

@RestController
public class HelloController {
    
    @Autowired
    private DiscoveryClient discoveryClient;

    @LoadBalanced
    @Bean
    RestTemplate restTemplate(){
        return new RestTemplate();
    }

    @Autowired
    RestTemplate restTemplate;

    @RequestMapping("/")
    public String index() {
    
    
        final StringBuilder musicServices = new StringBuilder();
        discoveryClient.getInstances("spring-service-a").forEach((ServiceInstance s) -> {
            musicServices.append("spring-service-a:" + s.getHost() + ":" + s.getPort() + "->" + s.getServiceId()
                    + " at " + s.getUri() + " ");
            //musicServices.append(ToStringBuilder.reflectionToString(s));
        });
        discoveryClient.getInstances("spring-service-b").forEach((ServiceInstance s) -> {
            musicServices.append("spring-service-b:" + s.getHost() + ":" + s.getPort() + "->" + s.getServiceId()
                    + " at " + s.getUri() + " ");
            //musicServices.append(ToStringBuilder.reflectionToString(s));
        });
        return "Greetings from Spring Boot to " + musicServices.toString();
    }

    @RequestMapping("/lb-test")
    public String loadBalancingTest() {

        final StringBuilder infos = new StringBuilder();
        // the rest template works together with the service discovery
        ResponseEntity<String> infoEntity = this.restTemplate.exchange(
                    "http://spring-service-a/",
                    HttpMethod.GET,
                    null,
                    String.class,
                    (Object) "mstine"
        );
        infos.append(infoEntity.getBody());

        return "Greetings from Spring other services:" + infos.toString();
    }



    @RequestMapping("/circuit-breaker")
    @HystrixCommand(fallbackMethod = "defaultResponse")
    public String circuitBreakerTest() {

        final StringBuilder infos = new StringBuilder();
        // the rest template works together with the service discovery
        ResponseEntity<String> infoEntity = this.restTemplate.exchange(
                "http://spring-service-a/",
                HttpMethod.GET,
                null,
                String.class,
                (Object) "mstine"
        );
        infos.append(infoEntity.getBody());

        return "Greetings from Spring other services:" + infos.toString();
    }

    String defaultResponse() {
        return "They do not respond";
    }
    
}
