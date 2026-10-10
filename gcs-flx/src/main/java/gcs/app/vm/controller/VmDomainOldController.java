package gcs.app.vm.controller;

import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/vm/{domainName}")
public class VmDomainOldController extends VmDomainController{
}
