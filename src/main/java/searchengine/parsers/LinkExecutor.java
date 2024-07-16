package searchengine.parsers;
import java.util.*;
import java.util.concurrent.RecursiveTask;
import java.io.IOException;
import java.util.concurrent.CopyOnWriteArraySet;

import org.apache.commons.io.FilenameUtils;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

public class LinkExecutor extends RecursiveTask<String> {
    private String url;
    private static String startUrl;
    public static ArrayList outHTML=new ArrayList();

    private static CopyOnWriteArraySet<String> allLinks = new CopyOnWriteArraySet<>();

    public LinkExecutor(String url) {
        this.url = url.trim();
    }

    public LinkExecutor(String url, String startUrl) {
        this.url = url.trim();
        LinkExecutor.startUrl = startUrl.trim();
    }
    @Override
    protected String compute() {
        StringBuffer sb = new StringBuffer(url +"\n");
        Set<LinkExecutor> subTask = new HashSet<>();
        getChildren(subTask);
        ControllerThread.setIsRun(LinkExecutor.inForkJoinPool());
        for (LinkExecutor link : subTask) {
            sb.append(link.join());
        }
        return sb.toString();
    }

    private void getChildren(Set<LinkExecutor> subTask) {
        int count=0;
        Document doc;
        Elements elements;
        try {
            Thread.sleep(200);
            while (ConditionStopIndexing.isIsStop()==true)
            {
                ConditionStopIndexing.setIsStop(false);
                Thread.sleep(50000);
                ConditionStopIndexing.setAfterStop(true);
            }
            doc = Jsoup.connect(url)
                    .userAgent("Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/112.0.0.0 Safari/537.36")
                    .referrer("http://www.google.com").followRedirects(false).ignoreContentType(true).ignoreHttpErrors(true)
                    .get();
            elements = doc.select("a");

            for (Element el : elements) {
                String attr = el.attr("abs:href");

                if (!attr.isEmpty() && attr.startsWith(startUrl) && !allLinks.contains(attr) && !attr.contains("#")
                        &&!attr.equals(startUrl)&& !FilenameUtils.getExtension(attr).equals("pdf")
                        && !FilenameUtils.getExtension(attr).equals("png")&& !FilenameUtils.getExtension(attr).equals("txt")) {
                    String regexHTML="[^0-9<>()_=]+";
                    if (el.html().matches(regexHTML)) {
                        outHTML.add(el.outerHtml());
                        System.out.println("Сканируем HTML..." + el.html());
                        System.out.println("Сканируем outHTML..." + el.outerHtml()+"\nРазмер outHTML..."+outHTML.size());
                        count++;//********
                        LinkExecutor linkExecutor = new LinkExecutor(attr);
                        linkExecutor.fork();
                        subTask.add(linkExecutor);
                        allLinks.add(attr);
                        System.out.println("allLinks- "+allLinks);

                        if (count < 10) {//********
                            break;//********
                        }//********

                    }
                }
            }
        } catch (InterruptedException | IOException ignored) {
        }
    }
}

