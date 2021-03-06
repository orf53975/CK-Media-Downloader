/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package downloader.Extractors;

import downloader.CommonUtils;
import downloader.DataStructures.GenericQuery;
import downloader.DataStructures.MediaDefinition;
import downloader.DataStructures.video;
import downloader.Exceptions.GenericDownloaderException;
import downloader.Exceptions.PageNotFoundException;
import downloader.Exceptions.PrivateVideoException;
import downloaderProject.MainApp;
import java.io.File;
import java.io.IOException;
import org.jsoup.UncheckedIOException;
import java.net.SocketTimeoutException;
import java.util.HashMap;
import java.util.Map;
import java.util.Random;
import java.util.Vector;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.select.Elements;

/**
 *
 * @author christopher
 */
public class Shesfreaky extends GenericQueryExtractor{
	private static final int skip = 1;
    
    public Shesfreaky() { //this contructor is used for when you jus want to query
        
    }
    
    public Shesfreaky(String url) throws IOException, SocketTimeoutException, UncheckedIOException, Exception{
        this(url,downloadThumb(configureUrl(url)),downloadVideoName(configureUrl(url)));
    }
    
    public Shesfreaky(String url, File thumb) throws IOException, SocketTimeoutException, UncheckedIOException, Exception{
        this(url,thumb,downloadVideoName(configureUrl(url)));
    }
    
    public Shesfreaky(String url, File thumb, String videoName){
        super(url,thumb,videoName);
    }

    @Override public MediaDefinition getVideo() throws IOException, SocketTimeoutException, UncheckedIOException, GenericDownloaderException{
        
        Document page = Jsoup.parse(Jsoup.connect(url).userAgent(CommonUtils.PCCLIENT).get().html());
        verify(page);
	String video = page.select("video").select("source").attr("src");
        
        Map<String,String> qualities = new HashMap<>();
        qualities.put("single",video); MediaDefinition media = new MediaDefinition();
        media.addThread(qualities,videoName);
        
        return media;
    }

    private static void verify(Document page) throws GenericDownloaderException {
       if (!page.select("p.private-video").isEmpty())
            throw new PrivateVideoException();
       String title = Jsoup.parse(page.select("title").toString()).text();
       if (title.equals("404 Not Found") || title.equals("404: File Not Found - ShesFreaky"))
           throw new PageNotFoundException();
    }
    
    @Override public GenericQuery query(String search) throws IOException, SocketTimeoutException, UncheckedIOException, Exception{
        search = search.trim(); 
        search = search.replaceAll(" ", "-");
        String searchUrl = "https://www.shesfreaky.com/search/videos/"+search+"/page1.html";
        GenericQuery thequery = new GenericQuery();
        
        Document page = getPage(searchUrl,false);
         
         Elements searchResults = page.select("div.blockItem.blockItemBox");
         for(int i = 0; i < searchResults.size(); i++) {
            String link = searchResults.get(i).select("a").attr("href");
            if (!CommonUtils.testPage(link)) continue; //test to avoid error 404
            try {verify(getPage(link,false));} catch (GenericDownloaderException e) {continue;}
            String title = searchResults.get(i).select("span.details").select("em").attr("title");
            String thumb = searchResults.get(i).select("span.thumb").select("img").attr("data-src");
            if (!thumb.startsWith("https:"))
                thumb = "https:" + thumb;
            thequery.addLink(link);
            if (!CommonUtils.checkImageCache(CommonUtils.getThumbName(thumb,skip))) //if file not already in cache download it
                if (CommonUtils.saveFile(thumb, CommonUtils.getThumbName(thumb,skip),MainApp.imageCache) != -2)
                    throw new IOException("Failed to completely download page");
            Document linkPage = Jsoup.parse(Jsoup.connect(link).userAgent(CommonUtils.PCCLIENT).get().html());
            String video = linkPage.select("video").select("source").attr("src");
            thequery.addThumbnail(new File(MainApp.imageCache.getAbsolutePath()+File.separator+CommonUtils.getThumbName(thumb,skip)));
            thequery.addPreview(parse(link));
            thequery.addName(title);
            thequery.addSize(CommonUtils.getContentSize(video));
         }
         return thequery;
    }

    @Override protected Vector<File> parse(String url) throws IOException, SocketTimeoutException, UncheckedIOException {
        Document page; 
        Vector<File> thumbs = new Vector<>();
         page = getPage(url,false);
         
         Elements thumbLinks = null;
         try {
            thumbLinks = page.getElementById("vidSeek").select("div.vidSeekThumb");
         } catch (NullPointerException e) { //different config
             thumbLinks = page.select("div.row.vtt-thumbs").select("a");
         }
         
         if (thumbLinks != null) {
            for(int i = 0; i < thumbLinks.size(); i++) {
                String link = thumbLinks.get(i).select("a").select("img").attr("src");
                if (!link.startsWith("https:"))
                    link = "https:" + link;
                 if(!CommonUtils.checkImageCache(CommonUtils.getThumbName(link,skip))) //if file not already in cache download it
                   CommonUtils.saveFile(link,CommonUtils.getThumbName(link,skip),MainApp.imageCache);
               thumbs.add(new File(MainApp.imageCache.getAbsolutePath()+File.separator+CommonUtils.getThumbName(link,skip)));
            }
         }
         return thumbs;
    }
    
    private static String downloadVideoName(String url) throws IOException , SocketTimeoutException, UncheckedIOException, Exception{
         Document page = getPage(url,false);
         verify(page);
	return Jsoup.parse(page.getElementById("n-vid-details").select("h2").toString()).body().text();
    } 
	
    //getVideo thumbnail
    private static File downloadThumb(String url) throws IOException, SocketTimeoutException, UncheckedIOException, Exception {
        Document page = getPage(url,false);
        verify(page);
        String thumbLink = "https:"+page.select("video").attr("poster");
        if (thumbLink.equals("https:")) {
            Elements thumbLinks = null;
            try {
               thumbLinks = page.getElementById("vidSeek").select("div.vidSeekThumb");
            } catch (NullPointerException e) { //different config
                thumbLinks = page.select("div.row.vtt-thumbs").select("a");
            }

            String link = thumbLinks.get(0).select("a").select("img").attr("src");
            if (!link.startsWith("https:"))
                link = "https:" + link;
            thumbLink = link;
        }
        if(!CommonUtils.checkImageCache(CommonUtils.getThumbName(thumbLink,skip))) //if file not already in cache download it
            CommonUtils.saveFile(thumbLink,CommonUtils.getThumbName(thumbLink,skip),MainApp.imageCache);
        return new File(MainApp.imageCache.getAbsolutePath()+File.separator+CommonUtils.getThumbName(thumbLink,skip));
    }
    
    @Override protected void setExtractorName() {
        extractorName = "Shesfreaky";
    }

    @Override public video similar() throws IOException {
    	if (url == null) return null;
        
        video v = null;
        Document page = getPage(url,false);
        Elements li = page.select("div.relatedSection").get(0).select("div.blockItem.blockItemBox");
        Random randomNum = new Random(); int count = 0; boolean got = false; if (li.size() == 0) got = true;
        while(!got) {
        	if (count > li.size()) break;
        	int i = randomNum.nextInt(li.size()); count++;
        	String link = li.get(i).select("a").attr("href");
        	try {verify(getPage(url,false)); } catch(GenericDownloaderException e) {continue;}
            String title = li.get(i).select("em").attr("title");
            Document linkPage = Jsoup.parse(Jsoup.connect(link).userAgent(CommonUtils.PCCLIENT).get().html());
            String video = linkPage.select("video").select("source").attr("src");
                try {v = new video(link,title,downloadThumb(link),CommonUtils.getContentSize(video));} catch(Exception e) {continue;}
                break;
            }
        return v;
    }

    @Override public video search(String str) throws IOException {
        str = str.trim(); 
        str = str.replaceAll(" ", "-");
        String searchUrl = "https://www.shesfreaky.com/search/videos/"+str+"/page1.html";
        
        Document page = getPage(searchUrl,false); video v = null;
         
         Elements searchResults = page.select("div.blockItem.blockItemBox");
         for(int i = 0; i < searchResults.size(); i++) {
            String link = searchResults.get(i).select("a").attr("href");
            if (!CommonUtils.testPage(link)) continue; //test to avoid error 404
            try {verify(getPage(link,false));} catch (GenericDownloaderException e) {continue;}
            String title = searchResults.get(i).select("span.details").select("em").attr("title");
            String thumb = searchResults.get(i).select("span.thumb").select("img").attr("data-src");
            if (!thumb.startsWith("https:"))
                thumb = "https:" + thumb;
            if (!CommonUtils.checkImageCache(CommonUtils.getThumbName(thumb,skip))) //if file not already in cache download it
                if (CommonUtils.saveFile(thumb, CommonUtils.getThumbName(thumb,skip),MainApp.imageCache) != -2)
                    throw new IOException("Failed to completely download page");
            Document linkPage = Jsoup.parse(Jsoup.connect(link).userAgent(CommonUtils.PCCLIENT).get().html());
            String video = linkPage.select("video").select("source").attr("src");
            v = new video(link,title,new File(MainApp.imageCache.getAbsolutePath()+File.separator+CommonUtils.getThumbName(thumb,skip)),CommonUtils.getContentSize(video));
            break; //if u made it this far u already have a vaild video
         }
         return v;
    }

    @Override  public long getSize() throws IOException, GenericDownloaderException {
        return CommonUtils.getContentSize(getVideo().iterator().next().get("single"));
    }
}
