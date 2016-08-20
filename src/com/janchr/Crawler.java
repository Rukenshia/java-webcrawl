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
        String url;
        public Thread t;

        public CrawlThread(Crawler c, String url) {
            this.c = c;
            this.url = url;
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
                    System.out.println("error parsing: " + e);
                    return urls;
                }
                if (lineBuf == null) {
                    return urls;
                }

                Matcher m = urlPat.matcher(lineBuf);
                while(m.find()) {
                    urls.add(m.group(0));
                }

            } while(lineBuf != null);
            return urls;
        }

        public void run() {
            URL url;
            try {
                url = new URL(this.url);
            } catch (MalformedURLException e) {
                System.out.println("bad url " + this.url + ": " + e);
                c.done(this, this.url);
                return;
            }

            BufferedReader r;
            try {
                r = Http.Get(url);
            } catch (IOException e) {
                System.out.println("IOException Http.Get " + this.url + ": " + e);
                c.done(this, this.url);
                return;
            }

            for (String newUrl: this.parse(r)) {
                c.addURL(newUrl);
            }
            c.done(this, this.url);
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

            this.tryNext();
        }

        public synchronized void stop() {
            for(CrawlThread t: this.threads) {
                // interrupting the thread should be fine for us in our use-case.
                t.t.interrupt();
            }
        }

        public synchronized boolean hasNext() {
            return this.queue.size() > 0;
        }

        public synchronized String next() {
            if (this.queue.size() == 0) {
                return null;
            }
            return this.queue.remove(0);
        }

        private void tryNext() {
            if (!this.hasNext() || this.threads.size() == this.maxThreads) {
                return;
            }

            String next = this.next();
            if (next == null) {
                System.out.println("invalid next string");
                return;
            }

            this.threads.add(new CrawlThread(this, next));
        }

        public void done(CrawlThread t, String url) {
            final VisitedURL obj = this.visited.putIfAbsent(url, new VisitedURL(url));
            if (obj == null) {
                this.visited.get(url).visits++;
            }
            this.threads.remove(t);
            this.tryNext();
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

            this.tryNext();
        }

        public Map<String, VisitedURL> getVisitedUrls() {
            return visited;
        }
    }
