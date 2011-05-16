package edu.stanford.mercury;

import java.util.Vector;

/** Static class to list all the message types used in Mercury.
 *
 * HolyC will use message type from 18,384 to 32,767 (INT_MAX for 16 bit).
 * 
 * Rant: This is nasty but necessary.
 *
 * @author ykk
 * @author huangty
 * @author gac1
 * @date Apr 2011
 */
public class MercuryMessage {
    /** Maximum message type number an application is allowed to use.
     * HolyC will use a number greater than this for its message.
     */
    public static final int MAX_MSG_TYPE = 18383;

    /**
     * @see RuleService
     */
    public class REGISTER_CLIENT {
    	public static final int type = MAX_MSG_TYPE+1;
    }

    /**
     * @see RuleService
     */
    public class UNREGISTER_CLIENT {
    	public static final int type = MAX_MSG_TYPE+2;
    }
    
    /**
     * @see RuleService
     */
    public class CONNECTION_NOTICE {
    	public static final int type = MAX_MSG_TYPE+3;
    	public static final String str_key = "MSG_CONNECTION_NOTICE";
    }
    
    /** Intent to send query to RuleService
    *
    * @see RuleQuery
    */
    public class Query {
    	public static final String action = "ruleService.intent.query";
    	public static final String str_key = "RULE_QUERY";
    }
   
	/** Intent to send result of query from RuleService
	*
	* @see RuleResult
	*/
	public class Result {
		public static final String action = "ruleService.intent.result";
		public static final String str_key = "RULE_RESULT";
    }
	
	/** Intent to Notify user of Network Action from RuleService
	*
	* @see RuleResult
	*/
	public class NetworkActionQuery {
		public static final String action = "ruleService.intent.network_action_query";
		public static final String str_key = "NETWORK_ACTION_QUERY";
    }
	
	/** Intent to Notify user of Network Action from RuleService
	*
	* @see RuleResult
	*/
	public class NetworkActionResult {
		public static final String action = "ruleService.intent.network_action_result";
		public static final String str_key = "NETWORK_ACTION_RESULT";
    }

    public class RuleResult {
	   public String[] columns = null;

	   public Vector<Vector<String>> results = new Vector<Vector<String>>();
    }   

    public class RuleQuery {		
		/** Columns to return
		 */
		public String[] columns = null;
		/** The SQL WHERE clause
		 */
		public String selection = null;
		/** May include ?s in selection, 
		 * which will be replaced by the values from selectionArgs
		 */
		public String[] selectionArgs = null;
		/** Group results by
		 */
		public String groupBy = null;
		/** A filter declare which row groups to include in the cursor
		 */
		public String having = null;
		/** Order result with
		 */
		public String orderBy = null;
    }
}