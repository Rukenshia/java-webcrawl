package com.janchr;


import java.io.BufferedReader;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Created by Jan on 8/20/2016.
 */

class CrawlThread implements Runnable {
    final static Pattern urlPat = Pattern.compile("(https?|ftp|file)://[-a-zA-Z0-9+&@#/%?=~_|!:,.;]*[-a-zA-Z0-9+&@#/%=~_|]");

    Crawler c;
    int num;
    boolean stop;
    public Thread t;

    public CrawlThread(Crawler c, int num) {
        this.c = c;
        this.num = num;
        this.t = new Thread(this, "CrawlThread");
        t.start();
    }

    private LinkedList<String> parse(BufferedReader r) {
        String lineBuf = "";
        LinkedList<String> urls = new LinkedList<String>();
        do {
            try {
                lineBuf = r.readLine();
            } catch (IOException e) {
                System.out.println("(" + this.num + ") error parsing: " + e);
                return urls;
            }
            if (lineBuf == null) {
                return urls;
            }

            Matcher m = urlPat.matcher(lineBuf);
            while(m.find()) {
                //System.out.println("(" + this.num + ") match: " + m.group(0));
                urls.add(m.group(0));
            }

        } while(lineBuf != null);
        return urls;
    }

    public void run() {
        // pop_front the next URL and get it
        do {
            String surl = c.next();
            //System.out.println("(" + this.num + ") getting " + surl);

            URL url;
            try {
                url = new URL(surl);
            } catch (MalformedURLException e) {
                System.out.println("(" + this.num + ") bad url " + surl + ": " + e);
                continue;
            }

            BufferedReader r;
            try {
                r = Http.Get(url);
            } catch (IOException e) {
                System.out.println("(" + this.num + ") IOException Http.Get " + surl + ": " + e);
                continue;
            }
            c.done(surl);

            for (String newUrl: this.parse(r)) {
                c.addURL(newUrl);
            }

        } while(!this.stop);
    }
}

class VisitedURL {
    public String url;
    public int visits;

    VisitedURL(String url) {
        this.url = url;
    }
}

public class Crawler {
    private List<String> queue = Collections.synchronizedList(new LinkedList<>());

    private Map<String, VisitedURL> visited = Collections.synchronizedMap(new LinkedHashMap<>());
    private ArrayList<CrawlThread> threads = new ArrayList<>();
    private int maxThreads;

    public Crawler(int maxThreads) {
        this.maxThreads = maxThreads;
    }

    public void start(String entryPoint) {
        this.queue.add(entryPoint);

        for (int i = 0; i < this.maxThreads; i++) {
            this.threads.add(new CrawlThread(this, i));
        }
    }

    public synchronized void stop() {
        for(CrawlThread t: this.threads) {
            // interrupting the thread should be fine for us in our use-case.
            t.stop = true;
            t.t.interrupt();
        }
    }

    public synchronized String next() {
        // I got IndexOutOfBoundsException here when starting up the crawler.
        // the only way to fix it for me was this loop. I don't know what would
        // be a better way to fix it. A mutex didn't work for me.
        do {
            if (this.queue.size() == 0) {
                try {
                    wait();
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        } while(this.queue.size() == 0);

        synchronized (this.queue) {
            if (this.queue.size() == 1) {
                System.out.println("QUEUE EMPTY NOW");
            }
            return this.queue.remove(0);
        }
    }

    public void done(String url) {
        final VisitedURL obj = this.visited.putIfAbsent(url, new VisitedURL(url));
        if (obj == null) {
            this.visited.get(url).visits++;
        }
    }

    public synchronized void addURL(String url) {
        // TODO: we might want to ignore the URLs query
        if (this.queue.contains(url)) {
            return;
        }
        if (this.visited.containsKey(url)) {
            this.visited.get(url).visits++;
            return;
        }
        this.queue.add(url);
        notifyAll();
    }

    public Map<String, VisitedURL> getVisitedUrls() {
        return visited;
    }
}
