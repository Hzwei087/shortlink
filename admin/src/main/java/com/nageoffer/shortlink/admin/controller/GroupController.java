package com.nageoffer.shortlink.admin.controller;

import com.nageoffer.shortlink.admin.service.GroupService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class GroupController {
    @Autowired
    private GroupService groupService;
}
