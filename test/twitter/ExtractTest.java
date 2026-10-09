/* Copyright (c) 2007-2016 MIT 6.005 course staff, all rights reserved.
 * Redistribution of original or derived work requires permission of course staff.
 */
package twitter;

import static org.junit.Assert.*;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.junit.Test;

public class ExtractTest {

    /*
     * Testing strategy for getTimespan(tweets):
     *
     * Partition the inputs as follows:
     *   tweets.size():                  0, 1, > 1
     *   order of timestamps in list:    ascending, descending, unordered
     *   position of earliest / latest:  first, middle, last
     *   distinct timestamps:            all equal, some equal, all distinct
     *
     * For an empty list the spec is underdetermined: any zero-length interval
     * is a minimum-length interval, so we only check start equals end.
     * For a single tweet, start and end must both equal its timestamp.
     * Also check that the input list is not modified.
     *
     * Testing strategy for getMentionedUsers(tweets):
     *
     * Partition the inputs as follows:
     *   tweets.size():                  0, 1, > 1
     *   mentions in a tweet:            0, 1, > 1
     *   position of mention in text:    start, middle, end
     *   character before "@":           none (start), space, punctuation,
     *                                   username character (e.g. email address)
     *   character after username:       none (end), space, punctuation
     *   username characters:            letters only, digits, "_" and "-"
     *   "@" not followed by a username: "@ ", "@" at end, "@@"
     *   same user mentioned:            once, twice in one tweet,
     *                                   in different tweets, in different case
     *
     * The case of the returned usernames is underdetermined, so the tests
     * compare the result after converting every username to lowercase, and
     * check the set size to make sure a user appears at most once.
     */

    private static final Instant d1 = Instant.parse("2016-02-17T10:00:00Z");
    private static final Instant d2 = Instant.parse("2016-02-17T11:00:00Z");
    private static final Instant d3 = Instant.parse("2016-02-17T12:30:00Z");
    private static final Instant d4 = Instant.parse("2016-02-18T09:15:00Z");

    private static final Tweet tweet1 = new Tweet(1, "alyssa", "is it reasonable to talk about rivest so much?", d1);
    private static final Tweet tweet2 = new Tweet(2, "bbitdiddle", "rivest talk in 30 minutes #hype", d2);
    private static final Tweet tweet3 = new Tweet(3, "ben", "@alyssa see you at the talk", d3);
    private static final Tweet tweet4 = new Tweet(4, "Eva_Lu-Ator", "Great talk by @Rivest today, thanks @MIT_6005-staff!", d4);

    @Test(expected=AssertionError.class)
    public void testAssertionsEnabled() {
        assert false; // make sure assertions are enabled with VM argument: -ea
    }

    /* ---------------------- getTimespan ---------------------- */

    // covers tweets.size() = 0
    @Test
    public void testGetTimespanEmptyListZeroLength() {
        Timespan timespan = Extract.getTimespan(new ArrayList<Tweet>());

        assertEquals("expected zero-length timespan", timespan.getStart(), timespan.getEnd());
    }

    // covers tweets.size() = 1
    @Test
    public void testGetTimespanOneTweet() {
        Timespan timespan = Extract.getTimespan(Arrays.asList(tweet1));

        assertEquals("expected start", d1, timespan.getStart());
        assertEquals("expected end", d1, timespan.getEnd());
    }

    // covers tweets.size() > 1, ascending order, all distinct
    @Test
    public void testGetTimespanTwoTweets() {
        Timespan timespan = Extract.getTimespan(Arrays.asList(tweet1, tweet2));

        assertEquals("expected start", d1, timespan.getStart());
        assertEquals("expected end", d2, timespan.getEnd());
    }

    // covers descending order, earliest last, latest first
    @Test
    public void testGetTimespanDescendingOrder() {
        Timespan timespan = Extract.getTimespan(Arrays.asList(tweet4, tweet3, tweet2, tweet1));

        assertEquals("expected start", d1, timespan.getStart());
        assertEquals("expected end", d4, timespan.getEnd());
    }

    // covers unordered list, earliest and latest in the middle
    @Test
    public void testGetTimespanUnorderedExtremesInMiddle() {
        Timespan timespan = Extract.getTimespan(Arrays.asList(tweet2, tweet1, tweet4, tweet3));

        assertEquals("expected start", d1, timespan.getStart());
        assertEquals("expected end", d4, timespan.getEnd());
    }

    // covers all timestamps equal
    @Test
    public void testGetTimespanAllSameTimestamp() {
        Tweet a = new Tweet(10, "user1", "first", d2);
        Tweet b = new Tweet(11, "user2", "second", d2);
        Tweet c = new Tweet(12, "user3", "third", d2);

        Timespan timespan = Extract.getTimespan(Arrays.asList(a, b, c));

        assertEquals("expected start", d2, timespan.getStart());
        assertEquals("expected end", d2, timespan.getEnd());
    }

    // covers some timestamps equal; input list not modified
    @Test
    public void testGetTimespanSomeSameTimestampListNotModified() {
        Tweet sameAsD3 = new Tweet(20, "user4", "same time as tweet3", d3);
        List<Tweet> tweets = Arrays.asList(tweet3, sameAsD3, tweet2);
        List<Tweet> copy = new ArrayList<>(tweets);

        Timespan timespan = Extract.getTimespan(tweets);

        assertEquals("expected start", d2, timespan.getStart());
        assertEquals("expected end", d3, timespan.getEnd());
        assertEquals("expected input list unchanged", copy, tweets);
    }

    /* ------------------- getMentionedUsers ------------------- */

    // covers tweets.size() = 0
    @Test
    public void testGetMentionedUsersEmptyList() {
        Set<String> mentionedUsers = Extract.getMentionedUsers(new ArrayList<Tweet>());

        assertTrue("expected empty set", mentionedUsers.isEmpty());
    }

    // covers tweets.size() = 1, 0 mentions
    @Test
    public void testGetMentionedUsersNoMention() {
        Set<String> mentionedUsers = Extract.getMentionedUsers(Arrays.asList(tweet1));

        assertTrue("expected empty set", mentionedUsers.isEmpty());
    }

    // covers 1 mention, mention at start of text, followed by space
    @Test
    public void testGetMentionedUsersOneMentionAtStart() {
        Set<String> mentionedUsers = Extract.getMentionedUsers(Arrays.asList(tweet3));

        assertEquals("expected one user", 1, mentionedUsers.size());
        assertEquals(set("alyssa"), toLowerCase(mentionedUsers));
    }

    // covers mention at end of text, nothing after the username
    @Test
    public void testGetMentionedUsersMentionAtEnd() {
        Tweet tweet = new Tweet(30, "ben", "slides are posted, thanks to @alyssa", d1);

        Set<String> mentionedUsers = Extract.getMentionedUsers(Arrays.asList(tweet));

        assertEquals(set("alyssa"), toLowerCase(mentionedUsers));
    }

    // covers > 1 mentions in one tweet, mixed case, "_" and "-" and digits,
    // username followed by punctuation
    @Test
    public void testGetMentionedUsersMultipleMentionsSpecialCharacters() {
        Set<String> mentionedUsers = Extract.getMentionedUsers(Arrays.asList(tweet4));

        assertEquals("expected two users", 2, mentionedUsers.size());
        assertEquals(set("rivest", "mit_6005-staff"), toLowerCase(mentionedUsers));
    }

    // covers "@" preceded by a username character (email address)
    @Test
    public void testGetMentionedUsersEmailAddressIsNotMention() {
        Tweet tweet = new Tweet(31, "ben", "email me at bitdiddle@mit.edu for the notes", d1);

        Set<String> mentionedUsers = Extract.getMentionedUsers(Arrays.asList(tweet));

        assertTrue("expected empty set", mentionedUsers.isEmpty());
    }

    // covers "@" preceded by punctuation, in the middle of the text
    @Test
    public void testGetMentionedUsersPrecededByPunctuation() {
        Tweet tweet = new Tweet(32, "ben", "great point (@alyssa) and also,@ben_b agrees", d1);

        Set<String> mentionedUsers = Extract.getMentionedUsers(Arrays.asList(tweet));

        assertEquals(set("alyssa", "ben_b"), toLowerCase(mentionedUsers));
    }

    // covers "@" not followed by any username character
    @Test
    public void testGetMentionedUsersAtSignWithoutUsername() {
        Tweet tweet = new Tweet(33, "ben", "meet me @ 5pm, bring @@ signs and an @", d1);

        Set<String> mentionedUsers = Extract.getMentionedUsers(Arrays.asList(tweet));

        assertTrue("expected empty set", mentionedUsers.isEmpty());
    }

    // covers same user mentioned twice in one tweet and in different case
    // across tweets; tweets.size() > 1
    @Test
    public void testGetMentionedUsersSameUserDifferentCase() {
        Tweet a = new Tweet(34, "ben", "@Alyssa @alyssa are you there?", d1);
        Tweet b = new Tweet(35, "eva", "ask @ALYSSA or @bbitdiddle", d2);

        Set<String> mentionedUsers = Extract.getMentionedUsers(Arrays.asList(a, b, tweet1));

        assertEquals("expected each user at most once", 2, mentionedUsers.size());
        assertEquals(set("alyssa", "bbitdiddle"), toLowerCase(mentionedUsers));
    }

    // covers input list not modified
    @Test
    public void testGetMentionedUsersListNotModified() {
        List<Tweet> tweets = Arrays.asList(tweet3, tweet4);
        List<Tweet> copy = new ArrayList<>(tweets);

        Extract.getMentionedUsers(tweets);

        assertEquals("expected input list unchanged", copy, tweets);
    }

    /* ------------------------ helpers ------------------------ */

    private static Set<String> set(String... usernames) {
        return new HashSet<>(Arrays.asList(usernames));
    }

    private static Set<String> toLowerCase(Set<String> usernames) {
        Set<String> lower = new HashSet<>();
        for (String username : usernames) {
            lower.add(username.toLowerCase());
        }
        return Collections.unmodifiableSet(lower);
    }

    /*
     * Warning: all the tests you write here must be runnable against any
     * Extract class that follows the spec. It will be run against several staff
     * implementations of Extract, which will be done by overwriting
     * (temporarily) your version of Extract with the staff's version.
     * DO NOT strengthen the spec of Extract or its methods.
     *
     * In particular, your test cases must not call helper methods of your own
     * that you have put in Extract, because that means you're testing a
     * stronger spec than Extract says. If you need such helper methods, define
     * them in a different class. If you only need them in this test class, then
     * keep them in this test class.
     */

}
