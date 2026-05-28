package ai.dochandler.controllers;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;


@Controller
public class SPAController
{
    @RequestMapping(value = {"/{path:[^\\.]*}"})
    public String spa()
    {
        return("forward:/index.html");
    }
}
