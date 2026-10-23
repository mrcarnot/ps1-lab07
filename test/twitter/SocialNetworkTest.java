/* Copyright (c) 2007-2016 MIT 6.005 course staff, all rights reserved.
 * Redistribution of original or derived work requires permission of course staff.
 */
package twitter;

import static org.junit.Assert.*;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.junit.Test;

public class SocialNetworkTest {

    /*
     * Testing strategy for guessFollowsGraph(tweets):
     *
     * Partition the inputs as follows:
     *   tweets.size():                    0, 1, > 1
     *   mentions in a tweet:              0, 1, > 1
     *   tweets by the same author:        1, > 1 (mentioning different users)
     *   same user mentioned:              once, again in a different case
     *   author mentions themself:         no, yes
     *
     * The postcondition is underdetermined in three ways, so the tests must
     * not over-specify it:
     *   - a user who follows nobody may be missing from the keys, or map to
     *     an empty set, so "no follows" is checked as "every set is empty";
     *   - the case of the returned usernames is not fixed, so usernames are
     *     compared after converting them to lowercase;
     *   - other kinds of evidence may add more follows, so a test checks that
     *     the required follows are present (containsAll), not that the sets
     *     are exactly equal. The only exception is when no other follow is
     *     possible (e.g. a single author, who cannot follow themself).
     * Every test also checks the parts of the spec that always hold: users do
     * not follow themselves, every username is an author or an @-mention, and
     * a username appears at most once in each set. The input list must not be
     * modified.
     *
     * Testing strategy for influencers(followsGraph):
     *
     * Partition the inputs as follows:
     *   number of users in the graph:     0, 1, > 1
     *   users with followers:             0, 1, > 1
     *   follower counts:                  all distinct, some tied
     *   user appears only as:             a key, a value, both
     *   case of the same username:        same everywhere, different case
     *
     * The order of users with the same follower count is underdetermined, so
     * tied users are compared as a set over the positions they occupy. The
     * case of the returned usernames is not fixed either, so they are
     * compared in lowercase. The graph must not be modified.
     */

    private static final Instant d1 = Instant.parse("2016-02-17T10:00:00Z");
    private static final Instant d2 = Instant.parse("2016-02-17T11:00:00Z");
    private static final Instant d3 = Instant.parse("2016-02-17T12:00:00Z");

    @Test(expected=AssertionError.class)
    public void testAssertionsEnabled() {
        assert false; // make sure assertions are enabled with VM argument: -ea
    }

    /* ------------------- guessFollowsGraph ------------------- */

    // Test case 1 - Empty List of Tweets
    // covers tweets.size() = 0
    @Test
    public void testGuessFollowsGraphEmpty() {
        Map<String, Set<String>> followsGraph = SocialNetwork.guessFollowsGraph(new ArrayList<>());

        assertTrue("expected empty graph", followsGraph.isEmpty());
    }

    // Test case 2 - Tweets Without Mentions
    // covers tweets.size() > 1, 0 mentions, > 1 tweets by the same author
    @Test
    public void testGuessFollowsGraphNoMentions() {
        Tweet t1 = new Tweet(1, "alyssa", "is it reasonable to talk about rivest so much?", d1);
        Tweet t2 = new Tweet(2, "alyssa", "mail me at alyssa@mit.edu", d2);
        List<Tweet> tweets = Arrays.asList(t1, t2);

        Map<String, Set<String>> followsGraph = SocialNetwork.guessFollowsGraph(tweets);

        // alyssa is the only user and cannot follow herself, so nobody follows anybody
        for (Set<String> follows : followsGraph.values()) {
            assertTrue("expected no follows", follows.isEmpty());
        }
        assertValidGraph(followsGraph, tweets);
    }

    // Test case 3 - Single Mention
    // covers tweets.size() = 1, 1 mention
    @Test
    public void testGuessFollowsGraphSingleMention() {
        Tweet t1 = new Tweet(1, "alyssa", "@bbitdiddle are you coming to the talk?", d1);
        List<Tweet> tweets = Arrays.asList(t1);

        Map<String, Set<String>> followsGraph = SocialNetwork.guessFollowsGraph(tweets);

        // alyssa cannot follow herself, so bbitdiddle is the only user she can follow
        assertEquals(set("bbitdiddle"), followsOf(followsGraph, "alyssa"));
        assertValidGraph(followsGraph, tweets);
    }

    // Test case 4 - Multiple Mentions
    // covers tweets.size() = 1, > 1 mentions
    @Test
    public void testGuessFollowsGraphMultipleMentions() {
        Tweet t1 = new Tweet(1, "alyssa", "@bbitdiddle @evaluator ask @rivest about the talk", d1);
        List<Tweet> tweets = Arrays.asList(t1);

        Map<String, Set<String>> followsGraph = SocialNetwork.guessFollowsGraph(tweets);

        assertTrue("expected alyssa to follow every mentioned user",
                followsOf(followsGraph, "alyssa").containsAll(set("bbitdiddle", "evaluator", "rivest")));
        assertValidGraph(followsGraph, tweets);
    }

    // Test case 5 - Multiple Tweets from One User
    // covers tweets.size() > 1, > 1 tweets by the same author mentioning different users
    @Test
    public void testGuessFollowsGraphMultipleTweetsFromOneUser() {
        Tweet t1 = new Tweet(1, "alyssa", "@bbitdiddle see you at the talk", d1);
        Tweet t2 = new Tweet(2, "alyssa", "thanks @evaluator!", d2);
        Tweet t3 = new Tweet(3, "alyssa", "@bbitdiddle saving you a seat", d3);
        List<Tweet> tweets = Arrays.asList(t1, t2, t3);

        Map<String, Set<String>> followsGraph = SocialNetwork.guessFollowsGraph(tweets);

        assertTrue("expected follows from all of alyssa's tweets",
                followsOf(followsGraph, "alyssa").containsAll(set("bbitdiddle", "evaluator")));
        assertValidGraph(followsGraph, tweets);
    }

    // covers same user mentioned again in a different case, author in a different case
    @Test
    public void testGuessFollowsGraphCaseInsensitive() {
        Tweet t1 = new Tweet(1, "alyssa", "@BBitDiddle are you there?", d1);
        Tweet t2 = new Tweet(2, "Alyssa", "@bbitdiddle hello?", d2);
        List<Tweet> tweets = Arrays.asList(t1, t2);

        Map<String, Set<String>> followsGraph = SocialNetwork.guessFollowsGraph(tweets);

        assertEquals(set("bbitdiddle"), followsOf(followsGraph, "alyssa"));
        assertValidGraph(followsGraph, tweets);
    }

    // covers author mentions themself
    @Test
    public void testGuessFollowsGraphSelfMention() {
        Tweet t1 = new Tweet(1, "alyssa", "note to self: @Alyssa, ask @rivest", d1);
        List<Tweet> tweets = Arrays.asList(t1);

        Map<String, Set<String>> followsGraph = SocialNetwork.guessFollowsGraph(tweets);

        assertEquals(set("rivest"), followsOf(followsGraph, "alyssa"));
        assertValidGraph(followsGraph, tweets);
    }

    // covers input list is not modified
    @Test
    public void testGuessFollowsGraphListNotModified() {
        Tweet t1 = new Tweet(1, "alyssa", "@bbitdiddle see you", d1);
        Tweet t2 = new Tweet(2, "bbitdiddle", "@alyssa ok", d2);
        List<Tweet> tweets = new ArrayList<>(Arrays.asList(t1, t2));
        List<Tweet> copy = new ArrayList<>(tweets);

        SocialNetwork.guessFollowsGraph(tweets);

        assertEquals("expected list not modified", copy, tweets);
    }

    /* ---------------------- influencers ---------------------- */

    // Test case 6 - Empty Graph for influencers()
    // covers 0 users
    @Test
    public void testInfluencersEmpty() {
        Map<String, Set<String>> followsGraph = new HashMap<>();
        List<String> influencers = SocialNetwork.influencers(followsGraph);

        assertTrue("expected empty list", influencers.isEmpty());
    }

    // Test case 7 - Single User Without Followers
    // covers 1 user, 0 users with followers
    @Test
    public void testInfluencersSingleUserWithoutFollowers() {
        Map<String, Set<String>> followsGraph = new HashMap<>();
        followsGraph.put("alyssa", new HashSet<>());

        List<String> influencers = SocialNetwork.influencers(followsGraph);

        // the spec returns every username, so alyssa is listed with 0 followers
        assertEquals(Arrays.asList("alyssa"), toLowerCase(influencers));
    }

    // Test case 8 - Single Influencer
    // covers > 1 users, 1 user with followers, user only a key / only a value
    @Test
    public void testInfluencersSingleInfluencer() {
        Map<String, Set<String>> followsGraph = new HashMap<>();
        followsGraph.put("alyssa", set("bbitdiddle"));

        List<String> influencers = SocialNetwork.influencers(followsGraph);

        assertEquals(Arrays.asList("bbitdiddle", "alyssa"), toLowerCase(influencers));
    }

    // Test case 9 - Multiple Influencers
    // covers > 1 users with followers, all distinct counts, user both a key and a value
    @Test
    public void testInfluencersMultipleInfluencers() {
        Map<String, Set<String>> followsGraph = new HashMap<>();
        followsGraph.put("alyssa", set("bbitdiddle", "rivest", "evaluator"));
        followsGraph.put("evaluator", set("bbitdiddle", "rivest"));
        followsGraph.put("rivest", set("bbitdiddle"));

        List<String> influencers = SocialNetwork.influencers(followsGraph);

        // bbitdiddle 3, rivest 2, evaluator 1, alyssa 0
        assertEquals(Arrays.asList("bbitdiddle", "rivest", "evaluator", "alyssa"), toLowerCase(influencers));
    }

    // Test case 10 - Tied Influence
    // covers some tied counts
    @Test
    public void testInfluencersTiedInfluence() {
        Map<String, Set<String>> followsGraph = new HashMap<>();
        followsGraph.put("alyssa", set("bbitdiddle", "rivest"));
        followsGraph.put("evaluator", set("bbitdiddle", "rivest"));

        List<String> influencers = toLowerCase(SocialNetwork.influencers(followsGraph));

        // bbitdiddle and rivest tie with 2, alyssa and evaluator tie with 0;
        // the order inside each tie is not specified
        assertEquals("expected every user once", 4, influencers.size());
        assertEquals(set("bbitdiddle", "rivest"), new HashSet<>(influencers.subList(0, 2)));
        assertEquals(set("alyssa", "evaluator"), new HashSet<>(influencers.subList(2, 4)));
    }

    // covers same username in different case
    @Test
    public void testInfluencersUpperCaseUsernames() {
        Map<String, Set<String>> followsGraph = new HashMap<>();
        followsGraph.put("alyssa", set("BBitDiddle"));
        followsGraph.put("Evaluator", set("bbitdiddle", "ALYSSA"));

        List<String> influencers = toLowerCase(SocialNetwork.influencers(followsGraph));

        // bbitdiddle 2, alyssa 1, evaluator 0, each listed once
        assertEquals(Arrays.asList("bbitdiddle", "alyssa", "evaluator"), influencers);
    }

    // covers graph is not modified
    @Test
    public void testInfluencersGraphNotModified() {
        Map<String, Set<String>> followsGraph = new HashMap<>();
        followsGraph.put("alyssa", set("bbitdiddle"));
        followsGraph.put("evaluator", new HashSet<>());
        Map<String, Set<String>> copy = new HashMap<>();
        copy.put("alyssa", set("bbitdiddle"));
        copy.put("evaluator", new HashSet<>());

        SocialNetwork.influencers(followsGraph);

        assertEquals("expected graph not modified", copy, followsGraph);
    }

    /* ------------------------ helpers ------------------------ */

    private static Set<String> set(String... usernames) {
        return new HashSet<>(Arrays.asList(usernames));
    }

    private static List<String> toLowerCase(List<String> usernames) {
        List<String> lower = new ArrayList<>();
        for (String username : usernames) {
            lower.add(username.toLowerCase());
        }
        return Collections.unmodifiableList(lower);
    }

    /*
     * The set of users that user follows in followsGraph, in lowercase. A user
     * missing from the keys follows nobody, so this returns an empty set.
     */
    private static Set<String> followsOf(Map<String, Set<String>> followsGraph, String user) {
        Set<String> follows = new HashSet<>();
        for (Map.Entry<String, Set<String>> entry : followsGraph.entrySet()) {
            if (entry.getKey().equalsIgnoreCase(user)) {
                for (String followed : entry.getValue()) {
                    follows.add(followed.toLowerCase());
                }
            }
        }
        return follows;
    }

    /*
     * Check the parts of guessFollowsGraph's spec that hold for every
     * implementation: no self-follows, no username twice in a set, a username
     * at most once as a key, and every username is an author or an @-mention.
     */
    private static void assertValidGraph(Map<String, Set<String>> followsGraph, List<Tweet> tweets) {
        Set<String> allowed = new HashSet<>();
        for (Tweet tweet : tweets) {
            allowed.add(tweet.getAuthor().toLowerCase());
        }
        for (String mentioned : Extract.getMentionedUsers(tweets)) {
            allowed.add(mentioned.toLowerCase());
        }

        Set<String> keys = new HashSet<>();
        for (Map.Entry<String, Set<String>> entry : followsGraph.entrySet()) {
            String user = entry.getKey().toLowerCase();
            assertTrue("expected each user at most once as a key", keys.add(user));
            assertTrue("unexpected user " + user, allowed.contains(user));

            Set<String> follows = new HashSet<>();
            for (String followed : entry.getValue()) {
                String lower = followed.toLowerCase();
                assertTrue("expected each user at most once in a set", follows.add(lower));
                assertTrue("unexpected user " + lower, allowed.contains(lower));
            }
            assertFalse(user + " follows themself", follows.contains(user));
        }
    }

    /*
     * Warning: all the tests you write here must be runnable against any
     * SocialNetwork class that follows the spec. It will be run against several
     * staff implementations of SocialNetwork, which will be done by overwriting
     * (temporarily) your version of SocialNetwork with the staff's version.
     * DO NOT strengthen the spec of SocialNetwork or its methods.
     *
     * In particular, your test cases must not call helper methods of your own
     * that you have put in SocialNetwork, because that means you're testing a
     * stronger spec than SocialNetwork says. If you need such helper methods,
     * define them in a different class. If you only need them in this test
     * class, then keep them in this test class.
     */

}
