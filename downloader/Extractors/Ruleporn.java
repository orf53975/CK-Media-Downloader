/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package downloader.Extractors;

import downloader.CommonUtils;
import downloader.DataStructures.GenericQuery;
import downloader.Exceptions.GenericDownloaderException;
import downloader.Exceptions.VideoDeletedException;
import downloaderProject.MainApp;
import downloaderProject.OperationStream;
import java.io.File;
import java.io.IOException;
import org.jsoup.UncheckedIOException;
import java.net.SocketTimeoutException;
import java.util.Vector;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.select.Elements;

/**
 *
 * @author christopher
 */
public class Ruleporn extends GenericQueryExtractor{
    
    public Ruleporn() { //this contructor is used for when you jus want to query
        
    }
    
    public Ruleporn(String url)throws IOException, SocketTimeoutException, UncheckedIOException, Exception{
        this(url,downloadThumb(configureUrl(url)),downloadVideoName(configureUrl(url)));
    }
    
    public Ruleporn(String url, File thumb) throws IOException, SocketTimeoutException, UncheckedIOException, Exception{
        this(url,thumb,downloadVideoName(configureUrl(url)));
    }
    
    public Ruleporn(String url, File thumb, String videoName) {
        super(url,thumb,videoName);
    }

    @Override
    public void getVideo(OperationStream s) throws IOException, SocketTimeoutException, UncheckedIOException, Exception{
        if (s != null) s.startTiming();
        
        Document page = Jsoup.parse(Jsoup.connect(url).userAgent(CommonUtils.pcClient).get().html());
	
        String video = page.select("video").select("source").attr("src");
        
        super.downloadVideo(video,this.videoName,s);
    }
    
    private static void verify(Document page) throws GenericDownloaderException {
        if (page.select("video") == null)
            throw new VideoDeletedException("No video found");
        else if (page.select("video").attr("poster").length() < 1)
            throw new VideoDeletedException("No video found");
    }
    
    @Override
    public GenericQuery query(String search) throws IOException, SocketTimeoutException, UncheckedIOException, Exception{
        search = search.trim(); 
        search = search.replaceAll(" ", "-");
        String searchUrl = "https://ruleporn.com/search/"+search+"/";
        
        GenericQuery thequery = new GenericQuery();
        Document page;
                
        if (CommonUtils.checkPageCache(CommonUtils.getCacheName(searchUrl,false))) //check to see if page was downloaded previous
            page = Jsoup.parse(CommonUtils.loadPage(MainApp.pageCache.getAbsolutePath()+File.separator+CommonUtils.getCacheName(searchUrl,false)));
        else {
            String html = Jsoup.connect(searchUrl).userAgent(CommonUtils.pcClient).get().html();
            page = Jsoup.parse(html);
            CommonUtils.savePage(html, searchUrl, false);
        } //if not found in cache download it
        
	Elements searchResults = page.select("div.row").select("div.item-inner-col.inner-col");
	for(int i = 0; i < searchResults.size(); i++)  {
            if (!CommonUtils.testPage(searchResults.get(i).select("a").get(0).attr("href"))) continue; //test to avoid error 404
            try {verify(getPage(searchResults.get(i).select("a").get(0).attr("href"),false));} catch (GenericDownloaderException e) {continue;}
            thequery.addLink(searchResults.get(i).select("a").get(0).attr("href"));
            String thumbLink = searchResults.get(i).select("img").attr("src");
            if (!CommonUtils.checkImageCache(CommonUtils.getThumbName(thumbLink))) //if file not already in cache download it
                if (CommonUtils.saveFile(thumbLink, CommonUtils.getThumbName(thumbLink),MainApp.imageCache) != -2)
                    throw new IOException("Failed to completely download page");
            thequery.addThumbnail(new File(MainApp.imageCache+File.separator+CommonUtils.getThumbName(thumbLink)));
            thequery.addPreview(parse(thequery.getLink(i)));
            thequery.addName(searchResults.get(i).select("span.title").text());
	}
        return thequery;
    }
    
    //get preview thumbnails
    @Override
    protected Vector<File> parse(String url) throws IOException, SocketTimeoutException, UncheckedIOException{ 
        Vector<File> thumbs = new Vector<>();
        
        try {
            String html;
            if (CommonUtils.checkPageCache(CommonUtils.getCacheName(url,true))) //check to see if page was downloaded previous 
                html = CommonUtils.loadPage(MainApp.pageCache.getAbsolutePath()+File.separator+CommonUtils.getCacheName(url,true));
            else {
                html = Jsoup.connect(url).followRedirects(true).userAgent(CommonUtils.mobileClient).get().html(); //not found so download it
                CommonUtils.savePage(html, url, true);
            }
            Document page = Jsoup.parse(html);
           String base = page.select("video").attr("poster");
            for(int i = 0; i < 11; i++) {
                String thumb = base.substring(0,base.indexOf("-")+1) + String.valueOf(i) + base.substring(base.indexOf("-")+2);
                if(!CommonUtils.checkImageCache(CommonUtils.getThumbName(thumb))) //if file not already in cache download it
                    CommonUtils.saveFile(thumb, CommonUtils.getThumbName(thumb),MainApp.imageCache);
                thumbs.add(new File(MainApp.imageCache.getAbsolutePath()+File.separator+CommonUtils.getThumbName(thumb)));		
            }
        } catch (NullPointerException e) {
            return thumbs; //return parse(url); //possible it went to interstitial? instead
        }
        return thumbs;
    }
    
    private static String downloadVideoName(String url) throws IOException, SocketTimeoutException, UncheckedIOException, Exception{
        Document page;
        if (CommonUtils.checkPageCache(CommonUtils.getCacheName(url,false))) //check to see if page was downloaded previous
            page = Jsoup.parse(CommonUtils.loadPage(MainApp.pageCache.getAbsolutePath()+File.separator+CommonUtils.getCacheName(url,false)));
        else {
            String html = Jsoup.connect(url).get().html();
            page = Jsoup.parse(html);
            CommonUtils.savePage(html, url, false);
        } //if not found in cache download it
        
        return page.select("div.item-tr-inner-col.inner-col").get(0).select("h1").text();
    } 
	
    //getVideo thumbnail
    private static File downloadThumb(String url) throws IOException, SocketTimeoutException, UncheckedIOException, Exception{
       Document page = getPage(url,false);
        
        String thumb = page.select("video").attr("poster");
        
        if(!CommonUtils.checkImageCache(CommonUtils.getThumbName(thumb))) //if file not already in cache download it
            CommonUtils.saveFile(thumb,CommonUtils.getThumbName(thumb),MainApp.imageCache);
        return new File(MainApp.imageCache.getAbsolutePath()+File.separator+CommonUtils.getThumbName(thumb));
    }
    
    @Override
    protected void setExtractorName() {
        extractorName = "Ruleporn";
    }
}
