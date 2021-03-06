package de.skubware.opentraining.activity.acra;

import android.util.Log;

import org.acra.collector.CrashReportData;
import org.acra.ACRA;
import org.acra.ReportField;
import org.acra.sender.ReportSender;
import org.acra.sender.ReportSenderException;
import org.apache.http.NameValuePair;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.message.BasicNameValuePair;
import org.apache.http.protocol.HTTP;
import java.math.BigInteger;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.security.SecureRandom;
import java.util.Random;

/** 
 * Based on acra-mailer(https://github.com/d-a-n/acra-mailer) of d-a-n. 
 */
public class ACRAFeedbackMailer implements ReportSender {
	private final static String TAG = "ACRAFeedbackMailer";
	private final static SecureRandom random = new SecureRandom();
	private final static String BASE_URL = "http://skubware.de/opentraining/acra_feedback.php";
	private final static String SHARED_SECRET = "my_on_github_with_everyone_shared_secret";



	@Override
	public void send(CrashReportData report) throws ReportSenderException {

		String url = getUrl();
		Log.e("xenim", url);

		try {
			DefaultHttpClient httpClient = new DefaultHttpClient();
			HttpPost httpPost = new HttpPost(url);

			List<NameValuePair> parameters = new ArrayList<NameValuePair>();

			parameters.add(new BasicNameValuePair("DATE", new Date().toString()));
			parameters.add(new BasicNameValuePair("APP_VERSION_NAME", report.get(ReportField.APP_VERSION_NAME)));
			
			
			String custom_data = report.get(ReportField.CUSTOM_DATA); // custom data has to be split
			for(String keyValue:custom_data.split("\n")){
				String[] splittedString =  keyValue.split("=");

				if(splittedString.length >1){
					parameters.add(new BasicNameValuePair(splittedString[0].trim(), splittedString[1].trim()));
				}else{
					Log.w(TAG, "No valid split for: " + splittedString);
				}
			}
			
			
			httpPost.setEntity(new UrlEncodedFormEntity(parameters, HTTP.UTF_8));
			httpClient.execute(httpPost);

			// set the crash report sender again after sending the feedback
			ACRA.getErrorReporter().removeAllReportSenders();
	        ACRA.getErrorReporter().setReportSender(new ACRACrashReportMailer());
	        
	        
		} catch (Exception e) {
			Log.v("ACRAFeedbackMailer", e.getMessage().toString());
		}
	}

	private String getUrl() {
		String token = getToken();
		String key = getKey(token);
		return String.format("%s?token=%s&key=%s&", BASE_URL, token, key);
	}

	private String getKey(String token) {
		Random randgen = new Random();
		return makeSha(String.format("%s+%s", new BigInteger(randgen.nextInt(), random).toString(), token));
	}

	private String getToken() {
		Random randgen = new Random();
		return makeSha(new BigInteger(randgen.nextInt(), random).toString());
	}

	public static String makeSha(String s) {
		MessageDigest m = null;
		try {
			m = MessageDigest.getInstance("SHA-256");
		} catch (NoSuchAlgorithmException e) {
			Log.v("ACRAFeedbackMailer", e.getMessage().toString());
		}
		m.update(s.getBytes(), 0, s.length());
		String hash = new BigInteger(1, m.digest()).toString(16);
		return hash;
	}
}
