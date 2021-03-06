package hello;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;


@EnableDiscoveryClient
@SpringBootApplication
public class Application {

    public static long START = System.currentTimeMillis();

    public static void main(String[] args) {
        SpringApplication.run(Application.class, args);
    }


}
