package neurosky;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.lang.reflect.Method;
import java.net.ConnectException;
import java.net.Socket;
import java.net.SocketException;
import java.net.UnknownHostException;
import java.util.Iterator;

import org.json.JSONException;
import org.json.JSONObject;

import processing.core.PApplet;

public class ThinkGearSocket  implements Runnable{
    
	public PApplet parent;
	public Socket neuroSocket;
	public OutputStream outStream;
	public InputStream inStream;
	public BufferedReader stdIn;
  private Method attentionEventMethod = null;
  private Method meditationEventMethod = null;
  private Method blinkEventMethod = null;
  private Method rawEventMethod = null;
  //private Method eegEventMethod = null;
  public String appName="";
  public String appKey="";
  private Thread t;
  
  private int raw[] = new int[512];
  private int index = 0;
  
	public final static String VERSION = "1.0";
  
  
  
  private boolean running = true;
	  public ThinkGearSocket(PApplet _parent, String _appName,String _appKey){
		  this(_parent);
		  appName = _appName;//these were mentioned in the documentation as required, but test prove they are not.
		  appKey = _appKey;  

	  }

	public ThinkGearSocket(PApplet _parent){
		parent = _parent;
	    try {
	        attentionEventMethod =
	          parent.getClass().getMethod("attentionEvent",  new Class[] { 
	          int.class
	        }   
	        );
	        
	      } 
	      catch (Exception e) {
	      	System.err.println("attentionEvent() method not defined. ");
	      }

	      try {
	        meditationEventMethod =
	          parent.getClass().getMethod("meditationEvent",  new Class[] { 
	          int.class
	        }   
	        );
	      } 
	      catch (Exception e) {
	      	System.err.println("meditationEvent() method not defined. ");
	      }

	      try {
	        blinkEventMethod =
	          parent.getClass().getMethod("blinkEvent",  new Class[] { 
	          int.class
	        }   
	        );
	      } 
	      catch (Exception e) {
	      	System.err.println("blinkEvent() method not defined. ");
	      }
              /*
              try {
	        eegEventMethod =
	          parent.getClass().getMethod("eegEvent",  new Class[] { 
	          int.class, int.class, int.class, int.class, int.class, int.class, int.class, int.class
	        }   
	        );
	      } 
	      catch (Exception e) {
	      	System.err.println("eegEvent() method not defined. ");
	      }
              */

	     try {
	        rawEventMethod =
	          parent.getClass().getMethod("rawEvent",  new Class[] { 
	          int[].class
	        }   
	        );
	      } 
	      catch (Exception e) {
	      	System.err.println("rawEvent() method not defined. ");
	      }
	}
	
	
	  public boolean isRunning(){
		  return running;
	  }
	
	/*
	 * return the version of the library.
	 * @return String
	 */
	public static String version() {
		return VERSION;
	}
		
	public void start() throws ConnectException{
		
		try {
			neuroSocket = new Socket("127.0.0.1",13854);	
		} catch (ConnectException e) {
			//e.printStackTrace();
			System.out.println("Is ThinkkGear running?");
			running = false;
			throw e;
		} catch (UnknownHostException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
		
		try {
			inStream  = neuroSocket.getInputStream();
			outStream = neuroSocket.getOutputStream();
			stdIn = new BufferedReader(new InputStreamReader(neuroSocket.getInputStream()));
			running = true;
		} catch (IOException e) {
			e.printStackTrace();
		}	
		
		if(appName !="" && appKey !=""){
			JSONObject appAuth = new JSONObject();
			try {
				appAuth.put("appName", appName);
			} catch (JSONException e1) {
				// TODO Auto-generated catch block
				e1.printStackTrace();
			}
			try {
				appAuth.put("appKey", appKey);
			} catch (JSONException e1) {
				// TODO Auto-generated catch block
				e1.printStackTrace();
			}
			//throws some error
			sendMessage(appAuth.toString());
			System.out.println("appAuth"+appAuth);
		}
		
		JSONObject format = new JSONObject();
		try {
			format.put("enableRawOutput", true);
		} catch (JSONException e) {
			// TODO Auto-generated catch block
			System.out.println("raw error");
			e.printStackTrace();
		}
		try {
			format.put("format", "Json");
		} catch (JSONException e) {
			// TODO Auto-generated catch block
			System.out.println("Json error");
			e.printStackTrace();
		}
		//System.out.println("format "+format);
		sendMessage(format.toString());
		 t = new Thread(this);
	    t.start();
	}
	
	@SuppressWarnings("deprecation")
	public void stop(){
		
		if(running){
			t.interrupt();
			try {
				
				neuroSocket.close();
				
				inStream.close();
				outStream.close();
				stdIn.close();
				stdIn = null;
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
				//System.out.println("Socket close issue");
			}
		}
		running = false;
	}
        
	public void sendMessage(String msg){
		PrintWriter out = new PrintWriter(outStream, true);
		//System.out.println("sendmsg");
		out.println(msg);
	}
        
	@Override
	public void run() {
		if(running && neuroSocket.isConnected()){
			String userInput;
	
			try {
				while ((userInput = stdIn.readLine()) != null) {
	
					String[] packets = userInput.split("/\r/");
					for(int s=0;s<packets.length;s++){
						if(((String) packets[s]).indexOf("{")>-1){
							JSONObject obj = new JSONObject((String) packets[s]);
							parsePacket(obj);
						}
					}
				}
			} 
			catch(SocketException e){
				e.printStackTrace();
			}
			catch (IOException e) {
				e.printStackTrace();
			} catch (JSONException e) {
				e.printStackTrace();
			}
			 parent.delay(50);
		}else{
			running = false;
		}
	}

	  private void triggerAttentionEvent(int attentionLevel) {
	    if (attentionEventMethod != null) {
	      try {
	        attentionEventMethod.invoke(parent, new Object[] {
	          attentionLevel
	        }   
	        );
	      } 
	      catch (Exception e) {
	        System.err.println("Disabling attentionEvent()  because of an error.");
	        e.printStackTrace();
	        attentionEventMethod = null;
	      }
	    }
	  }

	  private void triggerMeditationEvent(int meditationLevel) {
	    if (meditationEventMethod != null) {
	      try {
	        meditationEventMethod.invoke(parent, new Object[] {
	          meditationLevel
	        }   
	        );
	      } 
	      catch (Exception e) {
	        System.err.println("Disabling meditationEvent()  because of an error.");
	        e.printStackTrace();
	        meditationEventMethod = null;
	      }
	    }
	  }


	  private void triggerBlinkEvent(int blinkStrength) {
	    if (blinkEventMethod != null) {
	      try {
	        blinkEventMethod.invoke(parent, new Object[] {
	          blinkStrength
	        }   
	        );
	      } 
	      catch (Exception e) {
	        System.err.println("Disabling blinkEvent()  because of an error.");
	        e.printStackTrace();
	        blinkEventMethod = null;
	      }
	    }
	  }
          /*
          private void triggerEEGEvent(int delta, int theta, int low_alpha, int high_alpha, int low_beta, int high_beta, int low_gamma, int mid_gamma) {
	    if (eegEventMethod != null) {
	      try {
	        eegEventMethod.invoke(parent, new Object[] {
	          delta, theta, low_alpha, high_alpha, low_beta, high_beta, low_gamma, mid_gamma
	        }   
	        );
	      } 
	      catch (Exception e) {
	        System.err.println("Disabling eegEvent()  because of an error.");
	        e.printStackTrace();
	        eegEventMethod = null;
	      }
	    }
	  }
          */

	  private void triggerRawEvent(int []values) {
	    if (rawEventMethod != null) {
	      try {
	        rawEventMethod.invoke(parent, new Object[] {
	          values
	        }   
	        );
	      } 
	      catch (Exception e) {
	        System.err.println("Disabling rawEvent()  because of an error.");
	        e.printStackTrace();
	        rawEventMethod = null;
	      }
	    }
	  }	

	  private void parsePacket(JSONObject data){
			Iterator itr = data.keys(); 
			while(itr.hasNext()) {
			    Object e = itr.next(); 
			    String key = e.toString();
			    try{
				  if(key.matches("rawEeg")){
				    	 int rawValue =  (Integer) data.get("rawEeg");
				          raw[index] = rawValue;
				          index++;
				          if (index == 512) {
				            index = 0;
				            int rawCopy[] = new int[512];
				            parent.arrayCopy(raw, rawCopy);
				            triggerRawEvent(rawCopy);
				          }
				    }
				    if(key.matches("blinkStrength")){
				    	triggerBlinkEvent(data.getInt(e.toString()));
				    	
				    }  	
				    if(key.matches("eSense")){
				    	JSONObject esense = data.getJSONObject("eSense");
				    	triggerAttentionEvent(esense.getInt("attention"));
				    	triggerMeditationEvent(esense.getInt("meditation"));
				    	
				    }
                                    /*
                                    if(key.matches("eegPower")){
				    	JSONObject eegPower = data.getJSONObject("eegPower");
				    	triggerEEGEvent(eegPower.getInt("delta"), eegPower.getInt("theta"), eegPower.getInt("lowAlpha"), eegPower.getInt("highAlpha"),eegPower.getInt("lowBeta"), eegPower.getInt("highBeta"),eegPower.getInt("lowGamma"), eegPower.getInt("highGamma"));
				    }
                                    */
			    }
			    catch(Exception ex){
			    	
			    	ex.printStackTrace();
			    }
			} 
	  }
}