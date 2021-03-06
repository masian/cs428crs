package controllers;

import exceptions.NotAuthorizedException;
import exceptions.ResourceNotFoundException;
import exceptions.ServerException;
import models.Schedule;
import org.apache.commons.codec.binary.Base64;
import org.apache.commons.io.IOUtils;
import org.json.JSONObject;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;
import packages.Courses;
import packages.Departments;
import packages.Requirements;
import packages.Schedules;
import service.PublicWebService;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

import static org.springframework.web.bind.annotation.RequestMethod.*;

/**
 * @autor: Nick Humrich
 * @date: 1/17/14
 */
@Controller
@EnableAutoConfiguration
@RequestMapping("/public-api")
public class PublicWebController {
  private boolean shuttingDown;
  private int countdown;

	private PublicWebService webService;
	private Courses cachedCourses;

	public PublicWebController() {
    countdown = 5;
    shuttingDown = false;
		webService = new PublicWebService();
	}

  @RequestMapping(value = "/shutdown", method = POST)
  @ResponseStatus(value = HttpStatus.OK)
  public void shutdown(HttpServletRequest req)
  {
    //for temparary testing
    //System.out.println(req.getRemoteAddr());

    if (!req.getRemoteAddr().matches("172[.]31[.]\\d{1,3}[.]\\d{1,3}|localhost|127[.]0[.]0[.]1")) {
      throw new ResourceNotFoundException();
    }
    shuttingDown = true;
  }

  @RequestMapping(value = "/health", method = GET)
  @ResponseStatus(value = HttpStatus.OK)
  public void healthCheck(HttpServletRequest req)
  {
    //for temporary testing
    //System.out.println(req.getRemoteAddr());

    if (!req.getRemoteAddr().matches("172[.]31[.]\\d{1,3}[.]\\d{1,3}|localhost|127[.]0[.]0[.]1")) {
      throw new ResourceNotFoundException();
    }
    if (shuttingDown) {
      if (countdown-- <= 0) {
        System.exit(0);
      }
      //ToDo: should probably be changed to a 503 instead of a 500
      throw new ServerException("Server Shutting Down");
    }
  }

	/**
	 * Gets all departments that exist in the systen
	 * @param dummy a boolean stating if you want dummy data
	 * @return fully populated list of departments
	 */
	@RequestMapping(value = "/departments", method = GET)
	public @ResponseBody
	Departments getAllDepartments(
		@RequestParam(value = "dummy", required = false, defaultValue = "false") Boolean dummy)
	{
		if (dummy) {
			return webService.getMockDepartments();
		}
		return webService.getAllDepartments();
	}

	/**
	 * Gets all requirements for the given major.a If no major is given or major 'none', then all
	 * the general requirements will be given
	 * @param major shortCode for major
	 * @return requirements for given major
	 */
	@RequestMapping(value = "/requirements", method = GET)
	public @ResponseBody
	Requirements getRequirements(
		@RequestParam(value = "major", required = false, defaultValue = "none") String major,
		@RequestParam(value = "dummy", required = false, defaultValue = "false")Boolean dummy)
	{
		if (dummy) {
			return webService.getMockRequirements(major);
		}
		return webService.getRequirements(major);

	}

  @RequestMapping(value = "/courses/all", method = GET)
  public @ResponseBody
  Courses getAllCourses(
      @RequestParam(value = "semester", required = false, defaultValue ="current") String semester)
  {
    if ("current".equals(semester)) {
	  if( cachedCourses == null )
	      cachedCourses = webService.getAllCourses();
	  return cachedCourses;
    } else {
      return webService.getAllCourses(semester);
    }
  }

	@RequestMapping(value = "/courses", method = GET)
	public @ResponseBody
	Courses getCourses(
		@RequestParam(value = "ids", required = true) String ids)
	{

		return new Courses();
	}

    @RequestMapping(value = "/register", method = POST)
    public String registerSchedule(@RequestBody String courseDetails, HttpSession session) {
      session.setAttribute("CourseInfo", courseDetails);
        System.out.println(courseDetails);
        return "redirect:https://cas.byu.edu/cas/login?service=http://andyetitcompiles.com/" +
                "public-api/register/handle?courseInfo="
                + courseDetails;
    }

    @RequestMapping(value = "/register/handle")
    public @ResponseBody
    String handleRegistration(
            @RequestParam String ticket, HttpSession session)
    {
      String courseInfo = (String) session.getAttribute("CourseInfo");
        webService.handleRegistration(courseInfo, ticket);
        return "ok " + courseInfo + " " + ticket;
    }


  @RequestMapping(value = "/schedules/all", method = GET)
  public @ResponseBody
  Schedules getSchedulesForUser(HttpSession session)
  {
    String uid = getUserId(session);
    return webService.getAllSchedulesForUser(uid);
  }

  @RequestMapping(value = "/schedules", method = POST,
      consumes = "application/json")
  @ResponseStatus(value = HttpStatus.OK)
  public void addSchedule(
      @RequestBody Schedule schedule, HttpSession session)
  {
    String uid = getUserId(session);
    webService.addSchedule(uid, schedule);
  }

  @RequestMapping(value = "/schedules/{id}", method = PUT,
      consumes = "application/json")
  @ResponseStatus(value = HttpStatus.OK)
  public void updateSchedule(@RequestBody Schedule schedule,
       @PathVariable String id, HttpSession session)
  {
    String uid = getUserId(session);
    webService.editSchedule(uid, id, schedule);
  }

  @RequestMapping(value = "schedules/{id}", method = DELETE,
    consumes = "application/json")
  @ResponseStatus(value = HttpStatus.OK)
  public void removeSchedule(@PathVariable String id, HttpSession session)
  {
    String uid = getUserId(session);
    webService.removeSchedule(uid, id);
  }

  @RequestMapping(value = "schedules/{id}", method = GET)
  public @ResponseBody Schedule
  getSchedule(@PathVariable String id, HttpSession session)
  {
    String uid = getUserId(session);
    return webService.getSchedule(uid, id);
  }

  @RequestMapping(value = "recaptcha", method = GET)
  public @ResponseBody String getRecaptcha()
  {
	  URL url;
	  HttpURLConnection conn;
	  BufferedReader br;
	  String line;
	  StringBuilder result = new StringBuilder();
	  try {
		  url = new URL("https://www.google.com/recaptcha/api/challenge?k=6LfoisoSAAAAAFBP_LvBQ4YlpPTBOf12MnGsjk4z");
		  conn = (HttpURLConnection) url.openConnection();
		  conn.setRequestMethod("GET");
		  InputStreamReader reader = new InputStreamReader(conn.getInputStream());
		  String response = IOUtils.toString(reader);
		  reader.close();
		  List<Integer> braceIndexes = new ArrayList<>(2);
		  boolean inQuotes = false, jsonObjectFound=false;
		  char closingQuotesChar = '\0';
		  for( int i=0; i<response.length() && !jsonObjectFound; i++ )
		  {
			  if( !inQuotes ) {
				  switch (response.charAt(i)) {
					  case '"':
					  case '\'':
						  inQuotes = true;
						  closingQuotesChar = response.charAt(i);
						  break;
					  case '{':
					  case '}':
						  braceIndexes.add(i);
						  if( braceIndexes.size() == 2 )
							  jsonObjectFound = true;
						  break;

				  }
			  } else if( closingQuotesChar == response.charAt(i) ) {
				  inQuotes = false;
			  }
		  }
		  if( braceIndexes.size() != 2 )
			  throw new RuntimeException("Google returned invalid JSON: "+response);
		  JSONObject googleJSON = new JSONObject(response.substring(braceIndexes.get(0), braceIndexes.get(1)+1));
		  JSONObject ourJSON = new JSONObject();
		  ourJSON.put("challenge", googleJSON.get("challenge"));
		  ourJSON.put("image", getRecaptchaImage(googleJSON.get("challenge").toString()));

		  return ourJSON.toString();
	  } catch (IOException e) {
		  e.printStackTrace();
		  return e.getMessage();
	  } catch (Exception e) {
		  e.printStackTrace();
		  return e.getMessage();
	  }
  }

  private String getUserId(HttpSession session) {
    Object uid = session.getAttribute("uid");
    if (uid == null) {
      throw new NotAuthorizedException("User must be logged in.");
    }
    return (String) uid;
  }

	String getRecaptchaImage(String recaptchaChallenge) throws IOException {
		URL url = new URL("https://www.google.com/recaptcha/api/image?c=" + recaptchaChallenge);

		HttpURLConnection conn = (HttpURLConnection) url.openConnection();
		conn.setRequestMethod("GET");
		InputStream is = conn.getInputStream();
		String result = new String(Base64.encodeBase64(IOUtils.toByteArray(is)), "UTF-8");
		is.close();
		return result;
	}

	@RequestMapping(value="dbupdated", method=GET)
	public @ResponseBody String dbUpdated() {
		cachedCourses = null;
		return "success";
	}

}
