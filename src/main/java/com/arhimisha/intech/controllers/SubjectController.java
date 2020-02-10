package com.arhimisha.intech.controllers;

import com.arhimisha.intech.domain.Message;
import com.arhimisha.intech.domain.Subject;
import com.arhimisha.intech.domain.User;
import com.arhimisha.intech.services.MessageService;
import com.arhimisha.intech.services.SubjectService;
import com.arhimisha.intech.services.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.ModelAndView;

import java.util.GregorianCalendar;
import java.util.Optional;

@Controller
@RequestMapping(value = "/subject")
public class SubjectController {

    private final SubjectService subjectService;
    private final UserService userService;
    private final MessageService messageService;

    @Autowired
    public SubjectController(SubjectService subjectService, UserService userService, MessageService messageService) {
        this.subjectService = subjectService;
        this.userService = userService;
        this.messageService = messageService;
    }

    @GetMapping(value = "/{id:^[0-9]+$}")
    public ModelAndView getSubjectAndFirstMessagesPage(
            @PathVariable long id,
            @RequestParam(name = "pageNumber", defaultValue = "0") int pageNumber,
            @AuthenticationPrincipal UserDetails userDetails
    ) {
        final ModelAndView model = new ModelAndView("subject");

        final Optional<Subject> subject = this.subjectService.loadById(id);
        if (subject.isEmpty() || subject.get().isDeleted()) {
            throw new RuntimeException("Subject is not found");
        }
        model.addObject("subject", subject.get());

        final Pageable pageable = PageRequest.of(pageNumber, 10, Sort.by("creationDate").ascending());
        final Page<Message> messagesPage = this.messageService
                .loadAllBySubjectAndDeleted(subject.get(), false, pageable);
        if (0 > pageNumber || (pageNumber!=0 && pageNumber >= messagesPage.getTotalPages())) {
            model.addObject("error", String.format("Page number %d is not exist", pageNumber));
            model.setViewName("error");
            return model;
        }
        model.addObject("messages", messagesPage.getContent());
        model.addObject("currentPage", pageNumber);
        model.addObject("totalPages", messagesPage.getTotalPages());

        final Optional<User> user = this.userService.findUserByUsername(userDetails.getUsername());
        if (user.isEmpty() || !user.get().isEnabled()) {
            throw new RuntimeException("User " + userDetails.getUsername() + " is not exist");
        }
        model.addObject("currentUser", user.get());
        return model;
    }

    @PostMapping(value = "/create")
    public String create(
            @RequestParam(name = "name", required = true) String name,
            @RequestParam(name = "description", defaultValue = "", required = false) String description,
            @AuthenticationPrincipal UserDetails userDetails
    ) {
        if (name.isBlank()) {
            throw new RuntimeException("Subject Name is empty");
        }

        final Optional<User> user = this.userService.findUserByUsername(userDetails.getUsername());
        if (user.isEmpty() || !user.get().isEnabled()) {
            throw new RuntimeException("User " + userDetails.getUsername() + " is not exist");
        }
        Subject subject = new Subject(
                0L,
                name,
                description,
                user.get(),
                new GregorianCalendar(),
                false
        );
        subject = this.subjectService.save(subject);
        return "redirect:/subject/" + subject.getId();
    }
}
