package hello;

import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.RequestMapping;

import java.util.Calendar;

@RestController
public class HelloController {
    
    @RequestMapping("/")
    public String index() {

        int minute = Calendar.getInstance().get(Calendar.MINUTE);
        if(minute % 2 == 0) {
            return "Greetings from service a, started at " + Application.START;
        } else {
            throw new IllegalAccessError();
        }


    }
    
}
