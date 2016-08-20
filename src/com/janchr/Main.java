package com.janchr;

import java.util.Scanner;

public class Main {

    public static void main(String[] args) {
        Crawler c = new Crawler(8);

        System.out.println("starting crawler");
        c.start("http://google.com");

        Scanner s = new Scanner(System.in);
        while (!s.next().equals("exit"));

        c.stop();

        synchronized (c) {
            System.out.println("\n\n---------------------------------------------------------------------");
            for (VisitedURL u : c.getVisitedUrls().values()) {
                System.out.println(u.visits + "x " + u.url);
            }
            System.out.println("---------------------------------------------------------------------");
            System.out.println("visited " + c.getVisitedUrls().size() + " unique urls");
        }
    }
}
