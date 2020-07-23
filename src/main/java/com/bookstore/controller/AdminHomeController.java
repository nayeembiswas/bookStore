package com.bookstore.controller;

import java.security.Principal;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.UUID;

import javax.servlet.http.HttpServletRequest;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;

import com.bookstore.domain.User;
import com.bookstore.domain.UserShipping;
import com.bookstore.domain.security.PasswordResetToken;
import com.bookstore.domain.security.Role;
import com.bookstore.domain.security.UserRole;
import com.bookstore.service.UserService;
import com.bookstore.service.impl.UserSecurityService;
import com.bookstore.utility.MailConstructor;
import com.bookstore.utility.SecurityUtility;
import com.bookstore.utility.USConstants;

@Controller
public class AdminHomeController {
	
	@Autowired
    private JavaMailSender mailSender;

    @Autowired
    private MailConstructor mailConstructor;

    @Autowired
    private UserService userService;
    
    @Autowired
    private UserSecurityService userSecurityService;


//    @RequestMapping("/**/")
//    @ResponseBody
//    public String index() {
//        return "Page Not Found";
//    }

	 @RequestMapping("/adminhome")
	    public String home() {
	        return "adminhome";
	    }
	 
	 @RequestMapping("/login")
	    public String login(Model model) {
	        model.addAttribute("classActiveLogin", true);
	        return "myAccount";
	    }

	 

	  @RequestMapping(value = "/newUser", method = RequestMethod.POST)
	    public String newUserPost(
	            HttpServletRequest request,
	            @ModelAttribute("email") String userEmail,
	            @ModelAttribute("username") String username,
	            Model model
	    ) throws Exception {
	        model.addAttribute("classActiveNewAccount", true);
	        model.addAttribute("email", userEmail);
	        model.addAttribute("username", username);

	        if(userService.findByUsername(username) != null) {
	            model.addAttribute("usernameExists", true);

	            return "myAccount";
	        }

	        if(userService.findByEmail(userEmail) != null) {
	            model.addAttribute("emailExists", true);

	            return "myAccount";
	        }

	        User user = new User();
	        user.setUsername(username);
	        user.setEmail(userEmail);

	        String password = SecurityUtility.randomPassword();

	        String encryptedPassword = SecurityUtility.passwordEncoder().encode(password);
	        user.setPassword(encryptedPassword);

	        Role role = new Role();
	        role.setRoleId(1);
	        role.setName("ROLE_USER");
	        Set<UserRole> userRoles = new HashSet<>();
	        userRoles.add(new UserRole(user, role));
	        userService.createUser(user, userRoles);

	        String token = UUID.randomUUID().toString();
	        userService.createPasswordResetTokenForUser(user, token);

	        String appUrl = "http://"+request.getServerName()+":"+request.getServerPort()+request.getContextPath();


	        SimpleMailMessage email = mailConstructor.constructResetTokenEmail(appUrl, request.getLocale(), token, user, password);

	        mailSender.send(email);

	        model.addAttribute("emailSent", "true");

	        model.addAttribute("orderList", user.getOrderList());

	        return "myAccount";
	    }

	    @RequestMapping("/newUser")
	    public String newUser(Locale locale, @RequestParam("token") String token,Model model) {
	    	
	    	
	        PasswordResetToken passToken = userService.getPasswordResetToken(token);

	        if (passToken == null) {
	            String message = "Invalid Token.";
	            model.addAttribute("message", message);
	            return "redirect:/badRequest";
	        }

	        User user = passToken.getUser();
	        String username = user.getUsername();

	        UserDetails userDetails = userSecurityService.loadUserByUsername(username);

	        Authentication authentication = new UsernamePasswordAuthenticationToken(userDetails, userDetails.getPassword(), userDetails.getAuthorities());

	        SecurityContextHolder.getContext().setAuthentication(authentication);

	        model.addAttribute("user", user);

	        model.addAttribute("classActiveEdit", true);

	        return "myProfile";
	    }
   
	    @RequestMapping("/forgetPassword")
	    public String forgetPassword(
	            HttpServletRequest request,
	            @ModelAttribute("email") String email,
	            Model model
	    ) {
	        model.addAttribute("classActiveForgetPassword", true);

	        User user = userService.findByEmail(email);

	        if(user == null) {
	            model.addAttribute("emailNotExist", true);
	            return "myAccount";
	        }

	        String password = SecurityUtility.randomPassword();

	        String encryptedPassword = SecurityUtility.passwordEncoder().encode(password);
	        user.setPassword(encryptedPassword);

	        userService.save(user);

	        String token = UUID.randomUUID().toString();
	        userService.createPasswordResetTokenForUser(user, token);

	        String appUrl = "http://"+request.getServerName()+":"+request.getServerPort()+request.getContextPath();


	        SimpleMailMessage newEmail = mailConstructor.constructResetTokenEmail(appUrl, request.getLocale(), token, user, password);

	        mailSender.send(newEmail);

	        model.addAttribute("forgetPasswordEmailSent", "true");

	        return "myAccount";
	    }
   
	    
	    @RequestMapping("/myAdmin")
	    public String myProfile(Model model, Principal principal) {
	        User user = userService.findByUsername(principal.getName());
	        model.addAttribute("user", user);
	        model.addAttribute("userPaymentList", user.getUserPaymentList());
	        model.addAttribute("userShippingList", user.getUserShippingList());
	        model.addAttribute("orderList", user.getOrderList());

	        UserShipping userShipping = new UserShipping();
	        model.addAttribute("userShipping", userShipping);

	        model.addAttribute("listOfCreditCards", true);
	        model.addAttribute("listOfShippingAddresses", true);

	        List<String> stateList = USConstants.listOfUSStatesCode;
	        Collections.sort(stateList);
	        model.addAttribute("stateList", stateList);
	        model.addAttribute("classActiveEdit", true);

	        return "myProfile";
	    }
   
   
   
   


//    @RequestMapping("/login")
//    public String login() {
//        return "login";
//    }
}
