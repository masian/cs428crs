package controllers;

import exceptions.NotAuthorizedException;
import models.UserCredentials;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.ResponseStatus;
import service.AuthenticationService;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;

import static org.springframework.web.bind.annotation.RequestMethod.GET;
import static org.springframework.web.bind.annotation.RequestMethod.POST;
/**
 * @author: Nick Humrich
 * @date: 2/20/14
 */
@Controller
@EnableAutoConfiguration
@RequestMapping("/auth")
public class AuthenticationWebController {
	private AuthenticationService service;

	public AuthenticationWebController() {
		service = new AuthenticationService();
	}

	@RequestMapping(value = "/login", method = GET)
	public @ResponseBody
	UserCredentials startLoginProcess(HttpSession session)
	{
		UserCredentials user = service.startLoginProcess();
		int pepper = user.getPepper();
		session.setAttribute("pepper", pepper);
		return  user;
	}


	@RequestMapping(value = "/login", method = POST)
	@ResponseStatus(value = HttpStatus.OK)
	public void login(@RequestBody UserCredentials user, HttpSession session)
	{
		if (session.getAttribute("pepper") == null) {
			throw new NotAuthorizedException("Invalid pepper");
		}

		int pepper = (int) session.getAttribute("pepper");
		user.setPepper(pepper);
		String userId = service.login(user);
		session.setAttribute("uid", userId);
		session.removeAttribute("pepper");
	}

	@RequestMapping(value = "/register", method = POST,
		consumes = "application/json")
	@ResponseStatus(value = HttpStatus.OK)
	public void register(@RequestBody UserCredentials user, HttpSession session)
	{
		if (session.getAttribute("pepper") == null) {
			throw new NotAuthorizedException("Invalid pepper");
		}

		int pepper = (int) session.getAttribute("pepper");
		user.setPepper(pepper);
		String userId = service.register(user);
		session.setAttribute("uid", userId);
		session.removeAttribute("pepper");
	}

  @RequestMapping(value = "/logout", method = GET)
  @ResponseStatus(value = HttpStatus.OK)
  public void logout(HttpServletRequest request)
  {
    HttpSession session = request.getSession(false);
    if (session != null) {
      session.invalidate();
    }
  }

  @RequestMapping(value = "/test", method = GET)
  @ResponseStatus(value = HttpStatus.OK)
  public void testSession(HttpServletRequest request) {
    HttpSession session = request.getSession(false);
    if (session == null) {
      throw new NotAuthorizedException();
    }
  }

}
