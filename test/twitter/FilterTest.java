/* Copyright (c) 2007-2016 MIT 6.005 course staff, all rights reserved.
 * Redistribution of original or derived work requires permission of course staff.
 */
package twitter;

import static org.junit.Assert.*;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.junit.Test;

public class FilterTest {

    /*
     * Testing strategy for writtenBy(tweets, username):
     *
     * Partition the inputs as follows:
     *   tweets.size():                0, 1, > 1
     *   number of matching tweets:    0, 1, > 1
     *   case of username vs author:   same case, different case
     *   username vs other authors:    unrelated, prefix / substring of author
     * Also check that matching tweets keep their input order and that the
     * input list is not modified.
     *
     * Testing strategy for inTimespan(tweets, timespan):
     *
     * Partition the inputs as follows:
     *   tweets.size():                0, 1, > 1
     *   number of matching tweets:    0, 1, > 1, all
     *   tweet timestamp vs timespan:  before start, equal to start, strictly
     *                                 inside, equal to end, after end
     *   timespan length:              zero, > 0
     * Timespan includes its endpoints, so tweets sent exactly at the start or
     * end must be returned. Also check order and that input is not modified.
     *
     * Testing strategy for containing(tweets, words):
     *
     * Partition the inputs as follows:
     *   tweets.size():                0, 1, > 1
     *   words.size():                 0, 1, > 1
     *   number of matching tweets:    0, 1, > 1
     *   case of word vs tweet text:   same case, different case
     *   word position in text:        start, middle, end
     *   word appears only as part of a longer word ("talk" in "talked",
     *     "#talk", "talk?")          -> must not match
     *   tweet matches more than one of the words -> returned once
     *   text separated by more than one space
     * Also check order and that input lists are not modified.
     */

    private static final Instant d1 = Instant.parse("2016-02-17T10:00:00Z");
    private static final Instant d2 = Instant.parse("2016-02-17T11:00:00Z");
    private static final Instant d3 = Instant.parse("2016-02-17T12:00:00Z");
    private static final Instant d4 = Instant.parse("2016-02-17T13:00:00Z");

    private static final Tweet tweet1 = new Tweet(1, "alyssa", "is it reasonable to talk about rivest so much?", d1);
    private static final Tweet tweet2 = new Tweet(2, "bbitdiddle", "rivest talk in 30 minutes #hype", d2);
    private static final Tweet tweet3 = new Tweet(3, "Alyssa", "Talk was great, Rivest is a legend", d3);
    private static final Tweet tweet4 = new Tweet(4, "alyssa_p", "I talked to everyone about #talk", d4);

    @Test(expected=AssertionError.class)
    public void testAssertionsEnabled() {
        assert false; // make sure assertions are enabled with VM argument: -ea
    }

    /* ----------------------- writtenBy ----------------------- */

    // covers tweets.size() = 0
    @Test
    public void testWrittenByEmptyList() {
        List<Tweet> writtenBy = Filter.writtenBy(new ArrayList<Tweet>(), "alyssa");

        assertTrue("expected empty list", writtenBy.isEmpty());
    }

    // covers tweets.size() = 1, 0 matches
    @Test
    public void testWrittenByOneTweetNoMatch() {
        List<Tweet> writtenBy = Filter.writtenBy(Arrays.asList(tweet2), "alyssa");

        assertTrue("expected empty list", writtenBy.isEmpty());
    }

    // covers tweets.size() > 1, 1 match, same case
    @Test
    public void testWrittenByMultipleTweetsSingleResult() {
        List<Tweet> writtenBy = Filter.writtenBy(Arrays.asList(tweet1, tweet2), "alyssa");

        assertEquals("expected singleton list", 1, writtenBy.size());
        assertTrue("expected list to contain tweet", writtenBy.contains(tweet1));
    }

    // covers > 1 matches, different case, username is a prefix of another
    // author, order preserved
    @Test
    public void testWrittenByCaseInsensitiveMultipleResultsInOrder() {
        List<Tweet> writtenBy = Filter.writtenBy(Arrays.asList(tweet1, tweet2, tweet3, tweet4), "ALYSSA");

        assertEquals("expected tweets by alyssa in order", Arrays.asList(tweet1, tweet3), writtenBy);
    }

    // covers username is a substring of the author but not equal
    @Test
    public void testWrittenBySubstringOfAuthorNoMatch() {
        List<Tweet> writtenBy = Filter.writtenBy(Arrays.asList(tweet1, tweet2, tweet4), "bit");

        assertTrue("expected empty list", writtenBy.isEmpty());
    }

    // covers input list not modified
    @Test
    public void testWrittenByListNotModified() {
        List<Tweet> tweets = Arrays.asList(tweet3, tweet1, tweet2);
        List<Tweet> copy = new ArrayList<>(tweets);

        Filter.writtenBy(tweets, "alyssa");

        assertEquals("expected input list unchanged", copy, tweets);
    }

    /* ----------------------- inTimespan ---------------------- */

    // covers tweets.size() = 0
    @Test
    public void testInTimespanEmptyList() {
        List<Tweet> inTimespan = Filter.inTimespan(new ArrayList<Tweet>(), new Timespan(d1, d4));

        assertTrue("expected empty list", inTimespan.isEmpty());
    }

    // covers all tweets strictly inside, > 1 matches, order preserved
    @Test
    public void testInTimespanMultipleTweetsMultipleResults() {
        Instant testStart = Instant.parse("2016-02-17T09:00:00Z");
        Instant testEnd = Instant.parse("2016-02-17T12:00:00Z");

        List<Tweet> inTimespan = Filter.inTimespan(Arrays.asList(tweet1, tweet2), new Timespan(testStart, testEnd));

        assertFalse("expected non-empty list", inTimespan.isEmpty());
        assertTrue("expected list to contain tweets", inTimespan.containsAll(Arrays.asList(tweet1, tweet2)));
        assertEquals("expected same order", 0, inTimespan.indexOf(tweet1));
    }

    // covers tweets before start and after end, 0 matches
    @Test
    public void testInTimespanAllOutside() {
        Timespan timespan = new Timespan(Instant.parse("2016-02-17T10:30:00Z"),
                                         Instant.parse("2016-02-17T10:45:00Z"));

        List<Tweet> inTimespan = Filter.inTimespan(Arrays.asList(tweet1, tweet2, tweet3), timespan);

        assertTrue("expected empty list", inTimespan.isEmpty());
    }

    // covers timestamps equal to start and to end (endpoints are inclusive),
    // before start, after end, order preserved
    @Test
    public void testInTimespanEndpointsIncluded() {
        List<Tweet> inTimespan = Filter.inTimespan(Arrays.asList(tweet4, tweet1, tweet3, tweet2),
                                                   new Timespan(d2, d3));

        assertEquals("expected tweets at start and end, in input order", Arrays.asList(tweet3, tweet2), inTimespan);
    }

    // covers zero-length timespan, tweets.size() = 1, 1 match
    @Test
    public void testInTimespanZeroLengthTimespan() {
        List<Tweet> inTimespan = Filter.inTimespan(Arrays.asList(tweet2), new Timespan(d2, d2));

        assertEquals("expected the single tweet", Arrays.asList(tweet2), inTimespan);
    }

    // covers all tweets match; input list not modified
    @Test
    public void testInTimespanAllMatchListNotModified() {
        List<Tweet> tweets = Arrays.asList(tweet3, tweet1, tweet4, tweet2);
        List<Tweet> copy = new ArrayList<>(tweets);

        List<Tweet> inTimespan = Filter.inTimespan(tweets, new Timespan(d1, d4));

        assertEquals("expected all tweets in input order", copy, inTimespan);
        assertEquals("expected input list unchanged", copy, tweets);
    }

    /* ----------------------- containing ---------------------- */

    // covers words.size() = 1, > 1 matches, same case, middle of text
    @Test
    public void testContaining() {
        List<Tweet> containing = Filter.containing(Arrays.asList(tweet1, tweet2), Arrays.asList("talk"));

        assertFalse("expected non-empty list", containing.isEmpty());
        assertTrue("expected list to contain tweets", containing.containsAll(Arrays.asList(tweet1, tweet2)));
        assertEquals("expected same order", 0, containing.indexOf(tweet1));
    }

    // covers tweets.size() = 0
    @Test
    public void testContainingEmptyTweetList() {
        List<Tweet> containing = Filter.containing(new ArrayList<Tweet>(), Arrays.asList("talk"));

        assertTrue("expected empty list", containing.isEmpty());
    }

    // covers words.size() = 0
    @Test
    public void testContainingEmptyWordList() {
        List<Tweet> containing = Filter.containing(Arrays.asList(tweet1, tweet2), new ArrayList<String>());

        assertTrue("expected empty list", containing.isEmpty());
    }

    // covers different case, word at start of text
    @Test
    public void testContainingCaseInsensitive() {
        List<Tweet> containing = Filter.containing(Arrays.asList(tweet1, tweet3), Arrays.asList("TALK"));

        assertEquals("expected both tweets in order", Arrays.asList(tweet1, tweet3), containing);
    }

    // covers word only part of longer words ("talked", "#talk", "Talk"+",")
    // -> 0 matches
    @Test
    public void testContainingPartOfWordDoesNotMatch() {
        List<Tweet> containing = Filter.containing(Arrays.asList(tweet4), Arrays.asList("talk"));

        assertTrue("expected empty list", containing.isEmpty());
    }

    // covers words.size() > 1, word at end of text, tweet matching several
    // words returned only once, 1 match
    @Test
    public void testContainingMultipleWordsNoDuplicates() {
        List<Tweet> containing = Filter.containing(Arrays.asList(tweet1, tweet3, tweet4),
                                                   Arrays.asList("legend", "Was", "a"));

        assertEquals("expected tweet3 exactly once", Arrays.asList(tweet3), containing);
    }

    // covers words separated by several spaces, tweets.size() = 1
    @Test
    public void testContainingMultipleSpaces() {
        Tweet spaced = new Tweet(5, "ben", "  see   you  tomorrow  ", d1);

        List<Tweet> containing = Filter.containing(Arrays.asList(spaced), Arrays.asList("you"));

        assertEquals("expected the tweet", Arrays.asList(spaced), containing);
    }

    // covers input lists not modified
    @Test
    public void testContainingListsNotModified() {
        List<Tweet> tweets = Arrays.asList(tweet2, tweet1);
        List<String> words = Arrays.asList("rivest", "hype");
        List<Tweet> tweetsCopy = new ArrayList<>(tweets);
        List<String> wordsCopy = new ArrayList<>(words);

        List<Tweet> containing = Filter.containing(tweets, words);

        assertEquals("expected both tweets in input order", tweetsCopy, containing);
        assertEquals("expected tweet list unchanged", tweetsCopy, tweets);
        assertEquals("expected word list unchanged", wordsCopy, words);
    }

    /*
     * Warning: all the tests you write here must be runnable against any Filter
     * class that follows the spec. It will be run against several staff
     * implementations of Filter, which will be done by overwriting
     * (temporarily) your version of Filter with the staff's version.
     * DO NOT strengthen the spec of Filter or its methods.
     *
     * In particular, your test cases must not call helper methods of your own
     * that you have put in Filter, because that means you're testing a stronger
     * spec than Filter says. If you need such helper methods, define them in a
     * different class. If you only need them in this test class, then keep them
     * in this test class.
     */

}
