package com.serverless;

import java.util.Map;
import java.io.IOException;
import java.net.URI;
import org.apache.log4j.Logger;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Stack;
import java.util.TimeZone;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import twitter4j.Twitter;
import twitter4j.TwitterFactory;
import twitter4j.auth.AccessToken;
import twitter4j.TwitterException;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.RequestHandler;

public class Handler implements RequestHandler<Map<String, Object>, String> {

	private static final Logger LOG = Logger.getLogger(Handler.class);
	private static final String BASE_URL = "https://ja.wikipedia.org/wiki/";

	@Override
	public String handleRequest(Map<String, Object> input, Context context) {
		System.out.println(System.getenv("KEYWORD"));
		System.out.println(System.getenv("HEADER"));
		try {
			String keyword = System.getenv("KEYWORD");
			String header = System.getenv("HEADER");
			whatDay(keyword, header);
			return "finish";
		} catch (Exception e) {
			e.printStackTrace();
			return "error";
		}
	}

	public String getContent(String keyword) throws Exception {
		String xml = "";
		String url = getUrl(keyword);

		Document document = getDocumentFromFrom(url);
		Elements elements = document.select("#mw-content-text ul");

		for (Element element : elements) {
			Elements lis = element.select("li");
			for (Element li : lis) {
				xml += elementToWord(li);
			}
		}

		return xml;
	}

	private String elementToWord(Element element) {
		String str = "";

		str += "◆";
		str += element.text();
		str += "\n";

		return str;
	}

	private static Document getDocumentFromFrom(String requestUrl)
			throws IOException {
		Document document = Jsoup.connect(requestUrl).get();
		return document;
	}

	private static String getUrl(String keyword) throws Exception {
		return new URI(BASE_URL + keyword).toASCIIString();
	}

	public void whatDay(String keyword, String header) throws Exception {
		String content = getContent(keyword);
		System.out.println("wikipedia content = " + content);
		String addDayHeader = getToday(header);
		System.out.println("addDayHeader content = " + addDayHeader);

		tweetWhatDay(content, addDayHeader);
	}

	private void tweetWhatDay(String content, String header)
			throws TwitterException {
		Stack<String> stack = createTweetContents(content, header);

		System.out.println("stack content = " + stack);

		while (!stack.empty()) {
			String tweetContent = stack.pop();
			AccessToken accessToken = loadAccessToken();
			Twitter twitter = new TwitterFactory().getInstance();
			twitter.setOAuthConsumer(System.getenv("TWITTER4J_OAUTH_CONSUMERKEY"), System.getenv("TWITTER4J_OAUTH_CONSUMERSECRET"));
			twitter.setOAuthAccessToken(accessToken);
			twitter.updateStatus(tweetContent);
		}
	}

	private static AccessToken loadAccessToken(){
		String token = System.getenv("TWITTER4J_OAUTH_ACCESSTOKEN");
		String tokenSecret = System.getenv("TWITTER4J_OAUTH_ACCESSTOKENSECRET");
		return new AccessToken(token, tokenSecret);
	}

	private Stack<String> createTweetContents(String content, String header) {
		Stack<String> stack = new Stack<>();
		Integer tweCnt = 1;
		String oneTweetContent = "";

		String[] contents = content.split("\n");
		for (String row : contents) {
			String addCntheader = convertHeader(tweCnt, header);
			String tmpStr = addCntheader + oneTweetContent + "\n" + row;
			if (isOverTwitterLimit(tmpStr)) {
				stack.push(addCntheader + oneTweetContent);
				oneTweetContent = row;
				tweCnt++;
			} else {
				oneTweetContent = addRowToContent(oneTweetContent, row);
			}
		}

		String addCntheader = convertHeader(tweCnt, header);
		stack.push(addCntheader + oneTweetContent);

		return stack;
	}

	private boolean isOverTwitterLimit(String content) {
		return content.length() > 140;
	}

	private String addRowToContent(String content, String row) {
		if (content.isEmpty()) {
			content = row;
		} else {
			content = content + "\n" + row;
		}
		return content;
	}

	private String getToday(String headerContent) {
		Calendar c = Calendar.getInstance();
		SimpleDateFormat sdf = new SimpleDateFormat("【M月d日】");
		sdf.setTimeZone(TimeZone.getTimeZone("JST"));
		return sdf.format(c.getTime()) + headerContent;
	}

	private String convertHeader(Integer cnt, String header) {
		return header + "(その" + cnt + ")" + "\n\n";
	}
}
