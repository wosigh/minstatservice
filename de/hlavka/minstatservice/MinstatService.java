/**
 */
package de.hlavka.minstatservice;

import com.palm.luna.LSException; 
import com.palm.luna.service.LunaServiceThread;
import com.palm.luna.service.ServiceMessage;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Calendar;
import java.util.Date;

import org.json.JSONException;
import org.json.JSONObject;

/**
 * @author Tobias Hlavka
 */

public class MinstatService extends LunaServiceThread {
	
	@LunaServiceThread.PublicMethod
	public void getVersion(ServiceMessage msg) throws LSException, JSONException  {
		try
		{
				JSONObject reply = new JSONObject();
				reply.put("version","1.6");
				msg.respond(reply.toString());
		}
		catch (LSException e) 
		{
					this.logger.severe("", e);
		}
				
	}
	
	@LunaServiceThread.PublicMethod
	public void status(ServiceMessage msg) throws JSONException, LSException
	{
		try
		{
			JSONObject reply = new JSONObject();
			reply.put("returnValue",true);
			msg.respond(reply.toString());
		}
		catch (LSException e) 
		{
			this.logger.severe("", e);
		}
		
		
	}
	
	@LunaServiceThread.PublicMethod
	public void getSQLData(ServiceMessage msg) throws JSONException, LSException
	{
		if(msg.getJSONPayload().has("cashday") && msg.getJSONPayload().has("clockrateFirst") && msg.getJSONPayload().has("clockrateFurther")) {
			double cashday = msg.getJSONPayload().getDouble("cashday");
			double clockrateFirst = msg.getJSONPayload().getDouble("clockrateFirst");
			double clockrateFurther = msg.getJSONPayload().getDouble("clockrateFurther");
			
			Date now = new Date(); // today
			Calendar cal = Calendar.getInstance();
			cal.setTime(now);
			
			int month = cal.get(Calendar.MONTH);
			int year = cal.get(Calendar.YEAR);

			if (cal.get(Calendar.DAY_OF_MONTH) < cashday) {
				month = month-1;
				
				if (month == 0) {
					month = 12;
					year = year-1;
				}
			}
			
			Calendar lastPayroll = Calendar.getInstance();
			lastPayroll.set(year, month, (int) cashday);
			long sinceTimestamp = lastPayroll.getTimeInMillis();
					
		try {
			Class.forName("org.sqlite.JDBC");
			try {
				Connection conn = DriverManager.getConnection("jdbc:sqlite:/var/luna/data/dbdata/PalmDatabase.db3");
				Statement stat = conn.createStatement();
			 
			    ResultSet rs = stat.executeQuery("select messageText from com_palm_pim_FolderEntry where timeStamp>'"+sinceTimestamp+"' and smsClass='0' and messageType='SMS';");
			    JSONObject reply = new JSONObject();
			    
			    int cntSMS = 0;
			    
			    while (rs.next()) {
			    	cntSMS = cntSMS + (int)Math.ceil(rs.getString("messageText").length()/160d);
			    }
			    reply.put("SMS",cntSMS);
			    
			    
			   rs = stat.executeQuery("select messageText from com_palm_pim_FolderEntry where timeStamp>'"+sinceTimestamp+"' and smsClass='1' and messageType='SMS';");
			    
			   int incomingSMS = 0;
			    
			   while (rs.next()) {
			    	incomingSMS = incomingSMS + (int)Math.ceil(rs.getString("messageText").length()/160d);
			   }
			    reply.put("incomingSMS",cntSMS);
			    
			    
			    
			    rs = stat.executeQuery("select duration from com_palm_superlog_Superlog where startTime>'"+sinceTimestamp+"' and duration>'0' and type='outgoing';");
			    
			    double cntMin = 0;
			    double tmpSec = 0;
			    
			    while (rs.next()) {
			    	tmpSec = Math.ceil(Integer.parseInt(rs.getString("duration"))/1000d);
			    	
			    	tmpSec = tmpSec-clockrateFirst;
					cntMin = cntMin + (clockrateFirst/60d);

					while(tmpSec >= 0d) {
						tmpSec = tmpSec - clockrateFurther;
						cntMin = cntMin + (clockrateFurther/60d);
					}
					
			    }
			    reply.put("minutes", cntMin);
			    
			    
			    rs.close();
			    conn.close();
				msg.respond(reply.toString());

			} catch (SQLException e) {
				JSONObject reply = new JSONObject();
				reply.put("SQL error",e.getMessage());
				msg.respond(reply.toString());

				
			}
			
			
		} catch (ClassNotFoundException e) {
			JSONObject reply = new JSONObject();
			reply.put("Class not found",e.getMessage());
			msg.respond(reply.toString());

		}
		
		} else {
			// missing arguments
			JSONObject reply = new JSONObject();
			reply.put("SMS","-1");
			reply.put("minutes","-1");
			msg.respond(reply.toString());
		}
	}

	
	
}