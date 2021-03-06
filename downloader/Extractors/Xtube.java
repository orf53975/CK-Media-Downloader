/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package downloader.Extractors;

import downloader.CommonUtils;
import downloader.DataStructures.MediaDefinition;
import downloader.DataStructures.video;
import downloader.Exceptions.GenericDownloaderException;
import downloader.Exceptions.PrivateVideoException;
import downloaderProject.MainApp;
import java.io.File;
import java.io.IOException;
import org.jsoup.UncheckedIOException;
import java.net.SocketTimeoutException;
import java.util.HashMap;
import java.util.Map;
import java.util.Random;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.select.Elements;

/**
 *
 * @author christopher
 */
public class Xtube extends GenericExtractor{
	private static final int skip = 6;
    
    public Xtube() { //this contructor is used for when you jus want to search
        
    }
    
    public Xtube(String url)throws IOException, SocketTimeoutException, UncheckedIOException, Exception{
        this(url,downloadThumb(configureUrl(url)),downloadVideoName(configureUrl(url)));
    }
    
    public Xtube(String url, File thumb) throws IOException, SocketTimeoutException, UncheckedIOException, Exception{
        this(url,thumb,downloadVideoName(configureUrl(url)));
    }
    
    public Xtube(String url, File thumb, String videoName) {
        super(url,thumb,videoName);
    }

    private static Map<String,String> getQualities(String src) {
        String from = !src.contains("\"sources\":") ? "sources:" : "\"sources\":";
        String[] pair = CommonUtils.getBracket(src,src.indexOf(from)).split(",");
        
        Map<String, String> qualities = new HashMap<>();
        for(int i = 0; i < pair.length; i++) {
            String[] temp = pair[i].split(":\"");
            qualities.put(CommonUtils.getPureDigit(temp[0]), CommonUtils.eraseChar(temp[1], '\\').replace("\"","").replace("}",""));
        }
        return qualities;
    }
    
    public MediaDefinition getVideo() throws IOException, SocketTimeoutException, UncheckedIOException{        
        Document page = Jsoup.parse(Jsoup.connect(url).userAgent(CommonUtils.PCCLIENT).get().html());
	Map<String,String> quality = getQualities(page.toString());
        
        MediaDefinition media = new MediaDefinition();
        media.addThread(quality,videoName);
        
        return media;
    }
    
    private static void verify(Document page) throws GenericDownloaderException {
        if(!page.select("div.rootFakePlayer").isEmpty())
            throw new PrivateVideoException(page.select("div.rootFakePlayer").select("p.msg").select("strong").text());
    }
    
    private static String downloadVideoName(String url) throws IOException, SocketTimeoutException, UncheckedIOException, Exception{
        Document page = getPage(url,false);
        verify(page);
	return Jsoup.parse(page.select("h1").get(0).toString()).body().text();
    } 
	
    //getVideo thumbnail
    private static File downloadThumb(String url) throws IOException, SocketTimeoutException, UncheckedIOException, Exception{
        Document page = getPage(url,false);
        
        verify(page);
        String from = !page.toString().contains("\"poster\":") ? "poster:" : "\"poster\":";
        int offset = !page.toString().contains("\"poster\":") ? 8 : 10;
        
        String thumb = CommonUtils.eraseChar(CommonUtils.getLink(page.toString(),page.toString().indexOf(from) + offset,'\"'), '\\');
        
        if(!CommonUtils.checkImageCache(CommonUtils.getThumbName(thumb,skip))) //if file not already in cache download it
            CommonUtils.saveFile(thumb,CommonUtils.getThumbName(thumb,skip),MainApp.imageCache);
        return new File(MainApp.imageCache.getAbsolutePath()+File.separator+CommonUtils.getThumbName(thumb,skip));
    }
    
    @Override protected void setExtractorName() {
        extractorName = "Xtube";
    }

    @Override public video similar() throws IOException {
    	if (url == null) return null;
        
        video v = null;
        Document page = getPage(url,false);
        Elements li = page.select("div.cntPanel.relatedVideosPanel").select("ul.row.smallSpace.rowSpace").select("li");
        Random randomNum = new Random(); int count = 0; boolean got = false; if (li.isEmpty()) got = true;
        while(!got) {
        	if (count > li.size()) break;
        	int i = randomNum.nextInt(li.size()); count++;
        	String link = "https://www.xtube.com" + li.get(i).select("a").get(0).attr("href");
                try {verify(getPage(link,false));} catch (GenericDownloaderException ex) {continue;}
            String title = li.get(i).select("h3").text();
                
                try {v = new video(link,title,downloadThumb(link),getSize(link));}catch(Exception e) {continue;}
                break;
            }
        return v;
    }

    @Override public video search(String str) throws IOException{
    	str = str.trim(); str = str.replaceAll(" ", "-");
    	String searchUrl = "https://www.xtube.com/search/video/"+str;
    	
    	Document page = getPage(searchUrl,false);
        video v = null;
        
        Elements li = page.select("li.deleteListElement.col-xs-24.col-s-12.col-xl-6.col10-xxl-2");
        
        for(int i = 0; i < li.size(); i++) {
            String link = "http://www.xtube.com" + li.get(i).select("a").get(0).attr("href");
                try {verify(getPage(link,false));} catch (GenericDownloaderException ex) {continue;}
        	String thumbLink = li.get(i).select("img").get(0).attr("src"); 
        	if (!CommonUtils.checkImageCache(CommonUtils.getThumbName(thumbLink,skip))) //if file not already in cache download it
                    if (CommonUtils.saveFile(thumbLink, CommonUtils.getThumbName(thumbLink,skip),MainApp.imageCache) != -2)
                        throw new IOException("Failed to completely download page");
        	String name = li.get(i).select("h3").text();
        	if (link.isEmpty() || name.isEmpty()) continue;
        	try { v = new video(link,name,new File(MainApp.imageCache+File.separator+CommonUtils.getThumbName(thumbLink,skip)),getSize(link)); } catch(GenericDownloaderException | IOException e)  {}
        	break;
        }
        
        return v;
    }
    
    private static long getSize(String link) throws IOException, GenericDownloaderException {
        Document page = Jsoup.parse(Jsoup.connect(link).userAgent(CommonUtils.PCCLIENT).get().html());
        
	Map<String,String> quality = getQualities(page.toString());
        
        String video = null;
        if (quality.containsKey("720"))
            video = quality.get("720");
        else if(quality.containsKey("480"))
            video = quality.get("480");
        else if (quality.containsKey("360"))
            video = quality.get("360");
        else if (quality.containsKey("240"))
            video = quality.get("240");
        else video = quality.get((String)quality.values().toArray()[0]);
        return CommonUtils.getContentSize(video);
    }

    @Override public long getSize() throws IOException, GenericDownloaderException {
        return getSize(url);
    }
}
