/* Copyright (c) 2007-2016 MIT 6.005 course staff, all rights reserved.
 * Redistribution of original or derived work requires permission of course staff.
 */
package twitter;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Set;

/**
 * This is the main program.
 *
 * The MIT sample server is no longer online, so this version reads a small
 * file of sample tweets (tweets.txt, in the project folder) instead.
 * Each line has the form: id|author|timestamp|text. Lines starting with # are
 * comments.
 *
 * You may change this class if you wish, but you don't have to.
 */
public class Main {

    private static final String TWEETS_FILE = "tweets.txt";

    private static List<Tweet> readTweets(String fileName) throws IOException {
        List<Tweet> tweets = new ArrayList<>();
        for (String line : Files.readAllLines(Paths.get(fileName))) {
            if (line.trim().isEmpty() || line.startsWith("#")) {
                continue;
            }
            String[] parts = line.split("\\|", 4);
            tweets.add(new Tweet(Long.parseLong(parts[0]), parts[1], parts[3], Instant.parse(parts[2])));
        }
        return tweets;
    }

    /**
     * Main method of the program. Reads the sample tweets and prints some
     * facts about them.
     *
     * @param args command-line arguments (not used)
     */
    public static void main(String[] args) {
        try {
            assert false;
            throw new Error("Always run main and tests with assertions enabled");
        } catch (AssertionError ae) { }

        final List<Tweet> tweets;
        try {
            tweets = readTweets(TWEETS_FILE);
        } catch (IOException ioe) {
            throw new RuntimeException(ioe);
        }

        System.out.println("read " + tweets.size() + " tweets from " + TWEETS_FILE);

        final Timespan span = Extract.getTimespan(tweets);
        System.out.println("ranging from " + span.getStart() + " to " + span.getEnd());

        final Set<String> mentionedUsers = Extract.getMentionedUsers(tweets);
        System.out.println("mentioned users (" + mentionedUsers.size() + "): " + new java.util.TreeSet<>(mentionedUsers));

        System.out.println("tweets written by alyssa:");
        for (Tweet tweet : Filter.writtenBy(tweets, "alyssa")) {
            System.out.println("  " + tweet.getId() + ": " + tweet.getText());
        }

        System.out.println("tweets containing \"rivest\" or \"pset\":");
        for (Tweet tweet : Filter.containing(tweets, Arrays.asList("rivest", "pset"))) {
            System.out.println("  " + tweet.getId() + ": " + tweet.getText());
        }
    }

}
