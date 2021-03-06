package com.github.cstroe.spendhawk.web.it;

import com.github.cstroe.spendhawk.web.BaseClientIT;
import com.github.cstroe.spendhawk.web.WelcomeServlet;
import com.github.cstroe.spendhawk.web.user.UserManagerServlet;
import com.github.cstroe.spendhawk.web.user.UserSummaryServlet;
import com.github.cstroe.spendhawk.web.user.UserController;
import com.mashape.unirest.http.Unirest;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.select.Elements;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;

import java.net.URL;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static com.github.cstroe.spendhawk.util.TestUtil.hasLink;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.is;
import static org.junit.Assert.*;

/**
 * These tests are run in sequence, which goes against testing best practices.
 * Because of this, they should not be seen as individual tests, but this whole
 * class should be seen as one test.
 *
 * Other frameworks allow for nested contexts in tests, however JUnit does not
 * support nested contexts, which means it would be very expensive if these
 * tests were independent; each test would have to redo the work of the tests
 * before it.
 *
 * The other option is to have one large test that combines all these sequential
 * tests.  But we would lose the granularity we get when we split them up in
 * separate tests.
 */
public class BasicFeaturesIT extends BaseClientIT {

    private static String userDetailPath;
    private static Long userId;

    @Before
    public void setUp() {
        Unirest.setTimeouts(2000, 2000);
    }

    @Ignore
    @Test
    public void t0100_connectToWelcomeServlet() throws Exception {
        response = connect(WelcomeServlet.class);
        assertResponseStatus(200, response);

        Document doc = Jsoup.parse(response.getBody());
//        assertTrue("A link from the welcome page to the users page must exist.",
//            hasLink(doc, servletPath(UserController.class)));

        String welcomePage = response.getBody();

        response = connect(""); // connect to the context root
        assertResponseStatus(200, response);

        assertEquals("The welcome page should be served at the context root.",
                welcomePage, response.getBody());
    }

    @Ignore
    @Test
    public void t0200_connectToUsersServlet() throws Exception {
//        response = connect(UserController.class);
        assertResponseStatus(200, response);
    }

    @Ignore
    @Test
    public void t0300_connectToUserManagerServlet() throws Exception {
        response = connect(UserManagerServlet.class);
        assertResponseStatus(200, response);
    }

    @Ignore
    @Test
    public void t0400_createUser() throws Exception {
        // record how many users are in the system before we create another
//        final String viewUsersUrl = fullURL(UserController.class);
//        response = Unirest.get(viewUsersUrl).asString();
        Document docBefore = Jsoup.parse(response.getBody());
        Elements linksBefore = docBefore.getElementsByClass("userLink");

        final int numUsersBeforeTest = linksBefore.size();

        response = Unirest.post(fullURL(UserManagerServlet.class))
            .field("user.name", "testuser")
            .field("action", "Add UserDao")
            .asString();
        assertResponseStatus(302, response);

        String redirectUrl = response.getHeaders().getFirst("location");
        URL url = new URL(redirectUrl);
        assertTrue("Creating a user should take you to the summary page for that user.",
                url.getPath().startsWith(servletPath(UserSummaryServlet.class)) &&
                url.getQuery().contains("user.id="));

//        response = Unirest.get(viewUsersUrl).asString();

        Document doc = Jsoup.parse(response.getBody());
        Elements links = doc.getElementsByClass("userLink");

        assertThat(links.size(), is(equalTo(numUsersBeforeTest + 1)));

        //noinspection ThrowableResultOfMethodCallIgnored
        userDetailPath = findLinkByText(links, "testuser")
            .orElseThrow(() -> saveAndFail("Could not find link for 'testuser'.", response));

        assertTrue("The user detail link points to the AccountsServlet and takes params",
                userDetailPath.startsWith(servletPath(UserSummaryServlet.class) + "?"));

        Matcher m = Pattern.compile("user\\.id=(.*)").matcher(userDetailPath);
        if(m.find()) {
            userId = Long.parseLong(m.group(1));
        } else {
            fail("UserDao id not found in user detail path.");
        }

    }

    @Ignore
    @Test
    public void t0500_viewAccounts() throws Exception {
        response = connect(userDetailPath);
        assertResponseStatus(200, response);

        Document doc = Jsoup.parse(response.getBody());
//        assertTrue("A link from the accounts page to the account manager page must exist.",
//            hasLink(doc, servletPath(AccountManagerServlet.class), "user.id", userId.toString()));
    }

    @Ignore
    @Test
    public void t0600_addAccount() throws Exception {
        String accountName = "AccountDao 1";
//        response = Unirest.post(fullURL(AccountManagerServlet.class))
//            .field("action", "store")
//            .field("account.name", accountName)
//            .field("user.id", userId.toString())
//            .asString();

        assertResponseStatus(200, response);

        response = Unirest.get(url(UserSummaryServlet.class, "user.id", userId.toString()))
            .asString();

        assertResponseStatus(200, response);

        Document doc = Jsoup.parse(response.getBody());
        Elements links = doc.getElementsByTag("a");

        assertTrue("The accounts page must link to the individual account.",
            findLinkByText(links, accountName) != null );
    }
}
