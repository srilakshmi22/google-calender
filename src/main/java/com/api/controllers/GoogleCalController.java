package com.api.controllers;

import com.google.api.client.auth.oauth2.AuthorizationCodeRequestUrl;
import com.google.api.client.auth.oauth2.Credential;
import com.google.api.client.auth.oauth2.TokenResponse;
import com.google.api.client.googleapis.auth.oauth2.GoogleAuthorizationCodeFlow;
import com.google.api.client.googleapis.auth.oauth2.GoogleClientSecrets;
import com.google.api.client.googleapis.auth.oauth2.GoogleClientSecrets.Details;
import com.google.api.client.googleapis.javanet.GoogleNetHttpTransport;
import com.google.api.client.http.HttpTransport;
import com.google.api.client.json.JsonFactory;
import com.google.api.client.json.gson.GsonFactory;
import com.google.api.client.util.DateTime;
import com.google.api.services.calendar.CalendarScopes;
import com.google.api.services.calendar.model.*;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.view.RedirectView;
import javax.servlet.http.HttpServletRequest;
import java.io.IOException;
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;

@Controller
public class GoogleCalController  {

	private final static Log logger = LogFactory.getLog(GoogleCalController.class);
	private static final String APPLICATION_NAME = "google-calendar-api";
	private static HttpTransport httpTransport;
	private static final JsonFactory JSON_FACTORY = GsonFactory.getDefaultInstance();
	private static com.google.api.services.calendar.Calendar client;
	@Value("${google.client.client-id}")
	private String clientId;

	@Value("${google.client.client-secret}")
	private String clientSecret;
	@Value("${google.client.redirectUri}")
	private String redirectURI;

	final DateTime date1 = new DateTime("2022-07-27T09:00:00.000+05:30");
	final DateTime date2 = new DateTime("2022-07-27T20:00:00.000+05:30");

	GoogleClientSecrets clientSecrets;
	GoogleAuthorizationCodeFlow flow;
	Credential credential;
	TokenResponse response;

	@GetMapping("/meetings")
	public ResponseEntity<String> scheduleGoogleMeeting() throws IOException {

		credential = flow.createAndStoreCredential(response, "Srilakshmi Veesam");

		client = new com.google.api.services.calendar.Calendar.Builder(httpTransport, JSON_FACTORY, credential)
				.setApplicationName(APPLICATION_NAME)
				.build();

		String calendarId = "primary";
		client.events().insert(calendarId, createCalenderEvent())
				.setConferenceDataVersion(1)
				.setSendNotifications(true).execute();
		return new ResponseEntity<>("Created Calendar Event", HttpStatus.OK);

	}

	@RequestMapping(value = "/login/google", method = RequestMethod.GET)
	public RedirectView googleConnectionStatus(HttpServletRequest request) throws  Exception{
		return new RedirectView(authorize());
	}



	@RequestMapping(value = "/login/google", method = RequestMethod.GET, params = "code")
	public ResponseEntity<String> oauth2Callback(@RequestParam(value = "code") String code) {
		Events eventList;
		String message;
		try {

			response = flow.newTokenRequest(code).setRedirectUri(redirectURI).execute();
			response.setExpiresInSeconds(18000L);

			message = "Created Access token";


		} catch (Exception e) {
			logger.warn("Exception while handling OAuth2 callback (" + e.getMessage() + ")."
					+ " Redirecting to google connection status page.");
			message = "Exception while handling OAuth2 callback (" + e.getMessage() + ")."
					+ " Redirecting to google connection status page.";
		}
		return new ResponseEntity<>(message, HttpStatus.OK);
	}

	private String authorize() throws Exception {
		AuthorizationCodeRequestUrl authorizationUrl;

		if (flow == null) {
			Details web = new Details();
			web.setClientId(clientId);
			web.setClientSecret(clientSecret);
			clientSecrets = new GoogleClientSecrets().setWeb(web);
			httpTransport = GoogleNetHttpTransport.newTrustedTransport();
			flow = new GoogleAuthorizationCodeFlow.Builder(httpTransport, JSON_FACTORY, clientSecrets,
					Collections.singleton(CalendarScopes.CALENDAR))
					.setAccessType("offline")
					.setApprovalPrompt("force")
					.build();
		}
		authorizationUrl=flow.newAuthorizationUrl().setRedirectUri(redirectURI);

		System.out.println("cal authorizationUrl->" + authorizationUrl);
		return  authorizationUrl.build();
	}


	public Event createCalenderEvent(){
		Event event = new Event()
				.setSummary("Slack Google Calendar Test")
				.setLocation("Hyderabad")
				.setDescription("To test  Google's calendar integration.");
		ConferenceSolutionKey conferenceSKey = new ConferenceSolutionKey();
		conferenceSKey.setType("hangoutsMeet");
		CreateConferenceRequest createConferenceReq = new CreateConferenceRequest();
		createConferenceReq.setRequestId("adojajaod"); // ID generated by you

		createConferenceReq.setConferenceSolutionKey(conferenceSKey);
		ConferenceData conferenceData = new ConferenceData();
		conferenceData.setCreateRequest(createConferenceReq);

		System.out.println(conferenceData);
		event.setConferenceData(conferenceData);

		DateTime startDateTime = date1;
		EventDateTime start = new EventDateTime()
				.setDateTime(startDateTime)
				.setTimeZone("IST");
		event.setStart(start);

		DateTime endDateTime = date2;
		EventDateTime end = new EventDateTime()
				.setDateTime(endDateTime)
				.setTimeZone("IST");
		event.setEnd(end);

		String[] recurrence = new String[] {"RRULE:FREQ=DAILY;COUNT=2"};
		event.setRecurrence(Arrays.asList(recurrence));

		EventAttendee[] attendees = new EventAttendee[] {
				new EventAttendee().setEmail("srilakshmi.veesam@gmail.com"),
				new EventAttendee().setEmail("sveesam@salesforce.com"),
				new EventAttendee().setEmail("gurwinder.singh@salesforce.com"),
				new EventAttendee().setEmail("taddala@salesforce.com"),
				new EventAttendee().setEmail("ksivasubramaniam@salesforce.com")

		};
		event.setAttendees(Arrays.asList(attendees));

		EventReminder[] reminderOverrides = new EventReminder[] {
				new EventReminder().setMethod("email").setMinutes(24 * 60),
				new EventReminder().setMethod("popup").setMinutes(5),
		};
		Event.Reminders reminders = new Event.Reminders()
				.setUseDefault(false)
				.setOverrides(Arrays.asList(reminderOverrides));
		event.setReminders(reminders);
		event.getHangoutLink();
		return event;
	}


}